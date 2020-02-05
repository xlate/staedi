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

import java.io.ByteArrayInputStream;
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
        assertEquals(StaEDISchema.TRANSACTION, schema.getStandard().getId(), "Incorrect root id");
        assertTrue(schema.containsSegment("AK9"), "Missing AK9 segment");
    }

    @Test
    public void testCreateSchemaByStream() throws EDISchemaException {
        SchemaFactory factory = SchemaFactory.newFactory();
        assertTrue(factory instanceof StaEDISchemaFactory, "Not an instance");
        InputStream schemaStream = getClass().getResourceAsStream("/x12/EDISchema997.xml");
        Schema schema = factory.createSchema(schemaStream);
        assertEquals(StaEDISchema.TRANSACTION, schema.getStandard().getId(), "Incorrect root id");
        assertTrue(schema.containsSegment("AK9"), "Missing AK9 segment");
    }

    @Test
    public void testCreateEdifactInterchangeSchema() throws EDISchemaException {
        Schema schema = SchemaUtils.getControlSchema(Standards.EDIFACT, new String[] { "UNOA", "4", "", "", "02" });
        assertNotNull(schema, "schema was null");
        assertEquals(StaEDISchema.INTERCHANGE, schema.getStandard().getId(), "Incorrect root id");
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

    @Test
    public void testExceptionThrownWithoutSchema() {
        SchemaFactory factory = SchemaFactory.newFactory();
        InputStream stream = new ByteArrayInputStream("<noschema></noschema>".getBytes());
        EDISchemaException thrown = assertThrows(EDISchemaException.class, () -> factory.createSchema(stream));
        assertEquals("Unexpected XML element [noschema]", thrown.getOriginalMessage());
    }

    @Test
    public void testExceptionThrownWithoutInterchangeAndTransaction() {
        SchemaFactory factory = SchemaFactory.newFactory();
        InputStream stream = new ByteArrayInputStream(("<schema xmlns='" + StaEDISchemaFactory.XMLNS + "'><random/></schema>").getBytes());
        EDISchemaException thrown = assertThrows(EDISchemaException.class, () -> factory.createSchema(stream));
        assertEquals("Unexpected XML element [{"+StaEDISchemaFactory.XMLNS+"}random]", thrown.getOriginalMessage());
    }

    @Test
    public void testInterchangeRequiredAttributes() {
        SchemaFactory factory = SchemaFactory.newFactory();
        InputStream stream1 = new ByteArrayInputStream(("<schema xmlns='" + StaEDISchemaFactory.XMLNS + "'><interchange _header='ABC' trailer='XYZ' /></schema>").getBytes());
        EDISchemaException thrown1 = assertThrows(EDISchemaException.class, () -> factory.createSchema(stream1));
        assertEquals("Missing required attribute [header]", thrown1.getOriginalMessage());

        InputStream stream2 = new ByteArrayInputStream(("<schema xmlns='" + StaEDISchemaFactory.XMLNS + "'><interchange header='ABC' _trailer='XYZ' /></schema>").getBytes());
        EDISchemaException thrown2 = assertThrows(EDISchemaException.class, () -> factory.createSchema(stream2));
        assertEquals("Missing required attribute [trailer]", thrown2.getOriginalMessage());

    }

    @Test
    public void testUnexpectedElementBeforeSequence() {
        SchemaFactory factory = SchemaFactory.newFactory();
        InputStream stream = new ByteArrayInputStream((""
                + "<schema xmlns='" + StaEDISchemaFactory.XMLNS + "'>"
                        + "<interchange header='ABC' trailer='XYZ'>"
                        + "<description><![CDATA[TEXT ALLOWED]]></description>"
                        + "<unexpected></unexpected>"
                        + "<sequence></sequence>"
                        + "</interchange>"
                        + "</schema>").getBytes());
        EDISchemaException thrown = assertThrows(EDISchemaException.class, () -> factory.createSchema(stream));
        assertEquals("Unexpected XML element [{"+StaEDISchemaFactory.XMLNS+"}unexpected]", thrown.getOriginalMessage());
    }

}
