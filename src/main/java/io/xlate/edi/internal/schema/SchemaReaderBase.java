package io.xlate.edi.internal.schema;

import static io.xlate.edi.internal.schema.StaEDISchemaFactory.schemaException;
import static io.xlate.edi.internal.schema.StaEDISchemaFactory.unexpectedElement;
import static io.xlate.edi.internal.schema.StaEDISchemaFactory.unexpectedEvent;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import io.xlate.edi.schema.EDIReference;
import io.xlate.edi.schema.EDISchemaException;
import io.xlate.edi.schema.EDISimpleType;
import io.xlate.edi.schema.EDISyntaxRule;
import io.xlate.edi.schema.EDIType;

abstract class SchemaReaderBase implements SchemaReader {

    static final String REFERR_UNDECLARED = "Type %s references undeclared %s with ref='%s'";
    static final String REFERR_ILLEGAL = "Type '%s' must not be referenced as '%s' in definition of type '%s'";

    final String xmlns;

    final QName qnSchema;
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
        qnDescription = new QName(xmlns, "description");
        qnInterchange = new QName(xmlns, "interchange");
        qnGroup = new QName(xmlns, "group");
        qnTransaction = new QName(xmlns, "transaction");
        qnLoop = new QName(xmlns, "loop");
        qnSegment = new QName(xmlns, "segment");
        qnComposite = new QName(xmlns, "composite");
        qnElement = new QName(xmlns, "element");
        qnSyntax = new QName(xmlns, "syntax");
        qnPosition = new QName(xmlns, "position");
        qnSequence = new QName(xmlns, "sequence");
        qnEnumeration = new QName(xmlns, "enumeration");
        qnValue = new QName(xmlns, "value");

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
        return qnInterchange.toString();
    }

    @Override
    public String getTransactionName() {
        return qnTransaction.toString();
    }

    @Override
    public Map<String, EDIType> readTypes() throws EDISchemaException {
        Map<String, EDIType> types = new HashMap<>(100);

        try {
            if (isInterchangeSchema(reader)) {
                readInterchange(reader, types);
            } else {
                readTransaction(reader, types);
            }

            readTypeDefinitions(reader, types);
            setReferences(types);
            reader.next();
            requireEvent(XMLStreamConstants.END_DOCUMENT, reader);
        } catch (XMLStreamException e) {
            throw new EDISchemaException(e);
        }

        return types;
    }

    boolean isInterchangeSchema(XMLStreamReader reader) throws XMLStreamException {
        QName element;
        reader.nextTag();
        element = reader.getName();

        if (!qnInterchange.equals(element) && !qnTransaction.equals(element)) {
            throw unexpectedElement(element, reader);
        }

        return qnInterchange.equals(element);
    }

    String readDescription(XMLStreamReader reader) throws XMLStreamException {
        QName element;

        reader.nextTag();
        element = reader.getName();
        String description = null;

        if (qnDescription.equals(element)) {
            description = reader.getElementText();
            reader.nextTag();
        }

        return description;
    }

    void readInterchange(XMLStreamReader reader, Map<String, EDIType> types) throws XMLStreamException {
        QName element;

        Reference headerRef = createControlReference(reader, "header");
        Reference trailerRef = createControlReference(reader, "trailer");

        readDescription(reader);

        element = reader.getName();

        if (!qnSequence.equals(element)) {
            throw unexpectedElement(element, reader);
        }

        reader.nextTag();
        element = reader.getName();

        List<EDIReference> refs = new ArrayList<>(3);
        refs.add(headerRef);

        while (qnSegment.equals(element)) {
            refs.add(readReference(reader, types));
            reader.nextTag(); // Advance to end element
            reader.nextTag(); // Advance to next start element
            element = reader.getName();
        }

        if (qnGroup.equals(element)) {
            refs.add(readControlStructure(reader, element, qnTransaction, types));
            reader.nextTag(); // Advance to end element
            reader.nextTag(); // Advance to next start element
            element = reader.getName();
        }

        if (qnTransaction.equals(element)) {
            refs.add(readControlStructure(reader, element, null, types));
            reader.nextTag(); // Advance to end element
            reader.nextTag(); // Advance to next start element
        }

        refs.add(trailerRef);

        StructureType interchange = new StructureType(qnInterchange.toString(),
                                              EDIType.Type.INTERCHANGE,
                                              "INTERCHANGE",
                                              refs,
                                              Collections.emptyList());

        types.put(interchange.getId(), interchange);
    }

    Reference readControlStructure(XMLStreamReader reader,
                                   QName element,
                                   QName subelement,
                                   Map<String, EDIType> types) throws XMLStreamException {
        int minOccurs = 0;
        int maxOccurs = 99999;

        String use = reader.getAttributeValue(null, "use");
        if (use != null) {
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
        }

        Reference headerRef = createControlReference(reader, "header");
        Reference trailerRef = createControlReference(reader, "trailer");

        readDescription(reader);
        QName nextElement = reader.getName();

        if (subelement != null && !subelement.equals(nextElement)) {
            throw unexpectedElement(nextElement, reader);
        }

        List<EDIReference> refs = new ArrayList<>(3);
        refs.add(headerRef);
        if (subelement != null) {
            refs.add(readControlStructure(reader, subelement, null, types));
        }
        refs.add(trailerRef);

        StructureType struct = new StructureType(element.toString(),
                                         complex.get(element),
                                         complex.get(element).toString(),
                                         refs,
                                         Collections.emptyList());

        types.put(struct.getId(), struct);

        Reference structRef = new Reference(struct.getId(), element.getLocalPart(), minOccurs, maxOccurs);
        structRef.setReferencedType(struct);

        return structRef;
    }

    Reference createControlReference(XMLStreamReader reader, String attributeName) {
        String refId = reader.getAttributeValue(null, attributeName);

        if (refId == null) {
            throw schemaException("Missing required attribute [" + attributeName + ']', reader);
        }

        return new Reference(refId, "segment", 1, 1);
    }

    void readTransaction(XMLStreamReader reader, Map<String, EDIType> types) throws XMLStreamException {
        QName element = reader.getName();
        types.put(qnTransaction.toString(), readComplexType(reader, element, types));
    }

    void readTypeDefinitions(XMLStreamReader reader, Map<String, EDIType> types) throws XMLStreamException {
        boolean schemaEnd = false;

        // Cursor is already positioned on a type definition (e.g. from an earlier look-ahead)
        if (typeDefinitions.containsKey(reader.getName())
                && reader.getEventType() == XMLStreamConstants.START_ELEMENT) {
            readTypeDefinition(types, reader);
        }

        while (reader.hasNext() && !schemaEnd) {
            switch (reader.next()) {
            case XMLStreamConstants.START_ELEMENT:
                readTypeDefinition(types, reader);
                break;

            case XMLStreamConstants.END_ELEMENT:
                if (reader.getName().equals(qnSchema)) {
                    schemaEnd = true;
                }
                break;

            default:
                checkEvent(reader);
                break;
            }
        }

    }

    void readTypeDefinition(Map<String, EDIType> types, XMLStreamReader reader) throws XMLStreamException {
        QName element = reader.getName();
        String name = reader.getAttributeValue(null, "name");

        if (complex.containsKey(element)) {
            nameCheck(name, types, reader);
            types.put(name, readComplexType(reader, element, types));
        } else if (qnElementType.equals(element)) {
            nameCheck(name, types, reader);
            types.put(name, readSimpleType(reader));
        } else {
            throw unexpectedElement(element, reader);
        }
    }

    void nameCheck(String name, Map<String, EDIType> types, XMLStreamReader reader) {
        if (name == null) {
            throw schemaException("missing type name", reader);
        }

        if (types.containsKey(name)) {
            throw schemaException("duplicate name: " + name, reader);
        }
    }

    StructureType readComplexType(XMLStreamReader reader,
                               QName complexType,
                               Map<String, EDIType> types) throws XMLStreamException {

        final EDIType.Type type = complex.get(complexType);
        final String id;
        String code = reader.getAttributeValue(null, "code");

        if (qnTransaction.equals(complexType)) {
            id = qnTransaction.toString();
        } else if (qnLoop.equals(complexType)) {
            id = code;
        } else {
            id = reader.getAttributeValue(null, "name");

            if (type == EDIType.Type.SEGMENT && !id.matches("^[A-Z][A-Z0-9]{1,2}$")) {
                throw schemaException("Invalid segment name [" + id + ']', reader);
            }
        }

        if (code == null) {
            code = id;
        }

        final List<EDIReference> refs = new ArrayList<>(8);
        final List<EDISyntaxRule> rules = new ArrayList<>(2);

        reader.nextTag();
        requireElementStart(qnSequence, reader);
        readReferences(reader, types, refs);

        int event = reader.nextTag();

        if (event == XMLStreamConstants.START_ELEMENT) {
            requireElementStart(qnSyntax, reader);
            readSyntax(reader, rules);
            event = reader.nextTag();
        }

        if (event == XMLStreamConstants.END_ELEMENT) {
            return new StructureType(id, type, code, refs, rules);
        } else {
            throw unexpectedEvent(reader);
        }
    }

    void readReferences(XMLStreamReader reader,
                       Map<String, EDIType> types,
                       List<EDIReference> refs) {

        try {
            while (reader.hasNext()) {
                switch (reader.next()) {
                case XMLStreamConstants.START_ELEMENT:
                    refs.add(readReference(reader, types));
                    break;

                case XMLStreamConstants.END_ELEMENT:
                    if (reader.getName().equals(qnSequence)) {
                        return;
                    }

                    break;
                default:
                    checkEvent(reader);
                    break;
                }
            }
        } catch (XMLStreamException xse) {
            throw schemaException("Exception reading sequence", reader, xse);
        }
    }

    Reference readReference(XMLStreamReader reader, Map<String, EDIType> types) {
        QName element = reader.getName();
        String refId = null;

        if (references.contains(element)) {
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
            StructureType loop;

            try {
                loop = readComplexType(reader, element, types);
            } catch (XMLStreamException xse) {
                throw schemaException("Exception reading loop", reader, xse);
            }

            String loopRefId = qnLoop.toString() + '.' + refId;
            types.put(loopRefId, loop);
            ref = new Reference(loopRefId, refTag, minOccurs, maxOccurs);
            ref.setReferencedType(loop);
        } else {
            ref = new Reference(refId, refTag, minOccurs, maxOccurs);
        }

        return ref;
    }

    void readSyntax(XMLStreamReader reader, List<EDISyntaxRule> rules) {
        String type = reader.getAttributeValue(null, "type");
        EDISyntaxRule.Type typeInt = null;

        try {
            typeInt = EDISyntaxRule.Type.valueOf(type.toUpperCase());
        } catch (NullPointerException e) {
            throw schemaException("Invalid syntax 'type': [null]", reader, e);
        } catch (IllegalArgumentException e) {
            throw schemaException("Invalid syntax 'type': [" + type + ']', reader, e);
        }

        try {
            rules.add(new SyntaxRestriction(typeInt, readSyntaxPositions(reader)));
        } catch (XMLStreamException xse) {
            throw schemaException("Exception reading syntax", reader, xse);
        }
    }

    List<Integer> readSyntaxPositions(XMLStreamReader reader) throws XMLStreamException {
        final List<Integer> positions = new ArrayList<>(5);

        while (reader.hasNext()) {
            int event = reader.next();

            switch (event) {
            case XMLStreamConstants.START_ELEMENT:
                QName element = reader.getName();

                if (qnPosition.equals(element)) {
                    String position = reader.getElementText();

                    try {
                        positions.add(Integer.parseInt(position));
                    } catch (@SuppressWarnings("unused") NumberFormatException e) {
                        throw schemaException("invalid position", reader);
                    }
                }

                break;
            case XMLStreamConstants.END_ELEMENT:
                if (qnSyntax.equals(reader.getName())) {
                    return positions;
                }
                break;
            default:
                checkEvent(reader);
                break;
            }
        }

        throw schemaException("missing end element " + qnSyntax, reader);
    }

    ElementType readSimpleType(XMLStreamReader reader) throws XMLStreamException {
        String name = parseAttribute(reader, "name", String::valueOf);
        String base = parseAttribute(reader, "base", String::valueOf, EDISimpleType.Base.STRING.toString());
        EDISimpleType.Base intBase = null;

        try {
            intBase = EDISimpleType.Base.valueOf(base.toUpperCase());
        } catch (Exception e) {
            throw schemaException("Invalid element 'type': [" + base + ']', reader, e);
        }

        int number = parseAttribute(reader, "number", Integer::parseInt, -1);
        long minLength = parseAttribute(reader, "minLength", Long::parseLong, 1L);
        long maxLength = parseAttribute(reader, "maxLength", Long::parseLong, 1L);

        final Set<String> values;

        if (intBase == EDISimpleType.Base.IDENTIFIER) {
            values = readEnumerationValues(reader);
        } else {
            values = Collections.emptySet();
        }

        return new ElementType(name, intBase, number, minLength, maxLength, values);
    }

    Set<String> readEnumerationValues(XMLStreamReader reader) throws XMLStreamException {
        Set<String> values = new HashSet<>();
        QName element;
        boolean enumerationEnd = false;

        while (reader.hasNext() && !enumerationEnd) {
            switch (reader.next()) {
            case XMLStreamConstants.START_ELEMENT:
                element = reader.getName();

                if (element.equals(qnValue)) {
                    values.add(reader.getElementText());
                } else if (!element.equals(qnEnumeration)) {
                    throw unexpectedElement(element, reader);
                }

                break;
            case XMLStreamConstants.END_ELEMENT:
                if (reader.getName().equals(qnEnumeration)) {
                    enumerationEnd = true;
                }
                break;
            default:
                break;
            }
        }

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

    void requireEvent(int eventId, XMLStreamReader reader) {
        Integer event = reader.getEventType();

        if (event != eventId) {
            throw unexpectedEvent(reader);
        }
    }

    void requireElementStart(QName element, XMLStreamReader reader) {
        requireEvent(XMLStreamConstants.START_ELEMENT, reader);

        if (!element.equals(reader.getName())) {
            throw schemaException("Unexpected XML element [" + reader.getName() + "]", reader);
        }
    }

    void checkEvent(XMLStreamReader reader, int... expected) {
        Integer event = reader.getEventType();

        if (Arrays.stream(expected).anyMatch(event::equals)) {
            return;
        }

        switch (event) {
        case XMLStreamConstants.CHARACTERS:
        case XMLStreamConstants.SPACE:
            String text = reader.getText().trim();

            if (text.length() > 0) {
                throw schemaException("Unexpected XML [" + text + "]", reader);
            }
            break;
        case XMLStreamConstants.COMMENT:
            // Ignore comments
            break;
        default:
            throw schemaException("Unexpected XML event [" + event + ']', reader);
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

            if (refType != refTypeId(impl.getRefTag())) {
                throw schemaException(String.format(REFERR_ILLEGAL, impl.getRefId(), impl.getRefTag(), struct.getId()));
            }

            switch (struct.getType()) {
            case INTERCHANGE:
                // Transactions may be located directly within the interchange in EDIFACT.
                setReference(struct,
                             (Reference) ref,
                             target,
                             EDIType.Type.GROUP,
                             EDIType.Type.TRANSACTION,
                             EDIType.Type.SEGMENT);
                break;
            case GROUP:
                setReference(struct, (Reference) ref, target, EDIType.Type.TRANSACTION, EDIType.Type.SEGMENT);
                break;
            case TRANSACTION:
                setReference(struct, (Reference) ref, target, EDIType.Type.LOOP, EDIType.Type.SEGMENT);
                break;
            case LOOP:
                setReference(struct, (Reference) ref, target, EDIType.Type.LOOP, EDIType.Type.SEGMENT);
                break;
            case SEGMENT:
                setReference(struct, (Reference) ref, target, EDIType.Type.COMPOSITE, EDIType.Type.ELEMENT);
                break;
            case COMPOSITE:
                setReference(struct, (Reference) ref, target, EDIType.Type.ELEMENT);
                break;
            default:
                break;
            }
        }
    }

    EDIType.Type refTypeId(String tag) {
        if (tag != null) {
            return EDIType.Type.valueOf(tag.toUpperCase());
        }
        throw new IllegalArgumentException("Unexpected element: " + tag);
    }

    void setReference(StructureType struct, Reference reference, EDIType target, EDIType.Type... allowedTargets) {
        boolean isAllowed = Arrays.stream(allowedTargets).anyMatch(target.getType()::equals);

        if (isAllowed) {
            reference.setReferencedType(target);
        } else {
            StringBuilder excp = new StringBuilder();
            excp.append("Structure ");
            excp.append(struct.getId());
            excp.append(" attempts to reference type with id = ");
            excp.append(reference.getRefId());
            excp.append(". Allowed types: " + Arrays.stream(allowedTargets)
                                                    .map(Object::toString)
                                                    .collect(Collectors.joining(", ")));
            throw schemaException(excp.toString());
        }
    }

    protected abstract String readReferencedId(XMLStreamReader reader);
}
