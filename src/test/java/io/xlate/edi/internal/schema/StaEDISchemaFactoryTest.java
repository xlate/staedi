/*******************************************************************************
 * Copyright 2017 xlate.io LLC, http://www.xlate.io
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License.  You may obtain a copy
 * of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations under
 * the License.
 ******************************************************************************/
package io.xlate.edi.internal.schema;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.InputStream;
import java.net.URL;

import org.junit.Test;

import io.xlate.edi.internal.schema.SchemaUtils;
import io.xlate.edi.internal.schema.StaEDISchema;
import io.xlate.edi.internal.schema.StaEDISchemaFactory;
import io.xlate.edi.schema.EDISchemaException;
import io.xlate.edi.schema.Schema;
import io.xlate.edi.schema.SchemaFactory;
import io.xlate.edi.stream.EDIStreamConstants.Standards;

public class StaEDISchemaFactoryTest {

	@Test
	public void testCreateSchemaByURL() throws EDISchemaException {
		SchemaFactory factory = SchemaFactory.newFactory();
		assertTrue("Not an instance", factory instanceof StaEDISchemaFactory);
		URL schemaURL = getClass().getClassLoader().getResource("x12/EDISchema997.xml");
		Schema schema = factory.createSchema(schemaURL);
		assertEquals("Incorrect root id",  StaEDISchema.TRANSACTION, schema.getMainLoop().getId());
		assertTrue("Missing AK9 segment", schema.containsSegment("AK9"));
	}

	@Test
	public void testCreateSchemaByStream() throws EDISchemaException {
		SchemaFactory factory = SchemaFactory.newFactory();
		assertTrue("Not an instance", factory instanceof StaEDISchemaFactory);
		InputStream schemaStream = getClass().getClassLoader().getResourceAsStream("x12/EDISchema997.xml");
		Schema schema = factory.createSchema(schemaStream);
		assertEquals("Incorrect root id",  StaEDISchema.TRANSACTION, schema.getMainLoop().getId());
		assertTrue("Missing AK9 segment", schema.containsSegment("AK9"));
	}

	@Test
	public void testCreateEdifactInterchangeSchema() throws EDISchemaException {
		Schema schema = SchemaUtils.getControlSchema(Standards.EDIFACT, new String[] { "UNOA", "4", "", "", "02" });
		assertNotNull("schema was null", schema);
		assertEquals("Incorrect root id",  StaEDISchema.INTERCHANGE, schema.getMainLoop().getId());
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
		assertTrue("FOO *is* supported", !factory.isPropertySupported("FOO"));
	}

	@Test(expected = IllegalArgumentException.class)
	public void testGetProperty() {
		SchemaFactory factory = SchemaFactory.newFactory();
		factory.getProperty("FOO");
	}

	@Test(expected = IllegalArgumentException.class)
	public void testSetProperty() {
		SchemaFactory factory = SchemaFactory.newFactory();
		factory.setProperty("BAR", "BAZ");
	}
}
