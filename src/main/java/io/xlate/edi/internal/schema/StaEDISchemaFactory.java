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
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.xml.namespace.QName;
import javax.xml.stream.Location;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import io.xlate.edi.schema.EDIReference;
import io.xlate.edi.schema.EDISchemaException;
import io.xlate.edi.schema.EDIType;
import io.xlate.edi.schema.Schema;
import io.xlate.edi.schema.SchemaFactory;

public class StaEDISchemaFactory implements SchemaFactory {

    static final XMLInputFactory FACTORY = XMLInputFactory.newInstance();

    static final String XMLNS_V2 = "http://xlate.io/EDISchema/v2";
    static final String XMLNS_V3 = "http://xlate.io/EDISchema/v3";
    static final String REFERR_UNDECLARED = "Type %s references undeclared %s with ref='%s'";
    static final String REFERR_ILLEGAL = "Type '%s' must not be referenced as '%s' in definition of type '%s'";

    private final Map<String, Object> properties;
    private final Set<String> supportedProperties;

    public StaEDISchemaFactory() {
        properties = new HashMap<>();
        supportedProperties = new HashSet<>();
    }

    @Override
    public Schema createSchema(InputStream stream) throws EDISchemaException {
        QName schemaElement;

        try {
            XMLStreamReader reader = FACTORY.createXMLStreamReader(stream);

            if (reader.getEventType() != XMLStreamConstants.START_DOCUMENT) {
                throw schemaException("Unexpected XML event [" + reader.getEventType() + ']', reader);
            }

            reader.nextTag();
            schemaElement = reader.getName();

            if (!"schema".equals(schemaElement.getLocalPart())) {
                throw unexpectedElement(schemaElement, reader);
            }

            SchemaReader schemaReader;

            if (XMLNS_V2.equals(schemaElement.getNamespaceURI())) {
                schemaReader = new SchemaReaderV2(reader);
            } else if (XMLNS_V3.equals(schemaElement.getNamespaceURI())) {
                schemaReader = new SchemaReaderV3(reader);
            } else {
                throw unexpectedElement(schemaElement, reader);
            }

            StaEDISchema schema = new StaEDISchema(schemaReader.getInterchangeName(),
                                                   schemaReader.getTransactionName());

            Map<String, EDIType> types = schemaReader.readTypes();
            validateReferences(types);
            schema.setTypes(types);

            return schema;
        } catch (XMLStreamException e) {
            throw new EDISchemaException(e);
        } catch (StaEDISchemaReadException e) {
            Location location = e.getLocation();
            if (location != null) {
                throw new EDISchemaException(e.getMessage(), location, e);
            }
            throw new EDISchemaException(e.getMessage(), e);
        }
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

    void validateReferences(Map<String, EDIType> types) {
        types.values()
             .stream()
             .filter(type -> !type.isType(EDIType.Type.ELEMENT))
             .map(type -> (StructureType) type)
             .forEach(struct -> validateReferences(struct, types));
    }

    void validateReferences(StructureType struct, Map<String, EDIType> types) {
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

    static StaEDISchemaReadException schemaException(String message) {
        return new StaEDISchemaReadException(message, null, null);
    }

    static StaEDISchemaReadException schemaException(String message, XMLStreamReader reader) {
        return schemaException(message, reader, null);
    }

    static StaEDISchemaReadException unexpectedElement(QName element, XMLStreamReader reader) {
        return schemaException("Unexpected XML element [" + element.toString() + ']', reader);
    }

    static StaEDISchemaReadException schemaException(String message, XMLStreamReader reader, Throwable cause) {
        return new StaEDISchemaReadException(message, reader.getLocation(), cause);
    }
}
