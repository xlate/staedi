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

    final QName QN_SCHEMA;
    final QName QN_DESCRIPTION;
    final QName QN_INTERCHANGE;
    final QName QN_GROUP;
    final QName QN_TRANSACTION;
    final QName QN_LOOP;
    final QName QN_SEGMENT;
    final QName QN_COMPOSITE;
    final QName QN_ELEMENT;
    final QName QN_SYNTAX;
    final QName QN_POSITION;
    final QName QN_SEQUENCE;
    final QName QN_ENUMERATION;
    final QName QN_VALUE;

    final QName QN_COMPOSITE_T;
    final QName QN_ELEMENT_T;
    final QName QN_SEGMENT_T;

    final Map<QName, EDIType.Type> complex;
    final Map<QName, EDIType.Type> typeDefinitions;
    final Set<QName> references;

    protected XMLStreamReader reader;

    public SchemaReaderBase(String xmlns, XMLStreamReader reader) {
        this.xmlns = xmlns;
        QN_SCHEMA = new QName(xmlns, "schema");
        QN_DESCRIPTION = new QName(xmlns, "description");
        QN_INTERCHANGE = new QName(xmlns, "interchange");
        QN_GROUP = new QName(xmlns, "group");
        QN_TRANSACTION = new QName(xmlns, "transaction");
        QN_LOOP = new QName(xmlns, "loop");
        QN_SEGMENT = new QName(xmlns, "segment");
        QN_COMPOSITE = new QName(xmlns, "composite");
        QN_ELEMENT = new QName(xmlns, "element");
        QN_SYNTAX = new QName(xmlns, "syntax");
        QN_POSITION = new QName(xmlns, "position");
        QN_SEQUENCE = new QName(xmlns, "sequence");
        QN_ENUMERATION = new QName(xmlns, "enumeration");
        QN_VALUE = new QName(xmlns, "value");

        QN_COMPOSITE_T = new QName(xmlns, "compositeType");
        QN_ELEMENT_T = new QName(xmlns, "elementType");
        QN_SEGMENT_T = new QName(xmlns, "segmentType");

        complex = new HashMap<>(4);
        complex.put(QN_INTERCHANGE, EDIType.Type.INTERCHANGE);
        complex.put(QN_GROUP, EDIType.Type.GROUP);
        complex.put(QN_TRANSACTION, EDIType.Type.TRANSACTION);
        complex.put(QN_LOOP, EDIType.Type.LOOP);
        complex.put(QN_SEGMENT_T, EDIType.Type.SEGMENT);
        complex.put(QN_COMPOSITE_T, EDIType.Type.COMPOSITE);

        typeDefinitions = new HashMap<>(3);
        typeDefinitions.put(QN_SEGMENT_T, EDIType.Type.SEGMENT);
        typeDefinitions.put(QN_COMPOSITE_T, EDIType.Type.COMPOSITE);
        typeDefinitions.put(QN_ELEMENT_T, EDIType.Type.ELEMENT);

        references = new HashSet<>(4);
        references.add(QN_SEGMENT);
        references.add(QN_COMPOSITE);
        references.add(QN_ELEMENT);

        this.reader = reader;
    }

    @Override
    public String getInterchangeName() {
        return QN_INTERCHANGE.toString();
    }

    @Override
    public String getTransactionName() {
        return QN_TRANSACTION.toString();
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

        if (!QN_INTERCHANGE.equals(element) && !QN_TRANSACTION.equals(element)) {
            throw unexpectedElement(element, reader);
        }

        return QN_INTERCHANGE.equals(element);
    }

    String readDescription(XMLStreamReader reader) throws XMLStreamException {
        QName element;

        reader.nextTag();
        element = reader.getName();
        String description = null;

        if (QN_DESCRIPTION.equals(element)) {
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

        if (!QN_SEQUENCE.equals(element)) {
            throw unexpectedElement(element, reader);
        }

        reader.nextTag();
        element = reader.getName();

        List<EDIReference> refs = new ArrayList<>(3);
        refs.add(headerRef);

        while (QN_SEGMENT.equals(element)) {
            refs.add(readReference(reader, types));
            reader.nextTag(); // Advance to end element
            reader.nextTag(); // Advance to next start element
            element = reader.getName();
        }

        if (QN_GROUP.equals(element)) {
            refs.add(readControlStructure(reader, element, QN_TRANSACTION, types));
            reader.nextTag(); // Advance to end element
            reader.nextTag(); // Advance to next start element
            element = reader.getName();
        }

        if (QN_TRANSACTION.equals(element)) {
            refs.add(readControlStructure(reader, element, null, types));
            reader.nextTag(); // Advance to end element
            reader.nextTag(); // Advance to next start element
        }

        refs.add(trailerRef);

        StructureType interchange = new StructureType(QN_INTERCHANGE.toString(),
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
        types.put(QN_TRANSACTION.toString(), readComplexType(reader, element, types));
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
                if (reader.getName().equals(QN_SCHEMA)) {
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
        } else if (QN_ELEMENT_T.equals(element)) {
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
        final String name;
        String code = reader.getAttributeValue(null, "code");

        if (QN_TRANSACTION.equals(complexType)) {
            name = QN_TRANSACTION.toString();
        } else if (QN_LOOP.equals(complexType)) {
            name = code;
        } else {
            name = reader.getAttributeValue(null, "name");

            if (type == EDIType.Type.SEGMENT && !name.matches("^[A-Z][A-Z0-9]{1,2}$")) {
                throw schemaException("Invalid segment name [" + name + ']', reader);
            }
        }

        if (code == null) {
            code = name;
        }

        final List<EDIReference> refs = new ArrayList<>(8);
        final List<EDISyntaxRule> rules = new ArrayList<>(2);
        boolean sequence = false;
        boolean endOfType = false;

        while (!endOfType && reader.hasNext()) {
            switch (reader.next()) {
            case XMLStreamConstants.START_ELEMENT:
                QName element = reader.getName();

                if (element.equals(QN_SEQUENCE)) {
                    if (sequence) {
                        throw schemaException("multiple sequence elements", reader);
                    }
                    sequence = true;
                    readReferences(reader, types, refs);
                } else if (element.equals(QN_SYNTAX)) {
                    switch (type) {
                    case SEGMENT:
                    case COMPOSITE:
                        readSyntax(reader, rules);
                        break;
                    default:
                        break;
                    }
                } else {
                    throw schemaException("Unexpected element " + element, reader);
                }

                break;

            case XMLStreamConstants.END_ELEMENT:
                if (reader.getName().equals(complexType)) {
                    endOfType = true;
                }
                break;

            default:
                checkEvent(reader);
                break;
            }
        }

        return new StructureType(name, type, code, refs, rules);
    }

    void readReferences(XMLStreamReader reader,
                       Map<String, EDIType> types,
                       List<EDIReference> refs) throws XMLStreamException {
        while (reader.hasNext()) {
            switch (reader.next()) {
            case XMLStreamConstants.START_ELEMENT:
                refs.add(readReference(reader, types));
                break;

            case XMLStreamConstants.END_ELEMENT:
                if (reader.getName().equals(QN_SEQUENCE)) {
                    return;
                }

                break;
            default:
                checkEvent(reader);
                break;
            }
        }
    }

    Reference readReference(XMLStreamReader reader, Map<String, EDIType> types) throws XMLStreamException {
        QName element = reader.getName();
        String refId = null;

        if (references.contains(element)) {
            refId = readReferencedId(reader);
            Objects.requireNonNull(refId);
        } else if (QN_LOOP.equals(element)) {
            refId = reader.getAttributeValue(null, "code");
            //TODO: ensure not null
        } else {
            throw unexpectedElement(element, reader);
        }

        String refTag = element.getLocalPart();
        int minOccurs = parseAttribute(reader, "minOccurs", Integer::parseInt, 0);
        int maxOccurs = parseAttribute(reader, "maxOccurs", Integer::parseInt, 1);

        Reference ref;

        if (QN_LOOP.equals(element)) {
            StructureType loop = readComplexType(reader, element, types);
            String loopRefId = QN_LOOP.toString() + '.' + refId;
            types.put(loopRefId, loop);
            ref = new Reference(loopRefId, refTag, minOccurs, maxOccurs);
            ref.setReferencedType(loop);
        } else {
            ref = new Reference(refId, refTag, minOccurs, maxOccurs);
        }

        return ref;
    }

    void readSyntax(XMLStreamReader reader, List<EDISyntaxRule> rules) throws XMLStreamException {
        String type = reader.getAttributeValue(null, "type");
        EDISyntaxRule.Type typeInt = null;

        try {
            typeInt = EDISyntaxRule.Type.valueOf(type.toUpperCase());
        } catch (NullPointerException e) {
            throw schemaException("Invalid syntax 'type': [null]", reader, e);
        } catch (IllegalArgumentException e) {
            throw schemaException("Invalid syntax 'type': [" + type + ']', reader, e);
        }

        SyntaxRestriction rule = new SyntaxRestriction(typeInt, readSyntaxPositions(reader));

        rules.add(rule);
    }

    List<Integer> readSyntaxPositions(XMLStreamReader reader) throws XMLStreamException {
        final List<Integer> positions = new ArrayList<>(5);

        while (reader.hasNext()) {
            int event = reader.next();

            switch (event) {
            case XMLStreamConstants.START_ELEMENT:
                QName element = reader.getName();

                if (QN_POSITION.equals(element)) {
                    String position = reader.getElementText();

                    try {
                        positions.add(Integer.parseInt(position));
                    } catch (@SuppressWarnings("unused") NumberFormatException e) {
                        throw schemaException("invalid position", reader);
                    }
                }

                break;
            case XMLStreamConstants.END_ELEMENT:
                if (QN_SYNTAX.equals(reader.getName())) {
                    return positions;
                }
                break;
            default:
                checkEvent(reader);
                break;
            }
        }

        throw schemaException("missing end element " + QN_SYNTAX, reader);
    }

    ElementType readSimpleType(XMLStreamReader reader) throws XMLStreamException {
        String name = reader.getAttributeValue(null, "name");

        String base = reader.getAttributeValue(null, "base");
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

                if (element.equals(QN_VALUE)) {
                    values.add(reader.getElementText());
                } else if (!element.equals(QN_ENUMERATION)) {
                    throw unexpectedElement(element, reader);
                }

                break;
            case XMLStreamConstants.END_ELEMENT:
                if (reader.getName().equals(QN_ENUMERATION)) {
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
