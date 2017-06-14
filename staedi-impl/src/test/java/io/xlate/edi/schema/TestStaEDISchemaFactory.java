package io.xlate.edi.schema;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.InputStream;
import java.net.URL;

import org.junit.Test;

import io.xlate.edi.stream.EDIStreamConstants.Standards;

@SuppressWarnings("static-method")
public class TestStaEDISchemaFactory {

	@Test
	public void testCreateSchemaByURL() throws EDISchemaException {
		SchemaFactory factory = SchemaFactory.newFactory();
		assertTrue("Not an instance", factory instanceof StaEDISchemaFactory);
		URL schemaURL = getClass().getClassLoader().getResource("x12/EDISchema997.xml");
		Schema schema = factory.createSchema(schemaURL);
		assertEquals("Incorrect root id",  StaEDISchema.MAIN, schema.getMainLoop().getId());
		assertTrue("Missing AK9 segment", schema.containsSegment("AK9"));
	}

	@Test
	public void testCreateSchemaByStream() throws EDISchemaException {
		SchemaFactory factory = SchemaFactory.newFactory();
		assertTrue("Not an instance", factory instanceof StaEDISchemaFactory);
		@SuppressWarnings("resource")
		InputStream schemaStream = getClass().getClassLoader().getResourceAsStream("x12/EDISchema997.xml");
		Schema schema = factory.createSchema(schemaStream);
		assertEquals("Incorrect root id",  StaEDISchema.MAIN, schema.getMainLoop().getId());
		assertTrue("Missing AK9 segment", schema.containsSegment("AK9"));
	}

	@Test
	public void testCreateMapDBFactory() {
		SchemaFactory factory = SchemaFactory.newFactory(StaEDIMapSchemaFactory.ID, Thread.currentThread().getContextClassLoader());
		assertTrue(factory instanceof StaEDIMapSchemaFactory);
	}

	@Test
	@org.junit.Ignore
	public void testCreateMapDBInterchangeSchema() throws EDISchemaException {
		Schema schema = SchemaUtils.getMapSchema(Standards.EDIFACT, "40200", "INTERCHANGE");
		assertEquals("Incorrect root id",  StaEDISchema.MAIN, schema.getMainLoop().getId());
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
