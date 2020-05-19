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

import java.io.InputStream;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import io.xlate.edi.schema.Schema;
import io.xlate.edi.stream.EDIInputFactory;
import io.xlate.edi.stream.EDIReporter;
import io.xlate.edi.stream.EDIStreamException;
import io.xlate.edi.stream.EDIStreamFilter;
import io.xlate.edi.stream.EDIStreamReader;

public class StaEDIInputFactory extends EDIInputFactory {

    private static final String DEFAULT_ENCODING = "US-ASCII";

    private final Set<String> supportedCharsets;
    private EDIReporter reporter;

    public StaEDIInputFactory() {
        supportedProperties.add(EDI_VALIDATE_CONTROL_STRUCTURE);
        supportedProperties.add(EDI_VALIDATE_CONTROL_CODE_VALUES);
        supportedProperties.add(XML_DECLARE_TRANSACTION_XMLNS);

        supportedCharsets = new HashSet<>();
        supportedCharsets.add(DEFAULT_ENCODING);
    }

    @Override
    public EDIStreamReader createEDIStreamReader(InputStream stream) {
        return createEDIStreamReader(stream, (Schema) null);
    }

    @Override
    public EDIStreamReader createEDIStreamReader(InputStream stream, String encoding) throws EDIStreamException {
        return createEDIStreamReader(stream, encoding, null);
    }

    @Override
    public EDIStreamReader createEDIStreamReader(InputStream stream, Schema schema) {
        return new StaEDIStreamReader(stream, DEFAULT_ENCODING, schema, properties, getEDIReporter());
    }

    @SuppressWarnings("resource")
    @Override
    public EDIStreamReader createEDIStreamReader(InputStream stream, String encoding, Schema schema) throws EDIStreamException {
        Objects.requireNonNull(stream);

        if (supportedCharsets.contains(encoding)) {
            return new StaEDIStreamReader(stream, encoding, schema, properties, getEDIReporter());
        }

        throw new EDIStreamException("Unsupported encoding: " + encoding);
    }

    @Override
    public EDIStreamReader createFilteredReader(EDIStreamReader reader, EDIStreamFilter filter) {
        return new StaEDIFilteredStreamReader(reader, filter);
    }

    @Override
    public XMLStreamReader createXMLStreamReader(EDIStreamReader reader) throws XMLStreamException {
        return new StaEDIXMLStreamReader(reader, properties);
    }

    @Override
    public EDIReporter getEDIReporter() {
        return reporter;
    }

    @Override
    public void setEDIReporter(EDIReporter reporter) {
        this.reporter = reporter;
    }
}
