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
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.namespace.QName;
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

public class StaEDISchemaFactory extends SchemaFactory {

    private static XMLInputFactory factory = XMLInputFactory.newFactory();

    private static final String RESERVED = "io.xlate.";
    private static final String XMLNS = "http://xlate.io/2015/EDISchema";

    private static final QName QN_SCHEMA = new QName(XMLNS, "schema");

    private static final QName QN_MAIN_LOOP = new QName(XMLNS, "mainLoop");
    private static final QName QN_COMPOSITE_T = new QName(XMLNS, "compositeType");
    private static final QName QN_ELEMENT_T = new QName(XMLNS, "elementType");
    private static final QName QN_LOOP_T = new QName(XMLNS, "loopType");
    private static final QName QN_SEGMENT_T = new QName(XMLNS, "segmentType");

    private static final QName QN_SEQUENCE = new QName(XMLNS, "sequence");

    private static final QName QN_COMPOSITE = new QName(XMLNS, "composite");
    private static final QName QN_ELEMENT = new QName(XMLNS, "element");
    private static final QName QN_LOOP = new QName(XMLNS, "loop");
    private static final QName QN_SEGMENT = new QName(XMLNS, "segment");

    private static final QName QN_SYNTAX = new QName(XMLNS, "syntax");
    private static final QName QN_POSITION = new QName(XMLNS, "position");

    private static final QName QN_ENUMERATION = new QName(XMLNS, "enumeration");
    private static final QName QN_VALUE = new QName(XMLNS, "value");

    private static final String ID_INTERCHANGE = RESERVED + "edi.schema.INTERCHANGE";
    private static final String ID_GROUP = RESERVED + "edi.schema.GROUP";
    private static final String ID_TRANSACTION = RESERVED + "edi.schema.TRANSACTION";

    private static final Map<QName, EDIType.Type> complex;
    private static final Set<QName> references;

    private static final Set<String> specialIdentifiers;

    static {
        complex = new HashMap<>(4);
        complex.put(QN_COMPOSITE_T, EDIType.Type.COMPOSITE);
        complex.put(QN_LOOP_T, EDIType.Type.LOOP);
        complex.put(QN_MAIN_LOOP, EDIType.Type.LOOP);
        complex.put(QN_SEGMENT_T, EDIType.Type.SEGMENT);

        references = new HashSet<>(4);
        references.add(QN_COMPOSITE);
        references.add(QN_ELEMENT);
        references.add(QN_LOOP);
        references.add(QN_SEGMENT);

        specialIdentifiers = new HashSet<>(3);
        specialIdentifiers.add(ID_INTERCHANGE);
        specialIdentifiers.add(ID_GROUP);
        specialIdentifiers.add(ID_TRANSACTION);
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
            throw new EDISchemaException(e.getMessage(), e.getLocation(), e);
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
        boolean schemaEnd = false;

        try {
            XMLStreamReader reader = factory.createXMLStreamReader(stream);

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
        } catch (XMLStreamException e) {
            throw new EDISchemaException(e);
        }

        return types;
    }

    void startElement(Map<String, EDIType> types, XMLStreamReader reader) throws EDISchemaException, XMLStreamException {
        QName element = reader.getName();

        if (element.equals(QN_SCHEMA)) {
            return;
        }

        if (element.equals(QN_MAIN_LOOP)) {
            if (types.containsKey(StaEDISchema.MAIN)) {
                schemaException("Multiple mainLoop elements", reader);
            }

            types.put(StaEDISchema.MAIN, buildComplexType(reader, element));
        } else {
            String name = reader.getAttributeValue(null, "name");

            if (complex.containsKey(element)) {
                nameCheck(name, types, reader);
                types.put(name, buildComplexType(reader, element));
            } else if (QN_ELEMENT_T.equals(element)) {
                nameCheck(name, types, reader);
                types.put(name, buildSimpleType(reader));
            } else {
                schemaException("unknown element " + element, reader);
            }
        }
    }

    void nameCheck(String name, Map<String, EDIType> types, XMLStreamReader reader) throws EDISchemaException {
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
                StringBuilder excp = new StringBuilder();
                if (StaEDISchema.MAIN.equals(struct.getId())) {
                    excp.append(QN_MAIN_LOOP.getLocalPart());
                } else {
                    excp.append("Type ");
                    excp.append(struct.getId());
                }

                excp.append(" references undeclared ");
                excp.append(impl.getRefTag());
                excp.append(" with ref='");
                excp.append(impl.getRefId());
                excp.append('\'');
                schemaException(excp.toString());
            }

            final EDIType.Type refType = target.getType();

            if (refType != refTypeId(impl.getRefTag())) {
                StringBuilder excp = new StringBuilder();
                excp.append("Type '");
                excp.append(impl.getRefId());
                excp.append("' must not be referenced as \'");
                excp.append(impl.getRefTag());
                excp.append("\' in definition of type '");
                excp.append(struct.getId());
                excp.append('\'');
                schemaException(excp.toString());
            }

            switch (struct.getType()) {
            case LOOP: {
                if (refType != EDIType.Type.SEGMENT
                        && refType != EDIType.Type.LOOP) {
                    StringBuilder excp = new StringBuilder();
                    if (StaEDISchema.MAIN.equals(struct.getId())) {
                        excp.append(QN_MAIN_LOOP.getLocalPart());
                    } else {
                        excp.append("Loop ");
                        excp.append(struct.getId());
                    }
                    excp.append(" attempts to reference type with id = ");
                    excp.append(impl.getRefId());
                    excp.append(". Loops may only reference loop or segment types");
                    schemaException(excp.toString());
                }

                /*if (refType == EDIType.TYPE_LOOP) {
                    if (ref.getMinOccurs() > 0) {
                        StringBuilder excp = new StringBuilder();
                        excp.append("Reference to loop ");
                        excp.append(impl.getRefId());
                        excp.append(" must not specify minOccurs");
                        throw new EDISchemaException(excp.toString());
                    }
                }*/

                ((Reference) ref).setReferencedType(target);
                break;
            }
            case SEGMENT: {
                if (refType != EDIType.Type.ELEMENT && refType != EDIType.Type.COMPOSITE) {
                    StringBuilder excp = new StringBuilder();
                    excp.append("Segment ");
                    excp.append(struct.getId());
                    excp.append(" attempts to reference type with id = ");
                    excp.append(impl.getRefId());
                    excp.append(". Segments may only reference element or composite types");
                    schemaException(excp.toString());
                }

                ((Reference) ref).setReferencedType(target);
                break;
            }
            case COMPOSITE: {
                if (refType != EDIType.Type.ELEMENT) {
                    StringBuilder excp = new StringBuilder();
                    excp.append("Composite ");
                    excp.append(struct.getId());
                    excp.append(" attempts to reference type with id = ");
                    excp.append(impl.getRefId());
                    excp.append(". Composite may only reference element types");
                    schemaException(excp.toString());
                }

                ((Reference) ref).setReferencedType(target);
                break;
            }

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

    Structure buildComplexType(XMLStreamReader reader, QName complexType) throws EDISchemaException,
                                                                          XMLStreamException {
        final EDIType.Type type = complex.get(complexType);
        final String name;

        if (complexType.equals(QN_MAIN_LOOP)) {
            name = StaEDISchema.MAIN;
        } else {
            name = reader.getAttributeValue(null, "name");

            if (type == EDIType.Type.SEGMENT && !name.matches("^[A-Z][A-Z0-9]{1,2}$")) {
                schemaException("Invalid segment name [" + name + ']', reader);
            }
        }

        String code = reader.getAttributeValue(null, "code");

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
                    addReferences(reader, refs);
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

                schemaException("unexpected element " + element, reader);
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

    void addReferences(XMLStreamReader reader, List<EDIReference> refs) throws EDISchemaException,
                                                                        XMLStreamException {

        while (reader.hasNext()) {
            switch (reader.next()) {
            case XMLStreamConstants.START_ELEMENT:
                QName element = reader.getName();

                if (!references.contains(element)) {
                    schemaException("unexpected element " + element, reader);
                }

                String refTag = element.getLocalPart();

                String refId = reader.getAttributeValue(null, "ref");
                String min = reader.getAttributeValue(null, "minOccurs");
                String max = reader.getAttributeValue(null, "maxOccurs");

                int minValue = -1;

                try {
                    minValue = min != null ? Integer.parseInt(min) : 0;
                } catch (@SuppressWarnings("unused") NumberFormatException e) {
                    schemaException("invalid minOccurs", reader);
                }

                int maxValue = -1;

                try {
                    maxValue = max != null ? Integer.parseInt(max) : 1;
                } catch (@SuppressWarnings("unused") NumberFormatException e) {
                    schemaException("invalid maxOccurs", reader);
                }

                Reference ref = new Reference(refId, refTag, minValue, maxValue);

                refs.add(ref);

                break;

            case XMLStreamConstants.END_ELEMENT:
                if (reader.getName().equals(QN_SEQUENCE)) {
                    return;
                }
                if (reader.getName().equals(QN_MAIN_LOOP)) {
                    return;
                }
                break;
            default:
                checkEvent(reader);
                break;
            }
        }
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
        String nbr = reader.getAttributeValue(null, "number");
        int number = -1;

        try {
            number = nbr != null ? Integer.parseInt(nbr) : -1;
        } catch (NumberFormatException e) {
            schemaException("invalid number", reader, e);
        }

        String base = reader.getAttributeValue(null, "base");
        EDISimpleType.Base intBase = null;

        try {
            intBase = EDISimpleType.Base.valueOf(base.toUpperCase());
        } catch (Exception e) {
            schemaException("Invalid element 'type': [" + base + ']', reader, e);
        }

        String min = reader.getAttributeValue(null, "minLength");
        int minLength = -1;

        try {
            minLength = min != null ? Integer.parseInt(min) : 1;
        } catch (NumberFormatException e) {
            schemaException("invalid minLength", reader, e);
        }

        String max = reader.getAttributeValue(null, "maxLength");
        int maxLength = -1;

        try {
            maxLength = max != null ? Integer.parseInt(max) : 1;
        } catch (NumberFormatException e) {
            schemaException("invalid maxLength", reader, e);
        }

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

    void checkEvent(XMLStreamReader reader) {
        int event = reader.getEventType();

        switch (event) {
        case XMLStreamConstants.CHARACTERS:
        case XMLStreamConstants.SPACE:
            String text = reader.getText().trim();

            if (text.length() > 0) {
                schemaException("unexpected XML [" + text + "]", reader);
            }
            break;
        case XMLStreamConstants.COMMENT:
            // Ignore comments
            break;
        default:
            schemaException("unexpected XML event: " + event, reader);
        }
    }
}
