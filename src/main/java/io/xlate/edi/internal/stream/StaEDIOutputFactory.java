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

import java.io.OutputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import io.xlate.edi.stream.EDIOutputFactory;
import io.xlate.edi.stream.EDIStreamConstants;
import io.xlate.edi.stream.EDIStreamException;
import io.xlate.edi.stream.EDIStreamWriter;

public class StaEDIOutputFactory extends EDIOutputFactory {

    private static final String DEFAULT_ENCODING = "US-ASCII";

    private final Set<String> supportedCharsets;
    private final Set<String> supportedProperties;
    private final Map<String, Object> properties;

    public StaEDIOutputFactory() {
        supportedCharsets = new HashSet<>();
        supportedCharsets.add(DEFAULT_ENCODING);

        supportedProperties = new HashSet<>();
        supportedProperties.add(EDIStreamConstants.Delimiters.SEGMENT);
        supportedProperties.add(EDIStreamConstants.Delimiters.DATA_ELEMENT);
        supportedProperties.add(EDIStreamConstants.Delimiters.COMPONENT_ELEMENT);
        supportedProperties.add(EDIStreamConstants.Delimiters.REPETITION);
        supportedProperties.add(EDIStreamConstants.Delimiters.DECIMAL);
        supportedProperties.add(EDIStreamConstants.Delimiters.RELEASE);

        supportedProperties.add(PRETTY_PRINT);

        properties = new HashMap<>();
        properties.put(PRETTY_PRINT, Boolean.FALSE);
    }

    @Override
    public EDIStreamWriter createEDIStreamWriter(OutputStream stream) throws EDIStreamException {
        return createEDIStreamWriter(stream, DEFAULT_ENCODING);
    }

    @Override
    public EDIStreamWriter createEDIStreamWriter(OutputStream stream, String encoding) throws EDIStreamException {
        if (supportedCharsets.contains(encoding)) {
            return new StaEDIStreamWriter(stream, encoding, properties);
        }
        throw new EDIStreamException("Unsupported encoding: " + encoding);
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

        if (PRETTY_PRINT.equals(name) && !(value instanceof Boolean)) {
            throw new IllegalArgumentException(name + " must be Boolean");
        }

        properties.put(name, value);
    }
}
