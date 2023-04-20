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
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import javax.xml.stream.XMLStreamWriter;

import io.xlate.edi.stream.EDIOutputErrorReporter;
import io.xlate.edi.stream.EDIOutputFactory;
import io.xlate.edi.stream.EDIStreamConstants;
import io.xlate.edi.stream.EDIStreamException;
import io.xlate.edi.stream.EDIStreamWriter;

public class StaEDIOutputFactory extends EDIOutputFactory {

    private EDIOutputErrorReporter reporter;

    public StaEDIOutputFactory() {
        supportedProperties.add(EDIStreamConstants.Delimiters.SEGMENT);
        supportedProperties.add(EDIStreamConstants.Delimiters.DATA_ELEMENT);
        supportedProperties.add(EDIStreamConstants.Delimiters.COMPONENT_ELEMENT);
        supportedProperties.add(EDIStreamConstants.Delimiters.REPETITION);
        supportedProperties.add(EDIStreamConstants.Delimiters.DECIMAL);
        supportedProperties.add(EDIStreamConstants.Delimiters.RELEASE);
        supportedProperties.add(PRETTY_PRINT);
        supportedProperties.add(TRUNCATE_EMPTY_ELEMENTS);
        supportedProperties.add(FORMAT_ELEMENTS);
        supportedProperties.add(EDI_VALIDATE_CONTROL_STRUCTURE);

        properties.put(PRETTY_PRINT, Boolean.FALSE);
    }

    @Override
    public EDIStreamWriter createEDIStreamWriter(OutputStream stream) {
        return new StaEDIStreamWriter(stream, StandardCharsets.UTF_8, properties, reporter);
    }

    @Override
    public EDIStreamWriter createEDIStreamWriter(OutputStream stream, String encoding) throws EDIStreamException {
        if (Charset.isSupported(encoding)) {
            return new StaEDIStreamWriter(stream, Charset.forName(encoding), properties, reporter);
        }
        throw new EDIStreamException("Unsupported encoding: " + encoding);
    }

    @Override
    public XMLStreamWriter createXMLStreamWriter(EDIStreamWriter writer) {
        return new StaEDIXMLStreamWriter(writer);
    }

    @Override
    public EDIOutputErrorReporter getErrorReporter() {
        return this.reporter;
    }

    @Override
    public void setErrorReporter(EDIOutputErrorReporter reporter) {
        this.reporter = reporter;
    }
}
