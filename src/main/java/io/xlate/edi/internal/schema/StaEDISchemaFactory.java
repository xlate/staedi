/*******************************************************************************
 * Copyright 2017 xlate.io LLC, http://www.xlate.io
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 ******************************************************************************/
package io.xlate.edi.internal.schema;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.xml.namespace.QName;
import javax.xml.stream.Location;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import io.xlate.edi.schema.EDIReference;
import io.xlate.edi.schema.EDISchemaException;
import io.xlate.edi.schema.EDISimpleType;
import io.xlate.edi.schema.EDISyntaxRule;
import io.xlate.edi.schema.EDIType;
import io.xlate.edi.schema.Schema;
import io.xlate.edi.schema.SchemaFactory;

public class StaEDISchemaFactory implements SchemaFactory {

    private static XMLInputFactory factory = XMLInputFactory.newInstance();

    private static final String XMLNS = "http://xlate.io/EDISchema/v2";
    private static final String REFERR_UNDECLARED = "Type %s references undeclared %s with ref='%s'";
    private static final String REFERR_ILLEGAL = "Type '%s' must not be referenced as '%s' in definition of type '%s'";

    static final QName QN_SCHEMA = new QName(XMLNS, "schema");
    static final QName QN_DESCRIPTION = new QName(XMLNS, "description");
    static final QName QN_INTERCHANGE = new QName(XMLNS, "interchange");
    static final QName QN_GROUP = new QName(XMLNS, "group");
    static final QName QN_TRANSACTION = new QName(XMLNS, "transaction");
    static final QName QN_LOOP = new QName(XMLNS, "loop");
    static final QName QN_SEGMENT = new QName(XMLNS, "segment");
    static final QName QN_COMPOSITE = new QName(XMLNS, "composite");
    static final QName QN_ELEMENT = new QName(XMLNS, "element");
    static final QName QN_SYNTAX = new QName(XMLNS, "syntax");
    static final QName QN_POSITION = new QName(XMLNS, "position");
    static final QName QN_SEQUENCE = new QName(XMLNS, "sequence");
    static final QName QN_ENUMERATION = new QName(XMLNS, "enumeration");
    static final QName QN_VALUE = new QName(XMLNS, "value");

    private static final QName QN_COMPOSITE_T = new QName(XMLNS, "compositeType");
    private static final QName QN_ELEMENT_T = new QName(XMLNS, "elementType");
    private static final QName QN_SEGMENT_T = new QName(XMLNS, "segmentType");

    private static final Map<QName, EDIType.Type> complex;
    private static final Set<QName> references;

    static {
        complex = new HashMap<>(4);
        complex.put(QN_INTERCHANGE, EDIType.Type.INTERCHANGE);
        complex.put(QN_GROUP, EDIType.Type.GROUP);
        complex.put(QN_TRANSACTION, EDIType.Type.TRANSACTION);
        complex.put(QN_LOOP, EDIType.Type.LOOP);
        complex.put(QN_SEGMENT_T, EDIType.Type.SEGMENT);
        complex.put(QN_COMPOSITE_T, EDIType.Type.COMPOSITE);

        references = new HashSet<>(4);
        references.add(QN_SEGMENT);
        references.add(QN_COMPOSITE);
        references.add(QN_ELEMENT);
    }

    private final Map<String, Object> properties;
    private final Set<String> supportedProperties;

    public StaEDISchemaFactory() {
        properties = new HashMap<>();
        supportedProperties = new HashSet<>();
    }

    @Override
    public Schema createSchema(InputStream stream) throws EDISchemaException {
        StaEDISchema schema = new StaEDISchema();

        try {
            Map<String, EDIType> types = loadTypes(stream);
            validateReferences(types);
            schema.setTypes(types);
        } catch (StaEDISchemaReadException e) {
            Location location = e.getLocation();
            if (location != null) {
                throw new EDISchemaException(e.getMessage(), location, e);
            }
            throw new EDISchemaException(e.getMessage(), e);
        }

        return schema;
    }

    @Override
    public Schema createSchema(URL location) throws EDISchemaException {
        try {
            return createSchema(location.openStream());
        } catch (IOException e) {
            throw new EDISchemaException("Unable to open stream", e);
        }
    }

    @Override
    public boolean isPropertySupported(String name) {
        return supportedProperties.contains(name);
    }

    @Override
    public Object getProperty(String name) {
        if (isPropertySupported(name)) {
            return properties.get(name);
        }
        throw new IllegalArgumentException("Unsupported property: " + name);
    }

    @Override
    public void setProperty(String name, Object value) {
        if (isPropertySupported(name)) {
            properties.put(name, value);
        }
        throw new IllegalArgumentException("Unsupported property: " + name);
    }

    static void unexpectedElement(QName element, XMLStreamReader reader) {
        schemaException("Unexpected XML element [" + element.toString() + ']', reader);
    }

    static void schemaException(String message, XMLStreamReader reader, Throwable cause) {
        throw new StaEDISchemaReadException(message, reader.getLocation(), cause);
    }

    static void schemaException(String message, XMLStreamReader reader) {
        schemaException(message, reader, null);
    }

    static void schemaException(String message) {
        throw new StaEDISchemaReadException(message, null, null);
    }

    Map<String, EDIType> loadTypes(InputStream stream) throws EDISchemaException {
        Map<String, EDIType> types = new HashMap<>(100);

        try {
            XMLStreamReader reader = factory.createXMLStreamReader(stream);

            if (isInterchangeSchema(reader)) {
                readInterchange(reader, types);
            } else {
                readTransaction(reader, types);
            }

            readTypeDefinitions(reader, types);
            reader.next();
            requireEvent(XMLStreamConstants.END_DOCUMENT, reader);
        } catch (XMLStreamException e) {
            throw new EDISchemaException(e);
        }

        return types;
    }

    boolean isInterchangeSchema(XMLStreamReader reader) throws XMLStreamException {
        QName element;

        requireEvent(XMLStreamConstants.START_DOCUMENT, reader);
        reader.nextTag();
        element = reader.getName();

        if (!QN_SCHEMA.equals(element)) {
            unexpectedElement(element, reader);
        }

        reader.nextTag();
        element = reader.getName();

        if (!QN_INTERCHANGE.equals(element) && !QN_TRANSACTION.equals(element)) {
            unexpectedElement(element, reader);
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
            unexpectedElement(element, reader);
        }

        reader.nextTag();
        element = reader.getName();

        List<EDIReference> refs = new ArrayList<>(3);
        refs.add(headerRef);

        while (QN_SEGMENT.equals(element)) {
            refs.add(createReference(reader, types));
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

        Structure interchange = new Structure(QN_INTERCHANGE.toString(),
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
                schemaException("Invalid value for 'use': " + use, reader);
            }
        }

        Reference headerRef = createControlReference(reader, "header");
        Reference trailerRef = createControlReference(reader, "trailer");

        readDescription(reader);
        QName nextElement = reader.getName();

        if (subelement != null && !subelement.equals(nextElement)) {
            unexpectedElement(nextElement, reader);
        }

        List<EDIReference> refs = new ArrayList<>(3);
        refs.add(headerRef);
        if (subelement != null) {
            refs.add(readControlStructure(reader, subelement, null, types));
        }
        refs.add(trailerRef);

        Structure struct = new Structure(element.toString(),
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
            schemaException("Missing required attribute [" + attributeName + ']', reader);
        }

        return new Reference(refId, "segment", 1, 1);
    }

    void readTransaction(XMLStreamReader reader, Map<String, EDIType> types) throws XMLStreamException {
        QName element = reader.getName();
        types.put(QN_TRANSACTION.toString(), buildComplexType(reader, element, types));
    }

    void readTypeDefinitions(XMLStreamReader reader, Map<String, EDIType> types) throws XMLStreamException {
        boolean schemaEnd = false;

        while (reader.hasNext() && !schemaEnd) {
            switch (reader.next()) {
            case XMLStreamConstants.START_ELEMENT:
                startElement(types, reader);
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

    void startElement(Map<String, EDIType> types, XMLStreamReader reader) throws XMLStreamException {
        QName element = reader.getName();
        String name = reader.getAttributeValue(null, "name");

        if (complex.containsKey(element)) {
            nameCheck(name, types, reader);
            types.put(name, buildComplexType(reader, element, types));
        } else if (QN_ELEMENT_T.equals(element)) {
            nameCheck(name, types, reader);
            types.put(name, buildSimpleType(reader));
        } else {
            schemaException("Unexpected element: " + element, reader);
        }
    }

    void nameCheck(String name, Map<String, EDIType> types, XMLStreamReader reader) {
        if (name == null) {
            schemaException("missing type name", reader);
        }

        if (types.containsKey(name)) {
            schemaException("duplicate name: " + name, reader);
        }
    }

    void validateReferences(Map<String, EDIType> types) {
        types.values()
             .stream()
             .filter(type -> !type.isType(EDIType.Type.ELEMENT))
             .map(type -> (Structure) type)
             .forEach(struct -> validateReferences(struct, types));
    }

    void validateReferences(Structure struct, Map<String, EDIType> types) {
        for (EDIReference ref : struct.getReferences()) {
            Reference impl = (Reference) ref;
            EDIType target = types.get(impl.getRefId());

            if (target == null) {
                schemaException(String.format(REFERR_UNDECLARED, struct.getId(), impl.getRefTag(), impl.getRefId()));
            }

            final EDIType.Type refType = target.getType();

            if (refType != refTypeId(impl.getRefTag())) {
                schemaException(String.format(REFERR_ILLEGAL, impl.getRefId(), impl.getRefTag(), struct.getId()));
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

    void setReference(Structure struct, Reference reference, EDIType target, EDIType.Type... allowedTargets) {
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
            schemaException(excp.toString());
        }
    }

    Structure buildComplexType(XMLStreamReader reader,
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
                schemaException("Invalid segment name [" + name + ']', reader);
            }
        }

        if (code == null) {
            code = name;
        }

        final List<EDIReference> refs = new ArrayList<>(8);
        final List<EDISyntaxRule> rules = new ArrayList<>(2);
        boolean sequence = false;

        scan: while (reader.hasNext()) {
            switch (reader.next()) {
            case XMLStreamConstants.START_ELEMENT:
                QName element = reader.getName();

                if (element.equals(QN_SEQUENCE)) {
                    if (sequence) {
                        schemaException("multiple sequence elements", reader);
                    }
                    sequence = true;
                    addReferences(reader, types, refs);
                    continue;
                }

                if (element.equals(QN_SYNTAX)) {
                    switch (type) {
                    case SEGMENT:
                    case COMPOSITE:
                        addSyntax(reader, rules);
                        continue;
                    default:
                        break;
                    }
                }

                schemaException("Unexpected element " + element, reader);
                break;

            case XMLStreamConstants.END_ELEMENT:
                if (reader.getName().equals(complexType)) {
                    break scan;
                }
                break;

            default:
                checkEvent(reader);
                break;
            }
        }

        return new Structure(name, type, code, refs, rules);
    }

    void addReferences(XMLStreamReader reader,
                       Map<String, EDIType> types,
                       List<EDIReference> refs) throws XMLStreamException {
        while (reader.hasNext()) {
            switch (reader.next()) {
            case XMLStreamConstants.START_ELEMENT:
                refs.add(createReference(reader, types));
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

    Reference createReference(XMLStreamReader reader, Map<String, EDIType> types) throws XMLStreamException {
        QName element = reader.getName();
        String refId = null;

        if (references.contains(element)) {
            refId = reader.getAttributeValue(null, "ref");
        } else if (QN_LOOP.equals(element)) {
            refId = reader.getAttributeValue(null, "code");
            //TODO: ensure not null
        } else {
            unexpectedElement(element, reader);
        }

        String refTag = element.getLocalPart();
        int minOccurs = parseAttribute(reader, "minOccurs", Integer::parseInt, 0);
        int maxOccurs = parseAttribute(reader, "maxOccurs", Integer::parseInt, 1);

        Reference ref;

        if (QN_LOOP.equals(element)) {
            Structure loop = buildComplexType(reader, element, types);
            String loopRefId = QN_LOOP.toString() + '.' + refId;
            types.put(loopRefId, loop);
            ref = new Reference(loopRefId, refTag, minOccurs, maxOccurs);
            ref.setReferencedType(loop);
        } else {
            ref = new Reference(refId, refTag, minOccurs, maxOccurs);
        }

        return ref;
    }

    void addSyntax(XMLStreamReader reader, List<EDISyntaxRule> rules) throws XMLStreamException {
        String type = reader.getAttributeValue(null, "type");
        EDISyntaxRule.Type typeInt = null;

        try {
            typeInt = EDISyntaxRule.Type.valueOf(type.toUpperCase());
        } catch (NullPointerException e) {
            schemaException("Invalid syntax 'type': [null]", reader, e);
        } catch (IllegalArgumentException e) {
            schemaException("Invalid syntax 'type': [" + type + ']', reader, e);
        }

        SyntaxRestriction rule = new SyntaxRestriction(typeInt, buildSyntaxPositions(reader));

        rules.add(rule);
    }

    List<Integer> buildSyntaxPositions(XMLStreamReader reader) throws XMLStreamException {
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
                        schemaException("invalid position", reader);
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

        schemaException("missing end element " + QN_SYNTAX, reader);
        // For compilation only
        return Collections.emptyList();
    }

    Element buildSimpleType(XMLStreamReader reader) throws XMLStreamException {
        String name = reader.getAttributeValue(null, "name");

        String base = reader.getAttributeValue(null, "base");
        EDISimpleType.Base intBase = null;

        try {
            intBase = EDISimpleType.Base.valueOf(base.toUpperCase());
        } catch (Exception e) {
            schemaException("Invalid element 'type': [" + base + ']', reader, e);
        }

        int number = parseAttribute(reader, "number", Integer::parseInt, -1);
        long minLength = parseAttribute(reader, "minLength", Long::parseLong, 1L);
        long maxLength = parseAttribute(reader, "maxLength", Long::parseLong, 1L);

        final Set<String> values;

        if (intBase == EDISimpleType.Base.IDENTIFIER) {
            values = buildEnumerationValues(reader);
        } else {
            values = Collections.emptySet();
        }

        return new Element(name, intBase, number, minLength, maxLength, values);
    }

    Set<String> buildEnumerationValues(XMLStreamReader reader) throws XMLStreamException {
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
                    schemaException("unexpected element " + element, reader);
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
            schemaException("Invalid " + attrName, reader, e);
        }

        // Impossible
        return null;
    }

    void requireEvent(int eventId, XMLStreamReader reader) {
        Integer event = reader.getEventType();

        if (event != eventId) {
            schemaException("Unexpected XML event [" + event + ']', reader);
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
                schemaException("Unexpected XML [" + text + "]", reader);
            }
            break;
        case XMLStreamConstants.COMMENT:
            // Ignore comments
            break;
        default:
            schemaException("Unexpected XML event [" + event + ']', reader);
        }
    }
}
