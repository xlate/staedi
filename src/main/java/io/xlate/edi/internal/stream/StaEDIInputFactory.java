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
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import io.xlate.edi.internal.stream.json.JsonParserFactory;
import io.xlate.edi.schema.Schema;
import io.xlate.edi.stream.EDIInputErrorReporter;
import io.xlate.edi.stream.EDIInputFactory;
import io.xlate.edi.stream.EDIStreamException;
import io.xlate.edi.stream.EDIStreamFilter;
import io.xlate.edi.stream.EDIStreamReader;

public class StaEDIInputFactory extends EDIInputFactory {

    private EDIInputErrorReporter reporter;

    public StaEDIInputFactory() {
        supportedProperties.add(EDI_VALIDATE_CONTROL_STRUCTURE);
        supportedProperties.add(EDI_VALIDATE_CONTROL_CODE_VALUES);
        supportedProperties.add(EDI_IGNORE_EXTRANEOUS_CHARACTERS);
        supportedProperties.add(EDI_NEST_HIERARCHICAL_LOOPS);

        supportedProperties.add(XML_DECLARE_TRANSACTION_XMLNS);
        supportedProperties.add(XML_WRAP_TRANSACTION_CONTENTS);

        supportedProperties.add(JSON_NULL_EMPTY_ELEMENTS);
        supportedProperties.add(JSON_OBJECT_ELEMENTS);
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
        Objects.requireNonNull(stream, "stream must not be null");
        return new StaEDIStreamReader(stream, StandardCharsets.UTF_8, schema, properties, getErrorReporter());
    }

    @SuppressWarnings("resource")
    @Override
    public EDIStreamReader createEDIStreamReader(InputStream stream, String encoding, Schema schema) throws EDIStreamException {
        Objects.requireNonNull(stream, "stream must not be null");

        if (Charset.isSupported(encoding)) {
            return new StaEDIStreamReader(stream, Charset.forName(encoding), schema, properties, getErrorReporter());
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
    public <J> J createJsonParser(EDIStreamReader reader, Class<J> type) {
        return JsonParserFactory.createJsonParser(reader, type, properties);
    }

    @Override
    public EDIInputErrorReporter getErrorReporter() {
        return reporter;
    }

    @Override
    public void setErrorReporter(EDIInputErrorReporter reporter) {
        this.reporter = reporter;
    }

    @Override
    @SuppressWarnings({ "java:S1123", "java:S1133" })
    @Deprecated /*(forRemoval = true, since = "1.9")*/
    public io.xlate.edi.stream.EDIReporter getEDIReporter() {
        EDIInputErrorReporter errorReporter = getErrorReporter();

        if (errorReporter instanceof io.xlate.edi.stream.EDIReporter) {
            return (io.xlate.edi.stream.EDIReporter) errorReporter;
        }

        throw new ClassCastException("Can not cast reporter to EDIReporter; did you mean to call getErrorReporter() ?");
    }

    @Override
    @SuppressWarnings({ "java:S1123", "java:S1133" })
    @Deprecated /*(forRemoval = true, since = "1.9")*/
    public void setEDIReporter(io.xlate.edi.stream.EDIReporter reporter) {
        setErrorReporter(reporter);
    }
}
