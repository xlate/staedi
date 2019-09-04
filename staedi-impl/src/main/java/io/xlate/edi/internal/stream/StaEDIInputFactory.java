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
package io.xlate.edi.internal.stream;

import io.xlate.edi.schema.Schema;
import io.xlate.edi.stream.EDIInputFactory;
import io.xlate.edi.stream.EDIStreamException;
import io.xlate.edi.stream.EDIStreamFilter;
import io.xlate.edi.stream.EDIStreamReader;

import java.io.InputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class StaEDIInputFactory extends EDIInputFactory {

    private static final String DEFAULT_ENCODING = "US-ASCII";

    private final Set<String> supportedCharsets;
    private final Map<String, Object> properties;
    private final Set<String> supportedProperties;

    public StaEDIInputFactory() {
        properties = new HashMap<>();
        supportedProperties = new HashSet<>();
        supportedCharsets = new HashSet<>();
        supportedCharsets.add(DEFAULT_ENCODING);
    }

    @Override
    public EDIStreamReader createEDIStreamReader(InputStream stream) throws EDIStreamException {
        return createEDIStreamReader(stream, DEFAULT_ENCODING, null);
    }

    @Override
    public EDIStreamReader createEDIStreamReader(InputStream stream, String encoding) throws EDIStreamException {
        return createEDIStreamReader(stream, encoding, null);
    }

    @Override
    public EDIStreamReader createEDIStreamReader(InputStream stream, Schema schema) throws EDIStreamException {
        return createEDIStreamReader(stream, DEFAULT_ENCODING, schema);
    }

    @Override
    public EDIStreamReader createEDIStreamReader(InputStream stream, String encoding, Schema schema) throws EDIStreamException {
        if (supportedCharsets.contains(encoding)) {
            return new StaEDIStreamReader(stream, encoding, schema, properties);
        }

        throw new EDIStreamException("Unsupported encoding: " + encoding);
    }

    @Override
    public EDIStreamReader createFilteredReader(EDIStreamReader reader, EDIStreamFilter filter) throws EDIStreamException {
        return new StaEDIFilteredStreamReader(reader, filter);
    }

    @Override
    public boolean isPropertySupported(String name) {
        return supportedProperties.contains(name);
    }

    @Override
    public Object getProperty(String name) {
        if (!isPropertySupported(name)) {
            throw new IllegalArgumentException("Unsupported property: " + name);
        }

        return properties.get(name);
    }

    @Override
    public void setProperty(String name, Object value) {
        if (!isPropertySupported(name)) {
            throw new IllegalArgumentException("Unsupported property: " + name);
        }

        properties.put(name, value);
    }
}
