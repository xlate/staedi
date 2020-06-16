package io.xlate.edi.internal.schema;

import static io.xlate.edi.internal.schema.StaEDISchemaFactory.schemaException;
import static io.xlate.edi.internal.schema.StaEDISchemaFactory.unexpectedElement;
import static io.xlate.edi.internal.schema.StaEDISchemaFactory.unexpectedEvent;
import static java.util.stream.Collectors.toList;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.IntStream;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import io.xlate.edi.schema.EDIComplexType;
import io.xlate.edi.schema.EDIReference;
import io.xlate.edi.schema.EDISchemaException;
import io.xlate.edi.schema.EDISimpleType;
import io.xlate.edi.schema.EDISimpleType.Base;
import io.xlate.edi.schema.EDISyntaxRule;
import io.xlate.edi.schema.EDIType;
import io.xlate.edi.schema.EDIType.Type;

abstract class SchemaReaderBase implements SchemaReader {

    static final String REFERR_UNDECLARED = "Type %s references undeclared %s with ref='%s'";
    static final String REFERR_ILLEGAL = "Type '%s' must not be referenced as '%s' in definition of type '%s'";

    static final String LOCALNAME_ELEMENT = "element";
    static final String LOCALNAME_COMPOSITE = "composite";

    static final EDIReference ANY_ELEMENT_REF_OPT = new Reference(StaEDISchema.ANY_ELEMENT_ID, LOCALNAME_ELEMENT, 0, 1);
    static final EDIReference ANY_COMPOSITE_REF_OPT = new Reference(StaEDISchema.ANY_COMPOSITE_ID, LOCALNAME_COMPOSITE, 0, 99);

    static final EDIReference ANY_ELEMENT_REF_REQ = new Reference(StaEDISchema.ANY_ELEMENT_ID, LOCALNAME_ELEMENT, 1, 1);
    static final EDIReference ANY_COMPOSITE_REF_REQ = new Reference(StaEDISchema.ANY_COMPOSITE_ID, LOCALNAME_COMPOSITE, 1, 99);

    static final EDISimpleType ANY_ELEMENT = new ElementType(StaEDISchema.ANY_ELEMENT_ID,
                                                             Base.STRING,
                                                             "ANY",
                                                             0,
                                                             0,
                                                             99_999,
                                                             Collections.emptySet(),
                                                             Collections.emptyList());

    static final EDIComplexType ANY_COMPOSITE = new StructureType(StaEDISchema.ANY_COMPOSITE_ID,
                                                                  Type.COMPOSITE,
                                                                  "ANY",
                                                                  IntStream.rangeClosed(0, 99).mapToObj(i -> ANY_ELEMENT_REF_OPT)
                                                                           .collect(toList()),
                                                                  Collections.emptyList());

    final String xmlns;

    final QName qnSchema;
    final QName qnInclude;
    final QName qnDescription;
    final QName qnInterchange;
    final QName qnGroup;
    final QName qnTransaction;
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
    final Set<QName> references;

    protected XMLStreamReader reader;

    public SchemaReaderBase(String xmlns, XMLStreamReader reader) {
        this.xmlns = xmlns;
        qnSchema = new QName(xmlns, "schema");
        qnInclude = new QName(xmlns, "include");
        qnDescription = new QName(xmlns, "description");
        qnInterchange = new QName(xmlns, "interchange");
        qnGroup = new QName(xmlns, "group");
        qnTransaction = new QName(xmlns, "transaction");
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

        references = new HashSet<>(4);
        references.add(qnSegment);
        references.add(qnComposite);
        references.add(qnElement);

        this.reader = reader;
    }

    @Override
    public String getInterchangeName() {
        return StaEDISchema.INTERCHANGE_ID;
    }

    @Override
    public String getTransactionName() {
        return StaEDISchema.TRANSACTION_ID;
    }

    @Override
    public Map<String, EDIType> readTypes() throws EDISchemaException {
        Map<String, EDIType> types = new HashMap<>(100);

        types.put(StaEDISchema.ANY_ELEMENT_ID, ANY_ELEMENT);
        types.put(StaEDISchema.ANY_COMPOSITE_ID, ANY_COMPOSITE);

        try {
            reader.nextTag();
            QName element = reader.getName();

            if (qnInclude.equals(element)) {
                readInclude(reader, types);
                readImplementation(reader, types);
            } else if (qnInterchange.equals(element)) {
                readInterchange(reader, types);
            } else if (qnTransaction.equals(element)) {
                readTransaction(reader, types);
                readImplementation(reader, types);
            } else {
                throw unexpectedElement(element, reader);
            }

            readTypeDefinitions(reader, types);
            setReferences(types);
            reader.next();
            requireEvent(XMLStreamConstants.END_DOCUMENT, reader);
        } catch (XMLStreamException xse) {
            throw schemaException("XMLStreamException reading schema", reader, xse);
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

        readDescription(reader);

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
        }

        refs.add(trailerRef);

        StructureType interchange = new StructureType(StaEDISchema.INTERCHANGE_ID,
                                                      EDIType.Type.INTERCHANGE,
                                                      "INTERCHANGE",
                                                      refs,
                                                      Collections.emptyList());

        types.put(interchange.getId(), interchange);
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

        readDescription(reader);

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
                                                 Collections.emptyList());

        types.put(struct.getId(), struct);

        Reference structRef = new Reference(struct.getId(), element.getLocalPart(), minOccurs, maxOccurs);
        structRef.setReferencedType(struct);

        return structRef;
    }

    Reference createControlReference(XMLStreamReader reader, String attributeName) {
        final String refId = parseAttribute(reader, attributeName, String::valueOf);
        return new Reference(refId, "segment", 1, 1);
    }

    void readTransaction(XMLStreamReader reader, Map<String, EDIType> types) {
        QName element = reader.getName();
        types.put(StaEDISchema.TRANSACTION_ID, readComplexType(reader, element, types));
    }

    void readTypeDefinitions(XMLStreamReader reader, Map<String, EDIType> types) {
        boolean schemaEnd = false;

        // Cursor is already positioned on a type definition (e.g. from an earlier look-ahead)
        if (typeDefinitions.containsKey(reader.getName())
                && reader.getEventType() == XMLStreamConstants.START_ELEMENT) {
            readTypeDefinition(types, reader);
        }

        while (!schemaEnd) {
            if (nextTag(reader, "reading schema types") == XMLStreamConstants.START_ELEMENT) {
                readTypeDefinition(types, reader);
            } else {
                if (reader.getName().equals(qnSchema)) {
                    schemaEnd = true;
                }
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

        if (qnTransaction.equals(complexType)) {
            id = StaEDISchema.TRANSACTION_ID;
        } else if (qnLoop.equals(complexType)) {
            id = code;
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

        readDescription(reader);
        requireElementStart(qnSequence, reader);
        readReferences(reader, types, type, refs);

        int event = nextTag(reader, "searching for syntax element");

        if (event == XMLStreamConstants.START_ELEMENT) {
            requireElementStart(qnSyntax, reader);
            do {
                readSyntax(reader, rules);
                event = nextTag(reader, "reading syntax elements");
            } while (event == XMLStreamConstants.START_ELEMENT && qnSyntax.equals(reader.getName()));
        }

        if (event == XMLStreamConstants.END_ELEMENT) {
            return new StructureType(id, type, code, refs, rules);
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

        if (qnAny.equals(element)) {
            refId = "ANY";
        } else if (references.contains(element)) {
            refId = readReferencedId(reader);
            Objects.requireNonNull(refId);
        } else if (qnLoop.equals(element)) {
            refId = parseAttribute(reader, "code", String::valueOf);
        } else {
            throw unexpectedElement(element, reader);
        }

        String refTag = element.getLocalPart();
        int minOccurs = parseAttribute(reader, "minOccurs", Integer::parseInt, 0);
        int maxOccurs = parseAttribute(reader, "maxOccurs", Integer::parseInt, 1);

        Reference ref;

        if (qnLoop.equals(element)) {
            StructureType loop = readComplexType(reader, element, types);
            nameCheck(refId, types, reader);
            types.put(refId, loop);
            ref = new Reference(refId, refTag, minOccurs, maxOccurs);
            ref.setReferencedType(loop);
        } else {
            ref = new Reference(refId, refTag, minOccurs, maxOccurs);
        }

        return ref;
    }

    void readSyntax(XMLStreamReader reader, List<EDISyntaxRule> rules) {
        String type = parseAttribute(reader, "type", String::valueOf);
        EDISyntaxRule.Type typeInt = null;

        try {
            typeInt = EDISyntaxRule.Type.valueOf(type.toUpperCase());
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
        Base base = parseAttribute(reader, "base", val -> Base.valueOf(val.toUpperCase()), Base.STRING);
        int number = parseAttribute(reader, "number", Integer::parseInt, -1);
        long minLength = parseAttribute(reader, "minLength", Long::parseLong, 1L);
        long maxLength = parseAttribute(reader, "maxLength", Long::parseLong, 1L);

        final Set<String> values;
        final List<ElementType.Version> versions;

        if (nextTag(reader, "reading elementType contents") == XMLStreamConstants.END_ELEMENT) {
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

        return new ElementType(name, base, code, number, minLength, maxLength, values, versions);
    }

    ElementType.Version readSimpleTypeVersion(XMLStreamReader reader) {
        requireElementStart(qnVersion, reader);
        String minVersion = parseAttribute(reader, "minVersion", String::valueOf, "");
        String maxVersion = parseAttribute(reader, "maxVersion", String::valueOf, "");
        Long minLength = parseAttribute(reader, "minLength", Long::parseLong, null);
        Long maxLength = parseAttribute(reader, "maxLength", Long::parseLong, null);

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

    void requireElementStart(QName element, XMLStreamReader reader) {
        Integer event = reader.getEventType();

        if (event != XMLStreamConstants.START_ELEMENT) {
            throw schemaException("Expected XML element [" + element + "] not found", reader);
        }

        if (!element.equals(reader.getName())) {
            throw schemaException("Unexpected XML element [" + reader.getName() + "]", reader);
        }
    }

    void setReferences(Map<String, EDIType> types) {
        types.values()
             .stream()
             .filter(type -> type instanceof StructureType)
             .forEach(struct -> setReferences((StructureType) struct, types));
    }

    void setReferences(StructureType struct, Map<String, EDIType> types) {
        for (EDIReference ref : struct.getReferences()) {
            Reference impl = (Reference) ref;
            EDIType target = types.get(impl.getRefId());

            if (target == null) {
                throw schemaException(String.format(REFERR_UNDECLARED, struct.getId(), impl.getRefTag(), impl.getRefId()));
            }

            final EDIType.Type refType = target.getType();

            if (refType != EDIType.Type.valueOf(impl.getRefTag().toUpperCase())) {
                throw schemaException(String.format(REFERR_ILLEGAL, impl.getRefId(), impl.getRefTag(), struct.getId()));
            }

            impl.setReferencedType(target);
        }
    }

    protected abstract String readReferencedId(XMLStreamReader reader);

    protected abstract void readInclude(XMLStreamReader reader, Map<String, EDIType> types) throws EDISchemaException;

    protected abstract void readImplementation(XMLStreamReader reader, Map<String, EDIType> types) throws XMLStreamException;
}
