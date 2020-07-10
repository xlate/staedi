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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import io.xlate.edi.schema.EDISchemaException;
import io.xlate.edi.schema.Schema;
import io.xlate.edi.schema.SchemaFactory;
import io.xlate.edi.stream.EDIInputErrorReporter;
import io.xlate.edi.stream.EDIInputFactory;
import io.xlate.edi.stream.EDIStreamException;
import io.xlate.edi.stream.EDIStreamReader;

@SuppressWarnings("resource")
class StaEDIInputFactoryTest {

    @Test
    void testNewFactory() {
        EDIInputFactory factory = EDIInputFactory.newFactory();
        assertTrue(factory instanceof StaEDIInputFactory);
    }

    @Test
    void testCreateEDIStreamReader() {
        EDIInputFactory factory = EDIInputFactory.newFactory();
        InputStream stream = getClass().getResourceAsStream("/x12/simple997.edi");
        EDIStreamReader reader = factory.createEDIStreamReader(stream);
        assertNotNull(reader, "Reader was null");
    }

    @Test
    void testCreateEDIStreamReaderEncoded() throws EDIStreamException {
        EDIInputFactory factory = EDIInputFactory.newFactory();
        InputStream stream = getClass().getResourceAsStream("/x12/simple997.edi");
        String encoding = "US-ASCII";
        EDIStreamReader reader = factory.createEDIStreamReader(stream, encoding);
        assertNotNull(reader, "Reader was null");
    }

    @Test
    void testCreateEDIStreamReaderInvalidEncoding() {
        EDIInputFactory factory = EDIInputFactory.newFactory();
        InputStream stream = getClass().getResourceAsStream("/x12/simple997.edi");
        String encoding = "EBCDIC";
        EDIStreamException e = assertThrows(EDIStreamException.class, () -> factory.createEDIStreamReader(stream, encoding));
        assertEquals("Unsupported encoding: EBCDIC", e.getMessage());
    }

    @Test
    void testCreateEDIStreamReaderValidated() throws EDISchemaException {
        EDIInputFactory factory = EDIInputFactory.newFactory();
        InputStream stream = getClass().getResourceAsStream("/x12/simple997.edi");
        SchemaFactory schemaFactory = SchemaFactory.newFactory();
        Schema schema = schemaFactory.createSchema(getClass().getResourceAsStream("/x12/EDISchema997.xml"));
        EDIStreamReader reader = factory.createEDIStreamReader(stream, schema);
        assertNotNull(reader, "Reader was null");
    }

    @Test
    void testCreateEDIStreamReaderEncodedValidated() throws EDIStreamException, EDISchemaException {
        EDIInputFactory factory = EDIInputFactory.newFactory();
        InputStream stream = getClass().getResourceAsStream("/x12/simple997.edi");
        String encoding = "US-ASCII";
        SchemaFactory schemaFactory = SchemaFactory.newFactory();
        Schema schema = schemaFactory.createSchema(getClass().getResourceAsStream("/x12/EDISchema997.xml"));
        EDIStreamReader reader = factory.createEDIStreamReader(stream, encoding, schema);
        assertNotNull(reader, "Reader was null");
    }

    @Test
    void testCreateFilteredReader() {
        EDIInputFactory factory = EDIInputFactory.newFactory();
        EDIStreamReader reader = null;
        // EDIStreamFilter is a functional interface
        reader = factory.createFilteredReader(reader, (r) -> false);
        assertNotNull(reader, "Reader was null");
    }

    @Test
    void testIsPropertySupported() {
        EDIInputFactory factory = EDIInputFactory.newFactory();
        assertFalse(factory.isPropertySupported("FOO"), "Reporter property not supported");
    }

    @Test
    void testGetPropertyUnsupported() {
        EDIInputFactory factory = EDIInputFactory.newFactory();
        assertThrows(IllegalArgumentException.class, () -> factory.getProperty("FOO"));
    }

    @Test
    void testSetPropertyUnsupported() {
        EDIInputFactory factory = EDIInputFactory.newFactory();
        assertThrows(IllegalArgumentException.class, () -> factory.setProperty("FOO", null));
    }

    @SuppressWarnings("deprecation")
    @Test
    void testDeprecatedReporterMethodUsesDeprecatedType() {
        io.xlate.edi.stream.EDIReporter deprecatedReporter = Mockito.mock(io.xlate.edi.stream.EDIReporter.class);
        EDIInputFactory factory = EDIInputFactory.newFactory();
        factory.setEDIReporter(deprecatedReporter);
        assertSame(deprecatedReporter, factory.getErrorReporter());
        assertSame(deprecatedReporter, factory.getEDIReporter());
    }

    @SuppressWarnings("deprecation")
    @Test
    void testDeprecatedReporterMethodThrowsException() {
        EDIInputErrorReporter reporter = Mockito.mock(EDIInputErrorReporter.class);
        EDIInputFactory factory = EDIInputFactory.newFactory();
        factory.setErrorReporter(reporter);
        assertSame(reporter, factory.getErrorReporter());
        assertThrows(ClassCastException.class, () -> factory.getEDIReporter());
    }
}
