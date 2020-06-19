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
import java.util.function.BiFunction;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.namespace.QName;
import javax.xml.stream.Location;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import io.xlate.edi.schema.EDISchemaException;
import io.xlate.edi.schema.EDIType;
import io.xlate.edi.schema.Schema;
import io.xlate.edi.schema.SchemaFactory;

public class StaEDISchemaFactory implements SchemaFactory {

    static final Logger LOGGER = Logger.getLogger(StaEDISchemaFactory.class.getName());
    static final XMLInputFactory FACTORY = XMLInputFactory.newInstance();
    static final String SCHEMA_TAG = "schema";

    static final String XMLNS_V2 = "http://xlate.io/EDISchema/v2";
    static final String XMLNS_V3 = "http://xlate.io/EDISchema/v3";
    static final String XMLNS_V4 = "http://xlate.io/EDISchema/v4";

    static final Map<QName, BiFunction<XMLStreamReader, Map<String, Object>, SchemaReader>> readerFactories = new HashMap<>(3);
    static final Set<String> supportedProperties = new HashSet<>();

    static {
        readerFactories.put(new QName(XMLNS_V2, SCHEMA_TAG), SchemaReaderV2::new);
        readerFactories.put(new QName(XMLNS_V3, SCHEMA_TAG), SchemaReaderV3::new);
        readerFactories.put(new QName(XMLNS_V4, SCHEMA_TAG), SchemaReaderV4::new);

        supportedProperties.add(SCHEMA_LOCATION_URL_CONTEXT);
    }

    private final Map<String, Object> properties = new HashMap<>();

    @Override
    public Schema createSchema(InputStream stream) throws EDISchemaException {
        Map<String, EDIType> types = readSchemaTypes(stream, properties);

        StaEDISchema schema = new StaEDISchema(StaEDISchema.INTERCHANGE_ID,
                                               StaEDISchema.TRANSACTION_ID,
                                               StaEDISchema.IMPLEMENTATION_ID);

        schema.setTypes(types);

        LOGGER.log(Level.FINE, "Schema created, contains {0} types", types.size());

        return schema;
    }

    @Override
    public Schema createSchema(URL location) throws EDISchemaException {
        LOGGER.fine(() -> "Creating schema from URL: " + location);

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
        } else {
            throw new IllegalArgumentException("Unsupported property: " + name);
        }
    }

    @Override
    public void setProperty(String name, Object value) {
        if (isPropertySupported(name)) {
            if (value != null) {
                properties.put(name, value);
            } else {
                properties.remove(name);
            }
        } else {
            throw new IllegalArgumentException("Unsupported property: " + name);
        }
    }

    static Map<String, EDIType> readSchemaTypes(URL location, Map<String, Object> properties) throws EDISchemaException {
        LOGGER.fine(() -> "Reading schema from URL: " + location);

        try (InputStream stream = location.openStream()) {
            return readSchemaTypes(stream, properties);
        } catch (IOException e) {
            throw new EDISchemaException("Unable to read URL stream", e);
        }
    }

    static Map<String, EDIType> readSchemaTypes(InputStream stream, Map<String, Object> properties) throws EDISchemaException {
        try {
            return getReader(stream, properties).readTypes();
        } catch (StaEDISchemaReadException e) {
            throw wrapped(e);
        }
    }

    private static SchemaReader getReader(InputStream stream, Map<String, Object> properties) throws EDISchemaException {
        QName schemaElement;

        try {
            LOGGER.fine(() -> "Creating schema from stream");
            XMLStreamReader reader = FACTORY.createXMLStreamReader(stream);

            reader.nextTag();
            schemaElement = reader.getName();

            if (readerFactories.containsKey(schemaElement)) {
                return readerFactories.get(schemaElement).apply(reader, properties);
            }

            throw unexpectedElement(schemaElement, reader);
        } catch (XMLStreamException e) {
            throw new EDISchemaException("Exception checking start of schema XML", e);
        }
    }

    private static EDISchemaException wrapped(StaEDISchemaReadException e) {
        Location errorLocation = e.getLocation();

        if (errorLocation != null) {
            return new EDISchemaException(e.getMessage(), errorLocation, e);
        }

        return new EDISchemaException(e.getMessage(), e);
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
