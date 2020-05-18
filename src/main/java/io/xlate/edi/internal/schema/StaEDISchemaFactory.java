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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.namespace.QName;
import javax.xml.stream.Location;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import io.xlate.edi.schema.EDISchemaException;
import io.xlate.edi.schema.EDIType;
import io.xlate.edi.schema.Schema;
import io.xlate.edi.schema.SchemaFactory;

public class StaEDISchemaFactory implements SchemaFactory {

    static final Logger LOGGER = Logger.getLogger(StaEDISchemaFactory.class.getName());
    static final XMLInputFactory FACTORY = XMLInputFactory.newInstance();

    static final String XMLNS_V2 = "http://xlate.io/EDISchema/v2";
    static final String XMLNS_V3 = "http://xlate.io/EDISchema/v3";

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
            LOGGER.fine(() -> "Creating schema from stream");
            XMLStreamReader reader = FACTORY.createXMLStreamReader(stream);

            if (reader.getEventType() != XMLStreamConstants.START_DOCUMENT) {
                throw unexpectedEvent(reader);
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

            Map<String, EDIType> types = schemaReader.readTypes();

            StaEDISchema schema = new StaEDISchema(schemaReader.getInterchangeName(),
                                                   schemaReader.getTransactionName(),
                                                   schemaReader.getImplementationName());

            schema.setTypes(types);

            LOGGER.log(Level.FINE, "Schema created, contains {0} types", types.size());

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
        try (InputStream stream = location.openStream()) {
            return createSchema(stream);
        } catch (IOException e) {
            throw new EDISchemaException("Unable to read URL stream", e);
        }
    }

    @Override
    public Schema getControlSchema(String standard, String[] version) throws EDISchemaException {
        return SchemaUtils.getControlSchema(standard, version);
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

    static StaEDISchemaReadException schemaException(String message) {
        return new StaEDISchemaReadException(message, null, null);
    }

    static StaEDISchemaReadException schemaException(String message, XMLStreamReader reader) {
        return schemaException(message, reader, null);
    }

    static StaEDISchemaReadException unexpectedElement(QName element, XMLStreamReader reader) {
        return schemaException("Unexpected XML element [" + element.toString() + ']', reader);
    }

    static StaEDISchemaReadException unexpectedEvent(XMLStreamReader reader) {
        return schemaException("Unexpected XML event [" + reader.getEventType() + ']', reader);
    }

    static StaEDISchemaReadException schemaException(String message, XMLStreamReader reader, Throwable cause) {
        return new StaEDISchemaReadException(message, reader.getLocation(), cause);
    }
}
