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

    final String INTERCHANGE_V2 = "{http://xlate.io/EDISchema/v2}interchange";
    final String TRANSACTION_V2 = "{http://xlate.io/EDISchema/v2}transaction";

    final String STANDARD_V3 = "{http://xlate.io/EDISchema/v3}transaction";
    final String IMPL_V3 = "{http://xlate.io/EDISchema/v3}implementation";

    @Test
    public void testCreateSchemaByURL() throws EDISchemaException {
        SchemaFactory factory = SchemaFactory.newFactory();
        assertTrue(factory instanceof StaEDISchemaFactory, "Not an instance");
        URL schemaURL = getClass().getResource("/x12/EDISchema997.xml");
        Schema schema = factory.createSchema(schemaURL);
        assertEquals(TRANSACTION_V2, schema.getStandard().getId(), "Incorrect root id");
        assertTrue(schema.containsSegment("AK9"), "Missing AK9 segment");
    }

    @Test
    public void testCreateSchemaByStream() throws EDISchemaException {
        SchemaFactory factory = SchemaFactory.newFactory();
        assertTrue(factory instanceof StaEDISchemaFactory, "Not an instance");
        InputStream schemaStream = getClass().getResourceAsStream("/x12/EDISchema997.xml");
        Schema schema = factory.createSchema(schemaStream);
        assertEquals(TRANSACTION_V2, schema.getStandard().getId(), "Incorrect root id");
        assertTrue(schema.containsSegment("AK9"), "Missing AK9 segment");
    }

    @Test
    public void testCreateEdifactInterchangeSchema() throws EDISchemaException {
        Schema schema = SchemaUtils.getControlSchema(Standards.EDIFACT, new String[] { "UNOA", "4", "", "", "02" });
        assertNotNull(schema, "schema was null");
        assertEquals(INTERCHANGE_V2, schema.getStandard().getId(), "Incorrect root id");
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
    public void testExceptionThrownWithoutInterchangeAndTransactionV2() {
        SchemaFactory factory = SchemaFactory.newFactory();
        InputStream stream = new ByteArrayInputStream(("<schema xmlns='" + StaEDISchemaFactory.XMLNS_V2 + "'><random/></schema>").getBytes());
        EDISchemaException thrown = assertThrows(EDISchemaException.class, () -> factory.createSchema(stream));
        assertEquals("Unexpected XML element [{"+StaEDISchemaFactory.XMLNS_V2+"}random]", thrown.getOriginalMessage());
    }

    @Test
    public void testInterchangeRequiredAttributesV2() {
        SchemaFactory factory = SchemaFactory.newFactory();
        InputStream stream1 = new ByteArrayInputStream(("<schema xmlns='" + StaEDISchemaFactory.XMLNS_V2 + "'><interchange _header='ABC' trailer='XYZ' /></schema>").getBytes());
        EDISchemaException thrown1 = assertThrows(EDISchemaException.class, () -> factory.createSchema(stream1));
        assertEquals("Missing required attribute: [header]", thrown1.getOriginalMessage());

        InputStream stream2 = new ByteArrayInputStream(("<schema xmlns='" + StaEDISchemaFactory.XMLNS_V2 + "'><interchange header='ABC' _trailer='XYZ' /></schema>").getBytes());
        EDISchemaException thrown2 = assertThrows(EDISchemaException.class, () -> factory.createSchema(stream2));
        assertEquals("Missing required attribute: [trailer]", thrown2.getOriginalMessage());

    }

    @Test
    public void testUnexpectedElementBeforeSequenceV2() {
        SchemaFactory factory = SchemaFactory.newFactory();
        InputStream stream = new ByteArrayInputStream((""
                + "<schema xmlns='" + StaEDISchemaFactory.XMLNS_V2 + "'>"
                        + "<interchange header='ABC' trailer='XYZ'>"
                        + "<description><![CDATA[TEXT ALLOWED]]></description>"
                        + "<unexpected></unexpected>"
                        + "<sequence></sequence>"
                        + "</interchange>"
                        + "</schema>").getBytes());
        EDISchemaException thrown = assertThrows(EDISchemaException.class, () -> factory.createSchema(stream));
        assertEquals("Unexpected XML element [{"+StaEDISchemaFactory.XMLNS_V2+"}unexpected]", thrown.getOriginalMessage());
    }

    @Test
    public void testCreateSchemaByStreamV3() throws EDISchemaException {
        SchemaFactory factory = SchemaFactory.newFactory();
        assertTrue(factory instanceof StaEDISchemaFactory, "Not an instance");
        InputStream schemaStream = getClass().getResourceAsStream("/x12/IG-999.xml");
        Schema schema = factory.createSchema(schemaStream);
        assertEquals(STANDARD_V3, schema.getStandard().getId(), "Incorrect root id");
        assertEquals(IMPL_V3, schema.getImplementation().getId(), "Incorrect impl id");
        assertTrue(schema.containsSegment("AK9"), "Missing AK9 segment");
    }

    @Test
    public void testDuplicateImplPositionSpecified() {
        SchemaFactory factory = SchemaFactory.newFactory();
        assertTrue(factory instanceof StaEDISchemaFactory, "Not an instance");
        InputStream stream = getClass().getResourceAsStream("/x12/implSchemaDuplicatePosition.xml");
        EDISchemaException thrown = assertThrows(EDISchemaException.class, () -> factory.createSchema(stream));
        Throwable cause = thrown.getCause();
        assertNotNull(cause);
        assertEquals("Duplicate value for position 1", cause.getMessage());
    }

    @Test
    public void testDiscriminatorElementTooLarge() {
        SchemaFactory factory = SchemaFactory.newFactory();
        assertTrue(factory instanceof StaEDISchemaFactory, "Not an instance");
        InputStream stream = getClass().getResourceAsStream("/x12/discriminators/element-too-large.xml");
        EDISchemaException thrown = assertThrows(EDISchemaException.class, () -> factory.createSchema(stream));
        Throwable cause = thrown.getCause();
        assertNotNull(cause);
        assertEquals("Discriminator element position invalid: 3.1", cause.getMessage());
    }

    @Test
    public void testDiscriminatorElementTooSmall() {
        SchemaFactory factory = SchemaFactory.newFactory();
        assertTrue(factory instanceof StaEDISchemaFactory, "Not an instance");
        InputStream stream = getClass().getResourceAsStream("/x12/discriminators/element-too-small.xml");
        EDISchemaException thrown = assertThrows(EDISchemaException.class, () -> factory.createSchema(stream));
        Throwable cause = thrown.getCause();
        assertNotNull(cause);
        assertEquals("Discriminator element position invalid: 0.1", cause.getMessage());
    }

    @Test
    public void testDiscriminatorElementNotSpecified() {
        SchemaFactory factory = SchemaFactory.newFactory();
        assertTrue(factory instanceof StaEDISchemaFactory, "Not an instance");
        InputStream stream = getClass().getResourceAsStream("/x12/discriminators/element-not-specified.xml");
        EDISchemaException thrown = assertThrows(EDISchemaException.class, () -> factory.createSchema(stream));
        Throwable cause = thrown.getCause();
        assertNotNull(cause);
        assertEquals("Discriminator position is unused (not specified): 2.1", cause.getMessage());
    }

    @Test
    public void testDiscriminatorComponentTooLarge() {
        SchemaFactory factory = SchemaFactory.newFactory();
        assertTrue(factory instanceof StaEDISchemaFactory, "Not an instance");
        InputStream stream = getClass().getResourceAsStream("/x12/discriminators/component-too-large.xml");
        EDISchemaException thrown = assertThrows(EDISchemaException.class, () -> factory.createSchema(stream));
        Throwable cause = thrown.getCause();
        assertNotNull(cause);
        assertEquals("Discriminator component position invalid: 2.3", cause.getMessage());
    }

    @Test
    public void testDiscriminatorComponentTooSmall() {
        SchemaFactory factory = SchemaFactory.newFactory();
        assertTrue(factory instanceof StaEDISchemaFactory, "Not an instance");
        InputStream stream = getClass().getResourceAsStream("/x12/discriminators/component-too-small.xml");
        EDISchemaException thrown = assertThrows(EDISchemaException.class, () -> factory.createSchema(stream));
        Throwable cause = thrown.getCause();
        assertNotNull(cause);
        assertEquals("Discriminator component position invalid: 2.0", cause.getMessage());
    }

    @Test
    public void testDiscriminatorMissingEnumeration() {
        SchemaFactory factory = SchemaFactory.newFactory();
        assertTrue(factory instanceof StaEDISchemaFactory, "Not an instance");
        InputStream stream = getClass().getResourceAsStream("/x12/discriminators/element-without-enumeration.xml");
        EDISchemaException thrown = assertThrows(EDISchemaException.class, () -> factory.createSchema(stream));
        Throwable cause = thrown.getCause();
        assertNotNull(cause);
        assertEquals("Discriminator element does not specify value enumeration: 2.2", cause.getMessage());
    }

    @Test
    public void testInvalidSyntaxTypeValue() {
        SchemaFactory factory = SchemaFactory.newFactory();
        InputStream stream = getClass().getResourceAsStream("/EDIFACT/fragment-uci-invalid-syntax-type.xml");
        EDISchemaException thrown = assertThrows(EDISchemaException.class, () -> factory.createSchema(stream));
        Throwable cause = thrown.getCause();
        assertNotNull(cause);
        assertEquals("Invalid syntax 'type': [conditional-junk]", cause.getMessage());
    }

    @Test
    public void testInvalidMinOccursValue() {
        SchemaFactory factory = SchemaFactory.newFactory();
        InputStream stream = getClass().getResourceAsStream("/EDIFACT/fragment-uci-invalid-min-occurs.xml");
        EDISchemaException thrown = assertThrows(EDISchemaException.class, () -> factory.createSchema(stream));
        Throwable cause = thrown.getCause();
        assertNotNull(cause);
        assertEquals("Invalid minOccurs", cause.getMessage());
    }

    @Test
    public void testDuplicateElementTypeNames() {
        SchemaFactory factory = SchemaFactory.newFactory();
        InputStream stream = getClass().getResourceAsStream("/EDIFACT/fragment-uci-duplicate-element-names.xml");
        EDISchemaException thrown = assertThrows(EDISchemaException.class, () -> factory.createSchema(stream));
        Throwable cause = thrown.getCause();
        assertNotNull(cause);
        assertEquals("duplicate name: DE0004", cause.getMessage());
    }
}
