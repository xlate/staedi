package io.xlate.edi.internal.schema;

import static io.xlate.edi.internal.schema.StaEDISchemaFactory.schemaException;
import static io.xlate.edi.internal.schema.StaEDISchemaFactory.unexpectedElement;
import static io.xlate.edi.internal.schema.StaEDISchemaFactory.unexpectedEvent;
import static java.util.stream.Collectors.toList;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.IntStream;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import io.xlate.edi.schema.EDIComplexType;
import io.xlate.edi.schema.EDIElementPosition;
import io.xlate.edi.schema.EDIReference;
import io.xlate.edi.schema.EDISchemaException;
import io.xlate.edi.schema.EDISimpleType;
import io.xlate.edi.schema.EDISimpleType.Base;
import io.xlate.edi.schema.EDISyntaxRule;
import io.xlate.edi.schema.EDIType;
import io.xlate.edi.schema.EDIType.Type;

abstract class SchemaReaderBase implements SchemaReader {

    private static final Logger LOGGER = Logger.getLogger(SchemaReaderBase.class.getName());

    static final String REFERR_UNDECLARED = "Type %s references undeclared %s with ref='%s'";
    static final String REFERR_ILLEGAL = "Type '%s' must not be referenced as '%s' in definition of type '%s'";

    static final String LOCALNAME_ELEMENT = "element";
    static final String LOCALNAME_COMPOSITE = "composite";
    static final String ATTR_MIN_OCCURS = "minOccurs";
    static final String ATTR_MAX_OCCURS = "maxOccurs";
    static final String ATTR_TITLE = "title";
    static final String ATTR_LEVEL_ID_POSITION = "levelIdPosition";
    static final String ATTR_PARENT_ID_POSITION = "parentIdPosition";

    static final EDIReference ANY_ELEMENT_REF_OPT = new Reference(StaEDISchema.ANY_ELEMENT_ID, EDIType.Type.ELEMENT, 0, 1, null, null);
    static final EDIReference ANY_COMPOSITE_REF_OPT = new Reference(StaEDISchema.ANY_COMPOSITE_ID, EDIType.Type.COMPOSITE, 0, 99, null, null);

    static final EDIReference ANY_ELEMENT_REF_REQ = new Reference(StaEDISchema.ANY_ELEMENT_ID, EDIType.Type.ELEMENT, 1, 1, null, null);
    static final EDIReference ANY_COMPOSITE_REF_REQ = new Reference(StaEDISchema.ANY_COMPOSITE_ID, EDIType.Type.COMPOSITE, 1, 99, null, null);

    static final EDISimpleType ANY_ELEMENT = new ElementType(StaEDISchema.ANY_ELEMENT_ID,
                                                             Base.STRING,
                                                             -1,
                                                             "ANY",
                                                             0,
                                                             0,
                                                             99_999,
                                                             Collections.emptySet(),
                                                             Collections.emptyList(),
                                                             null,
                                                             null);

    static final EDIComplexType ANY_COMPOSITE = new StructureType(StaEDISchema.ANY_COMPOSITE_ID,
                                                                  Type.COMPOSITE,
                                                                  "ANY",
                                                                  IntStream.rangeClosed(0, 99).mapToObj(i -> ANY_ELEMENT_REF_OPT)
                                                                           .collect(toList()),
                                                                  Collections.emptyList(),
                                                                  null,
                                                                  null);

    final String xmlns;

    final QName qnSchema;
    final QName qnInclude;
    final QName qnDescription;
    final QName qnInterchange;
    final QName qnGroup;
    final QName qnTransaction;
    final QName qnImplementation;
    final QName qnLoop;
    final QName qnSegment;
    final QName qnComposite;
    final QName qnElement;
    final QName qnSyntax;
    final QName qnPosition;
    final QName qnSequence;
    final QName qnEnumeration;
    final QName qnValue;
    final QName qnVersion;
    final QName qnAny;

    final QName qnCompositeType;
    final QName qnElementType;
    final QName qnSegmentType;

    final Map<QName, EDIType.Type> complex;
    final Map<QName, EDIType.Type> typeDefinitions;
    final Map<QName, EDIType.Type> references;

    protected XMLStreamReader reader;
    protected Map<String, Object> properties;

    protected SchemaReaderBase(String xmlns, XMLStreamReader reader, Map<String, Object> properties) {
        this.xmlns = xmlns;
        qnSchema = new QName(xmlns, "schema");
        qnInclude = new QName(xmlns, "include");
        qnDescription = new QName(xmlns, "description");
        qnInterchange = new QName(xmlns, "interchange");
        qnGroup = new QName(xmlns, "group");
        qnTransaction = new QName(xmlns, "transaction");
        qnImplementation = new QName(xmlns, "implementation");
        qnLoop = new QName(xmlns, "loop");
        qnSegment = new QName(xmlns, "segment");
        qnComposite = new QName(xmlns, LOCALNAME_COMPOSITE);
        qnElement = new QName(xmlns, LOCALNAME_ELEMENT);
        qnSyntax = new QName(xmlns, "syntax");
        qnPosition = new QName(xmlns, "position");
        qnSequence = new QName(xmlns, "sequence");
        qnEnumeration = new QName(xmlns, "enumeration");
        qnValue = new QName(xmlns, "value");
        qnVersion = new QName(xmlns, "version");
        qnAny = new QName(xmlns, "any");

        qnCompositeType = new QName(xmlns, "compositeType");
        qnElementType = new QName(xmlns, "elementType");
        qnSegmentType = new QName(xmlns, "segmentType");

        complex = new HashMap<>(4);
        complex.put(qnInterchange, EDIType.Type.INTERCHANGE);
        complex.put(qnGroup, EDIType.Type.GROUP);
        complex.put(qnTransaction, EDIType.Type.TRANSACTION);
        complex.put(qnLoop, EDIType.Type.LOOP);
        complex.put(qnSegmentType, EDIType.Type.SEGMENT);
        complex.put(qnCompositeType, EDIType.Type.COMPOSITE);

        typeDefinitions = new HashMap<>(3);
        typeDefinitions.put(qnSegmentType, EDIType.Type.SEGMENT);
        typeDefinitions.put(qnCompositeType, EDIType.Type.COMPOSITE);
        typeDefinitions.put(qnElementType, EDIType.Type.ELEMENT);

        references = new HashMap<>(4);
        references.put(qnSegment, EDIType.Type.SEGMENT);
        references.put(qnComposite, EDIType.Type.COMPOSITE);
        references.put(qnElement, EDIType.Type.ELEMENT);

        this.reader = reader;
        this.properties = properties;
    }

    @Override
    public Map<String, EDIType> readTypes(boolean setReferences) throws EDISchemaException {
        Map<String, EDIType> types = new HashMap<>(100);

        types.put(StaEDISchema.ANY_ELEMENT_ID, ANY_ELEMENT);
        types.put(StaEDISchema.ANY_COMPOSITE_ID, ANY_COMPOSITE);

        nextTag(reader, "advancing to first schema element");
        QName element = reader.getName();

        while (qnInclude.equals(element)) {
            readInclude(reader, types);
            element = reader.getName();
        }

        if (qnInterchange.equals(element)) {
            readInterchange(reader, types);
        } else if (qnTransaction.equals(element)) {
            readTransaction(reader, types);
            readImplementation(reader, types);
        } else if (qnImplementation.equals(element)) {
            readImplementation(reader, types);
        } else if (!typeDefinitions.containsKey(reader.getName())) {
            throw unexpectedElement(element, reader);
        }

        readTypeDefinitions(reader, types);

        try {
            reader.next();
        } catch (XMLStreamException xse) {
            throw schemaException("XMLStreamException reading end of document", reader, xse);
        }

        requireEvent(XMLStreamConstants.END_DOCUMENT, reader);

        if (setReferences) {
            setReferences(types);
        }

        return types;
    }

    String readDescription(XMLStreamReader reader) {
        nextTag(reader, "seeking description element");
        QName element = reader.getName();
        String description = null;

        if (qnDescription.equals(element)) {
            description = getElementText(reader, "description");
            nextTag(reader, "after description element");
        }

        return description;
    }

    void readInterchange(XMLStreamReader reader, Map<String, EDIType> types) {
        QName element;

        Reference headerRef = createControlReference(reader, "header");
        Reference trailerRef = createControlReference(reader, "trailer");
        String title = parseAttribute(reader, ATTR_TITLE, String::valueOf, null);
        String descr = readDescription(reader);

        element = reader.getName();

        if (!qnSequence.equals(element)) {
            throw unexpectedElement(element, reader);
        }

        nextTag(reader, "reading interchange sequence");
        element = reader.getName();

        List<EDIReference> refs = new ArrayList<>(3);
        refs.add(headerRef);

        while (qnSegment.equals(element)) {
            addReferences(reader, EDIType.Type.SEGMENT, refs, readReference(reader, types));
            nextTag(reader, "completing interchange segment"); // Advance to end element
            nextTag(reader, "reading after interchange segment"); // Advance to next start element
            element = reader.getName();
        }

        if (qnGroup.equals(element)) {
            refs.add(readControlStructure(reader, element, qnTransaction, types));
            nextTag(reader, "completing group"); // Advance to end element
            nextTag(reader, "reading after group"); // Advance to next start element
            element = reader.getName();
        }

        if (qnTransaction.equals(element)) {
            refs.add(readControlStructure(reader, element, null, types));
            nextTag(reader, "completing transaction"); // Advance to end element
            nextTag(reader, "reading after transaction"); // Advance to next start element
            element = reader.getName();
        }

        refs.add(trailerRef);

        final List<EDISyntaxRule> rules;

        if (qnSyntax.equals(element)) {
            rules = new ArrayList<>(2);
            readSyntaxList(reader, rules);
        } else {
            rules = Collections.emptyList();
        }

        StructureType interchange = new StructureType(StaEDISchema.INTERCHANGE_ID,
                                                      EDIType.Type.INTERCHANGE,
                                                      "INTERCHANGE",
                                                      refs,
                                                      rules,
                                                      title,
                                                      descr);

        types.put(interchange.getId(), interchange);
        nextTag(reader, "advancing after interchange");
    }

    Reference readControlStructure(XMLStreamReader reader,
                                   QName element,
                                   QName subelement,
                                   Map<String, EDIType> types) {
        int minOccurs = 0;
        int maxOccurs = 99999;
        String use = parseAttribute(reader, "use", String::valueOf, "optional");

        switch (use) {
        case "required":
            minOccurs = 1;
            break;
        case "optional":
            minOccurs = 0;
            break;
        case "prohibited":
            maxOccurs = 0;
            break;
        default:
            throw schemaException("Invalid value for 'use': " + use, reader);
        }

        Reference headerRef = createControlReference(reader, "header");
        Reference trailerRef = createControlReference(reader, "trailer");
        String title = parseAttribute(reader, ATTR_TITLE, String::valueOf, null);
        String descr = readDescription(reader);

        if (subelement != null) {
            requireElementStart(subelement, reader);
        }

        List<EDIReference> refs = new ArrayList<>(3);
        refs.add(headerRef);
        if (subelement != null) {
            refs.add(readControlStructure(reader, subelement, null, types));
        }
        refs.add(trailerRef);

        Type elementType = complex.get(element);
        String elementId = StaEDISchema.ID_PREFIX + elementType.name();

        StructureType struct = new StructureType(elementId,
                                                 elementType,
                                                 elementType.toString(),
                                                 refs,
                                                 Collections.emptyList(),
                                                 title,
                                                 descr);

        types.put(struct.getId(), struct);

        Reference structRef = new Reference(struct.getId(), elementType, minOccurs, maxOccurs, title, descr);
        structRef.setReferencedType(struct);

        return structRef;
    }

    Reference createControlReference(XMLStreamReader reader, String attributeName) {
        final String refId = parseAttribute(reader, attributeName, String::valueOf);
        return new Reference(refId, EDIType.Type.SEGMENT, 1, 1, null, null);
    }

    void readTransaction(XMLStreamReader reader, Map<String, EDIType> types) {
        QName element = reader.getName();
        types.put(StaEDISchema.TRANSACTION_ID, readComplexType(reader, element, types));
        nextTag(reader, "seeking next element after transaction");
    }

    void readTypeDefinitions(XMLStreamReader reader, Map<String, EDIType> types) {
        boolean schemaEnd = reader.getName().equals(qnSchema);

        // Cursor is already positioned on a type definition (e.g. from an earlier look-ahead)
        if (typeDefinitions.containsKey(reader.getName())
                && reader.getEventType() == XMLStreamConstants.START_ELEMENT) {
            readTypeDefinition(types, reader);
        }

        while (!schemaEnd) {
            if (nextTag(reader, "reading schema types") == XMLStreamConstants.START_ELEMENT) {
                readTypeDefinition(types, reader);
            } else {
                schemaEnd = reader.getName().equals(qnSchema);
            }
        }
    }

    void readTypeDefinition(Map<String, EDIType> types, XMLStreamReader reader) {
        QName element = reader.getName();
        String name;

        if (complex.containsKey(element)) {
            name = parseAttribute(reader, "name", String::valueOf);
            nameCheck(name, types, reader);
            types.put(name, readComplexType(reader, element, types));
        } else if (qnElementType.equals(element)) {
            name = parseAttribute(reader, "name", String::valueOf);
            nameCheck(name, types, reader);
            types.put(name, readSimpleType(reader));
        } else {
            throw unexpectedElement(element, reader);
        }
    }

    void nameCheck(String name, Map<String, EDIType> types, XMLStreamReader reader) {
        if (types.containsKey(name)) {
            throw schemaException("duplicate name: " + name, reader);
        }
    }

    StructureType readComplexType(XMLStreamReader reader,
                                  QName complexType,
                                  Map<String, EDIType> types) {

        final EDIType.Type type = complex.get(complexType);
        final String id;
        String code = parseAttribute(reader, "code", String::valueOf, null);
        EDIElementPosition levelIdPosition = null;
        EDIElementPosition parentIdPosition = null;

        if (qnTransaction.equals(complexType)) {
            id = StaEDISchema.TRANSACTION_ID;
        } else if (qnLoop.equals(complexType)) {
            id = code;
            levelIdPosition = parseElementPosition(reader, ATTR_LEVEL_ID_POSITION);
            parentIdPosition = parseElementPosition(reader, ATTR_PARENT_ID_POSITION);
        } else {
            id = parseAttribute(reader, "name", String::valueOf);

            if (type == EDIType.Type.SEGMENT && !id.matches("^[A-Z][A-Z0-9]{1,2}$")) {
                throw schemaException("Invalid segment name [" + id + ']', reader);
            }
        }

        if (code == null) {
            code = id;
        }

        final List<EDIReference> refs = new ArrayList<>(8);
        final List<EDISyntaxRule> rules = new ArrayList<>(2);

        String title = parseAttribute(reader, ATTR_TITLE, String::valueOf, null);
        String descr = readDescription(reader);
        requireElementStart(qnSequence, reader);
        readReferences(reader, types, type, refs);

        int event = nextTag(reader, "searching for syntax element");

        if (event == XMLStreamConstants.START_ELEMENT) {
            requireElementStart(qnSyntax, reader);
            readSyntaxList(reader, rules);
        }

        event = reader.getEventType();

        if (event == XMLStreamConstants.END_ELEMENT) {
            StructureType structure;

            if (qnLoop.equals(complexType)) {
                structure = new LoopType(code, refs, rules, levelIdPosition, parentIdPosition, title, descr);
            } else {
                structure = new StructureType(id, type, code, refs, rules, title, descr);
            }

            return structure;
        } else {
            throw unexpectedEvent(reader);
        }
    }

    void readReferences(XMLStreamReader reader,
                        Map<String, EDIType> types,
                        EDIType.Type parentType,
                        List<EDIReference> refs) {

        boolean endOfReferences = false;

        while (!endOfReferences) {
            int event = nextTag(reader, "reading sequence");

            if (event == XMLStreamConstants.START_ELEMENT) {
                addReferences(reader, parentType, refs, readReference(reader, types));
            } else {
                if (reader.getName().equals(qnSequence)) {
                    endOfReferences = true;
                }
            }
        }
    }

    void addReferences(XMLStreamReader reader, EDIType.Type parentType, List<EDIReference> refs, Reference reference) {
        if ("ANY".equals(reference.getRefId())) {
            final EDIReference optRef;
            final EDIReference reqRef;

            switch (parentType) {
            case SEGMENT:
                optRef = ANY_COMPOSITE_REF_OPT;
                reqRef = ANY_COMPOSITE_REF_REQ;
                break;
            case COMPOSITE:
                optRef = ANY_ELEMENT_REF_OPT;
                reqRef = ANY_ELEMENT_REF_REQ;
                break;
            default:
                throw schemaException("Element " + qnAny + " may only be present for segmentType and compositeType", reader);
            }

            final int min = reference.getMinOccurs();
            final int max = reference.getMaxOccurs();

            for (int i = 0; i < max; i++) {
                refs.add(i < min ? reqRef : optRef);
            }
        } else {
            refs.add(reference);
        }
    }

    Reference readReference(XMLStreamReader reader, Map<String, EDIType> types) {
        QName element = reader.getName();
        String refId = null;
        EDIType.Type refTag;

        if (qnAny.equals(element)) {
            refId = "ANY";
            refTag = null;
        } else if (references.containsKey(element)) {
            refId = readReferencedId(reader);
            refTag = references.get(element);
            Objects.requireNonNull(refId);
        } else if (qnLoop.equals(element)) {
            refId = parseAttribute(reader, "code", String::valueOf);
            refTag = EDIType.Type.LOOP;
        } else {
            throw unexpectedElement(element, reader);
        }

        int minOccurs = parseAttribute(reader, ATTR_MIN_OCCURS, Integer::parseInt, 0);
        int maxOccurs = parseAttribute(reader, ATTR_MAX_OCCURS, Integer::parseInt, 1);
        String title = parseAttribute(reader, ATTR_TITLE, String::valueOf, null);

        Reference ref;

        if (qnLoop.equals(element)) {
            StructureType loop = readComplexType(reader, element, types);
            nameCheck(refId, types, reader);
            types.put(refId, loop);
            ref = new Reference(refId, refTag, minOccurs, maxOccurs, title, null);
            ref.setReferencedType(loop);
        } else if (qnComposite.equals(element) || qnElement.equals(element)) {
            List<Reference.Version> versions = null;

            if (nextTag(reader, "reading " + element + " contents") != XMLStreamConstants.END_ELEMENT) {
                requireElementStart(qnVersion, reader);
                versions = new ArrayList<>();

                do {
                    versions.add(readReferenceVersion(reader));
                } while (nextTag(reader, "reading after " + element + " version") != XMLStreamConstants.END_ELEMENT);
            } else {
                versions = Collections.emptyList();
            }

            ref = new Reference(refId, refTag, minOccurs, maxOccurs, versions, title, null);
        } else {
            ref = new Reference(refId, refTag, minOccurs, maxOccurs, title, null);
        }

        return ref;
    }

    Reference.Version readReferenceVersion(XMLStreamReader reader) {
        requireElementStart(qnVersion, reader);
        String minVersion = parseAttribute(reader, "minVersion", String::valueOf, "");
        String maxVersion = parseAttribute(reader, "maxVersion", String::valueOf, "");
        Integer minOccurs = parseAttribute(reader, ATTR_MIN_OCCURS, Integer::valueOf, null);
        Integer maxOccurs = parseAttribute(reader, ATTR_MAX_OCCURS, Integer::valueOf, null);

        if (nextTag(reader, "reading version contents") != XMLStreamConstants.END_ELEMENT) {
            throw unexpectedElement(reader.getName(), reader);
        }

        return new Reference.Version(minVersion, maxVersion, minOccurs, maxOccurs);
    }

    void readSyntaxList(XMLStreamReader reader, List<EDISyntaxRule> rules) {
        do {
            readSyntax(reader, rules);
            nextTag(reader, "reading after syntax element");
        } while (qnSyntax.equals(reader.getName()));
    }

    void readSyntax(XMLStreamReader reader, List<EDISyntaxRule> rules) {
        String type = parseAttribute(reader, "type", String::valueOf);
        EDISyntaxRule.Type typeInt = null;

        try {
            typeInt = EDISyntaxRule.Type.fromString(type);
        } catch (IllegalArgumentException e) {
            throw schemaException("Invalid syntax 'type': [" + type + ']', reader, e);
        }

        rules.add(new SyntaxRestriction(typeInt, readSyntaxPositions(reader)));
    }

    List<Integer> readSyntaxPositions(XMLStreamReader reader) {
        final List<Integer> positions = new ArrayList<>(5);
        boolean endOfSyntax = false;

        while (!endOfSyntax) {
            final int event = nextTag(reader, "reading syntax positions");
            final QName element = reader.getName();

            if (event == XMLStreamConstants.START_ELEMENT) {
                if (element.equals(qnPosition)) {
                    final String position = getElementText(reader, "syntax position");

                    try {
                        positions.add(Integer.parseInt(position));
                    } catch (@SuppressWarnings("unused") NumberFormatException e) {
                        throw schemaException("invalid position [" + position + ']', reader);
                    }
                }
            } else {
                endOfSyntax = true;
            }
        }

        return positions;
    }

    ElementType readSimpleType(XMLStreamReader reader) {
        String name = parseAttribute(reader, "name", String::valueOf);
        String code = parseAttribute(reader, "code", String::valueOf, name);
        Base base = parseAttribute(reader, "base", Base::fromString, Base.STRING);
        int scale = (Base.NUMERIC == base) ? parseAttribute(reader, "scale", Integer::parseInt, 0) : -1;
        int number = parseAttribute(reader, "number", Integer::parseInt, -1);
        long minLength = parseAttribute(reader, "minLength", Long::parseLong, 1L);
        long maxLength = parseAttribute(reader, "maxLength", Long::parseLong, 1L);
        String title = parseAttribute(reader, ATTR_TITLE, String::valueOf, null);
        String descr = readDescription(reader);

        final Set<String> values;
        final List<ElementType.Version> versions;

        // Reader was advanced by `readDescription`, check the current state to proceed.
        if (reader.getEventType() == XMLStreamConstants.END_ELEMENT) {
            values = Collections.emptySet();
            versions = Collections.emptyList();
        } else {
            if (qnEnumeration.equals(reader.getName())) {
                values = readEnumerationValues(reader);
                nextTag(reader, "reading after elementType enumeration");
            } else {
                values = Collections.emptySet();
            }

            if (qnVersion.equals(reader.getName())) {
                versions = new ArrayList<>();

                do {
                    versions.add(readSimpleTypeVersion(reader));
                } while (nextTag(reader, "reading after elementType version") != XMLStreamConstants.END_ELEMENT);
            } else {
                versions = Collections.emptyList();
            }
        }

        return new ElementType(name, base, scale, code, number, minLength, maxLength, values, versions, title, descr);
    }

    ElementType.Version readSimpleTypeVersion(XMLStreamReader reader) {
        requireElementStart(qnVersion, reader);
        String minVersion = parseAttribute(reader, "minVersion", String::valueOf, "");
        String maxVersion = parseAttribute(reader, "maxVersion", String::valueOf, "");
        Long minLength = parseAttribute(reader, "minLength", Long::valueOf, null);
        Long maxLength = parseAttribute(reader, "maxLength", Long::valueOf, null);

        Set<String> values;

        if (nextTag(reader, "reading elementType version contents") == XMLStreamConstants.END_ELEMENT) {
            // Set to null instead of empty to indicate that no enumeration is present in this version
            values = null;
        } else if (qnEnumeration.equals(reader.getName())) {
            values = readEnumerationValues(reader);
            nextTag(reader, "reading after elementType version enumeration");
        } else {
            throw unexpectedElement(reader.getName(), reader);
        }

        return new ElementType.Version(minVersion, maxVersion, minLength, maxLength, values);
    }

    Set<String> readEnumerationValues(XMLStreamReader reader) {
        Set<String> values = null;
        boolean endOfEnumeration = false;

        while (!endOfEnumeration) {
            final int event = nextTag(reader, "reading enumeration");
            final QName element = reader.getName();

            if (event == XMLStreamConstants.START_ELEMENT) {
                if (element.equals(qnValue)) {
                    values = readEnumerationValue(reader, values);
                } else {
                    throw unexpectedElement(element, reader);
                }
            } else {
                endOfEnumeration = true;
            }
        }

        return values != null ? values : Collections.emptySet();
    }

    Set<String> readEnumerationValue(XMLStreamReader reader, Set<String> values) {
        if (values == null) {
            values = new LinkedHashSet<>();
        }

        values.add(getElementText(reader, "enumeration value"));

        return values;
    }

    <T> T parseAttribute(XMLStreamReader reader, String attrName, Function<String, T> converter, T defaultValue) {
        String attr = reader.getAttributeValue(null, attrName);

        try {
            return attr != null ? converter.apply(attr) : defaultValue;
        } catch (Exception e) {
            throw schemaException("Invalid " + attrName, reader, e);
        }
    }

    <T> T parseAttribute(XMLStreamReader reader, String attrName, Function<String, T> converter) {
        String attr = reader.getAttributeValue(null, attrName);

        if (attr != null) {
            try {
                return converter.apply(attr);
            } catch (Exception e) {
                throw schemaException("Invalid " + attrName, reader, e);
            }
        } else {
            throw schemaException("Missing required attribute: [" + attrName + ']', reader);
        }
    }

    int nextTag(XMLStreamReader reader, String activity) {
        try {
            if (!reader.hasNext()) {
                throw schemaException("End of stream reached while " + activity, reader, null);
            }
            return reader.nextTag();
        } catch (XMLStreamException xse) {
            throw schemaException("XMLStreamException while " + activity, reader, xse);
        }
    }

    String getElementText(XMLStreamReader reader, String context) {
        try {
            return reader.getElementText();
        } catch (XMLStreamException xse) {
            throw schemaException("XMLStreamException reading element text: " + context, reader, xse);
        }
    }

    void requireEvent(int eventId, XMLStreamReader reader) {
        Integer event = reader.getEventType();

        if (event != eventId) {
            throw unexpectedEvent(reader);
        }
    }

    void requireElement(QName element, XMLStreamReader reader) {
        Integer event = reader.getEventType();

        if (event != XMLStreamConstants.START_ELEMENT && event != XMLStreamConstants.END_ELEMENT) {
            throw unexpectedEvent(reader);
        }

        if (!element.equals(reader.getName())) {
            throw unexpectedElement(reader.getName(), reader);
        }
    }

    void requireElementStart(QName element, XMLStreamReader reader) {
        Integer event = reader.getEventType();

        if (event != XMLStreamConstants.START_ELEMENT) {
            throw schemaException("Expected XML element [" + element + "] not found", reader);
        }

        if (!element.equals(reader.getName())) {
            throw unexpectedElement(reader.getName(), reader);
        }
    }

    void setReferences(Map<String, EDIType> types) {
        types.values()
             .stream()
             .filter(StructureType.class::isInstance)
             .forEach(struct -> setReferences((StructureType) struct, types));

        logUnusedTypes(types, Level.INFO);
    }

    void setReferences(StructureType struct, Map<String, EDIType> types) {
        for (EDIReference ref : struct.getReferences()) {
            Reference impl = (Reference) ref;
            EDIType target = types.get(impl.getRefId());

            if (target == null) {
                throw schemaException(String.format(REFERR_UNDECLARED, struct.getId(), impl.getRefTag().name().toLowerCase(Locale.ROOT), impl.getRefId()));
            }

            final EDIType.Type refType = target.getType();

            if (refType != impl.getRefTag()) {
                throw schemaException(String.format(REFERR_ILLEGAL, impl.getRefId(), impl.getRefTag().name().toLowerCase(Locale.ROOT), struct.getId()));
            }

            impl.setReferencedType(target);
        }
    }

    void logUnusedTypes(Map<String, EDIType> types, Level level) {
        if (LOGGER.isLoggable(level)) {
            Set<String> unused = new HashSet<>(types.keySet());
            unused.remove(StaEDISchema.INTERCHANGE_ID);
            unused.remove(StaEDISchema.TRANSACTION_ID);
            unused.remove(StaEDISchema.IMPLEMENTATION_ID);
            unused.remove(ANY_COMPOSITE.getId());

            types.values()
                 .stream()
                 .filter(StructureType.class::isInstance)
                 .map(StructureType.class::cast)
                 .flatMap(struct -> struct.getReferences().stream())
                 .forEach(ref -> unused.remove(ref.getReferencedType().getId()));

            for (String u : unused) {
                LOGGER.log(level, String.format("Unused %s '%s'", types.get(u).getType(), u));
            }
        }
    }

    EDIElementPosition parseElementPosition(XMLStreamReader reader, String attrName) {
        BigDecimal positionAttr = parseAttribute(reader, attrName, BigDecimal::new, null);

        if (positionAttr == null) {
            return null;
        }

        int elementPosition = positionAttr.intValue();
        int componentPosition = positionAttr.remainder(BigDecimal.ONE)
                                                 .movePointRight(positionAttr.scale())
                                                 .intValue();

        return new ElementPosition(elementPosition, componentPosition);
    }

    protected abstract String readReferencedId(XMLStreamReader reader);

    protected abstract void readInclude(XMLStreamReader reader, Map<String, EDIType> types) throws EDISchemaException;

    protected abstract void readImplementation(XMLStreamReader reader, Map<String, EDIType> types);
}
