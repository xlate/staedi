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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.OutputStream;

import org.junit.jupiter.api.Test;

import io.xlate.edi.stream.EDIOutputFactory;
import io.xlate.edi.stream.EDIStreamConstants;
import io.xlate.edi.stream.EDIStreamException;
import io.xlate.edi.stream.EDIStreamWriter;

public class StaEDIOutputFactoryTest {

    @Test
    public void testNewFactory() {
        EDIOutputFactory factory = EDIOutputFactory.newFactory();
        assertTrue(factory instanceof StaEDIOutputFactory);
    }

    @Test
    public void testCreateEDIStreamWriterOutputStream() {
        EDIOutputFactory factory = EDIOutputFactory.newFactory();
        OutputStream stream = System.out;
        EDIStreamWriter writer = factory.createEDIStreamWriter(stream);
        assertNotNull(writer, "Writer was null");
    }

    @Test
    public void testCreateEDIStreamWriterOutputStreamString() throws EDIStreamException {
        EDIOutputFactory factory = EDIOutputFactory.newFactory();
        OutputStream stream = System.out;
        String encoding = "US-ASCII";
        EDIStreamWriter writer = factory.createEDIStreamWriter(stream, encoding);
        assertNotNull(writer, "Writer was null");
    }

    @Test
    public void testCreateEDIStreamWriterInvalidEncoding() {
        EDIOutputFactory factory = EDIOutputFactory.newFactory();
        OutputStream stream = System.out;
        String encoding = "EBCDIC";
        EDIStreamException e = assertThrows(EDIStreamException.class, () -> factory.createEDIStreamWriter(stream, encoding));
        assertEquals("Unsupported encoding: EBCDIC", e.getMessage());
    }

    @Test
    public void testIsPropertySupported() {
        EDIOutputFactory factory = EDIOutputFactory.newFactory();
        assertTrue(!factory.isPropertySupported("FOO"), "FOO supported");
    }

    @Test
    public void testGetPropertyUnsupported() {
        EDIOutputFactory factory = EDIOutputFactory.newFactory();
        assertThrows(IllegalArgumentException.class, () -> factory.getProperty("FOO"));
    }

    @Test()
    public void testGetPropertySupported() {
        EDIOutputFactory factory = EDIOutputFactory.newFactory();
        assertNull(factory.getProperty(EDIStreamConstants.Delimiters.DATA_ELEMENT));
    }

    @Test
    public void testSetPropertyUnsupported() {
        EDIOutputFactory factory = EDIOutputFactory.newFactory();
        assertThrows(IllegalArgumentException.class, () -> factory.setProperty("FOO", null));
    }

    @Test
    public void testSetPropertySupported() {
        EDIOutputFactory factory = EDIOutputFactory.newFactory();
        factory.setProperty(EDIStreamConstants.Delimiters.REPETITION, '`');
        assertEquals('`', factory.getProperty(EDIStreamConstants.Delimiters.REPETITION));
    }
}
