package io.xlate.edi.stream;

import java.io.InputStream;

import org.junit.Assert;
import org.junit.Test;

import io.xlate.edi.schema.EDISchemaException;
import io.xlate.edi.schema.Schema;
import io.xlate.edi.schema.SchemaFactory;

@SuppressWarnings({ "static-method", "resource" })
public class TestStaEDIInputFactory {

	@Test
	public void testNewFactory() {
		EDIInputFactory factory = EDIInputFactory.newFactory();
		Assert.assertTrue(factory instanceof StaEDIInputFactory);
	}

	@Test
	public void testNewFactoryById() {
		String factoryId = StaEDIInputFactory.class.getName();
		ClassLoader loader = null;// Thread.currentThread().getContextClassLoader();
		EDIInputFactory factory = EDIInputFactory.newFactory(factoryId, loader);
		Assert.assertTrue(factory instanceof StaEDIInputFactory);
	}

	@Test
	public void testCreateEDIStreamReader()
			throws EDIStreamException {

		EDIInputFactory factory = EDIInputFactory.newFactory();
		InputStream stream = getClass().getClassLoader().getResourceAsStream("x12/simple997.edi");
		EDIStreamReader reader = factory.createEDIStreamReader(stream);
		Assert.assertNotNull("Reader was null", reader);
	}

	@Test
	public void testCreateEDIStreamReaderEncoded()
			throws EDIStreamException {

		EDIInputFactory factory = EDIInputFactory.newFactory();
		InputStream stream = getClass().getClassLoader().getResourceAsStream("x12/simple997.edi");
		String encoding = "US-ASCII";
		EDIStreamReader reader = factory.createEDIStreamReader(stream, encoding);
		Assert.assertNotNull("Reader was null", reader);
	}

	@Test
	public void testCreateEDIStreamReaderValidated()
			throws EDIStreamException, EDISchemaException {

		EDIInputFactory factory = EDIInputFactory.newFactory();
		InputStream stream = getClass().getClassLoader().getResourceAsStream("x12/simple997.edi");
		SchemaFactory schemaFactory = SchemaFactory.newFactory();
		Schema schema = schemaFactory.createSchema(getClass().getClassLoader().getResource("x12/EDISchema997.xml"));
		EDIStreamReader reader = factory.createEDIStreamReader(stream, schema);
		Assert.assertNotNull("Reader was null", reader);
	}

	@Test
	public void testCreateEDIStreamReaderEncodedValidated()
			throws EDIStreamException, EDISchemaException {

		EDIInputFactory factory = EDIInputFactory.newFactory();
		InputStream stream = getClass().getClassLoader().getResourceAsStream("x12/simple997.edi");
		String encoding = "US-ASCII";
		SchemaFactory schemaFactory = SchemaFactory.newFactory();
		Schema schema = schemaFactory.createSchema(getClass().getClassLoader().getResource("x12/EDISchema997.xml"));
		EDIStreamReader reader = factory.createEDIStreamReader(stream, encoding, schema);
		Assert.assertNotNull("Reader was null", reader);
	}

	@Test
	public void testCreateFilteredReader() throws EDIStreamException {
		EDIInputFactory factory = EDIInputFactory.newFactory();
		EDIStreamReader reader = null;

		reader = factory.createFilteredReader(reader, new EDIStreamFilter() {
			@Override
			public boolean accept(EDIStreamReader r) {
				return false;
			}
		});

		Assert.assertNotNull("Reader was null", reader);
	}

	/*@Test
	public void testIsPropertySupported() {
		EDIInputFactory factory = EDIInputFactory.newFactory();
		Assert.assertTrue(
				"Reporter property not supported",
				factory.isPropertySupported(EDIInputFactory.REPORTER));
	}*/

	@Test(expected = IllegalArgumentException.class)
	public void testGetPropertyUnsupported() {
		EDIInputFactory factory = EDIInputFactory.newFactory();
		factory.getProperty("FOO");
	}

	/*@Test
	public void testGetPropertySupported() {
		EDIInputFactory factory = EDIInputFactory.newFactory();
		Object property = factory.getProperty(EDIInputFactory.REPORTER);
		Assert.assertNull("Reporter property was not supported", property);
	}*/

	@Test(expected = IllegalArgumentException.class)
	public void testSetPropertyUnsupported() {
		EDIInputFactory factory = EDIInputFactory.newFactory();
		factory.setProperty("FOO", null);
	}

	/*@Test
	public void testSetPropertySupported() {
		EDIInputFactory factory = EDIInputFactory.newFactory();
		factory.setProperty(EDIInputFactory.REPORTER, null);
	}*/
}
