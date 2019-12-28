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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.net.URL;

import org.junit.jupiter.api.Test;

import io.xlate.edi.schema.EDISchemaException;
import io.xlate.edi.schema.Schema;
import io.xlate.edi.schema.SchemaFactory;
import io.xlate.edi.stream.EDIStreamConstants.Standards;

@SuppressWarnings("resource")
public class StaEDISchemaFactoryTest {

    @Test
    public void testCreateSchemaByURL() throws EDISchemaException {
        SchemaFactory factory = SchemaFactory.newFactory();
        assertTrue(factory instanceof StaEDISchemaFactory, "Not an instance");
        URL schemaURL = getClass().getResource("/x12/EDISchema997.xml");
        Schema schema = factory.createSchema(schemaURL);
        assertEquals(StaEDISchema.TRANSACTION, schema.getMainLoop().getId(), "Incorrect root id");
        assertTrue(schema.containsSegment("AK9"), "Missing AK9 segment");
    }

    @Test
    public void testCreateSchemaByStream() throws EDISchemaException {
        SchemaFactory factory = SchemaFactory.newFactory();
        assertTrue(factory instanceof StaEDISchemaFactory, "Not an instance");
        InputStream schemaStream = getClass().getResourceAsStream("/x12/EDISchema997.xml");
        Schema schema = factory.createSchema(schemaStream);
        assertEquals(StaEDISchema.TRANSACTION, schema.getMainLoop().getId(), "Incorrect root id");
        assertTrue(schema.containsSegment("AK9"), "Missing AK9 segment");
    }

    @Test
    public void testCreateEdifactInterchangeSchema() throws EDISchemaException {
        Schema schema = SchemaUtils.getControlSchema(Standards.EDIFACT, new String[] { "UNOA", "4", "", "", "02" });
        assertNotNull(schema, "schema was null");
        assertEquals(StaEDISchema.INTERCHANGE, schema.getMainLoop().getId(), "Incorrect root id");
    }

    //TODO: no supported properties for now
    /*@Test
    public void testIsPropertySupported() {
    	SchemaFactory factory = SchemaFactory.newFactory();
    	assertTrue("FOO *is* supported", !factory.isPropertySupported("FOO"));
    }*/

    @Test
    public void testIsPropertyUnsupported() {
        SchemaFactory factory = SchemaFactory.newFactory();
        assertTrue(!factory.isPropertySupported("FOO"), "FOO *is* supported");
    }

    @Test
    public void testGetProperty() {
        SchemaFactory factory = SchemaFactory.newFactory();
        assertThrows(IllegalArgumentException.class, () -> factory.getProperty("FOO"));
    }

    @Test
    public void testSetProperty() {
        SchemaFactory factory = SchemaFactory.newFactory();
        assertThrows(IllegalArgumentException.class, () -> factory.setProperty("BAR", "BAZ"));
    }
}
