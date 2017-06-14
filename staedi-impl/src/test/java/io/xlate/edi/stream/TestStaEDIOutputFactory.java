package io.xlate.edi.stream;

import java.io.OutputStream;

import org.junit.Assert;
import org.junit.Test;

@SuppressWarnings("static-method")
public class TestStaEDIOutputFactory {

	@Test
	public void testNewFactory() {
		EDIOutputFactory factory = EDIOutputFactory.newFactory();
		Assert.assertTrue(factory instanceof StaEDIOutputFactory);
	}

	@Test
	public void testNewFactoryById() {
		String factoryId = StaEDIOutputFactory.class.getName();
		ClassLoader loader = null;// Thread.currentThread().getContextClassLoader();
		EDIOutputFactory factory = EDIOutputFactory.newFactory(factoryId, loader);
		Assert.assertTrue(factory instanceof StaEDIOutputFactory);
	}

	@Test
	public void testCreateEDIStreamWriterOutputStream() throws EDIStreamException {
		EDIOutputFactory factory = EDIOutputFactory.newFactory();
		OutputStream stream = System.out;
		EDIStreamWriter writer = factory.createEDIStreamWriter(stream);
		Assert.assertNotNull("Writer was null", writer);
	}

	@Test
	public void testCreateEDIStreamWriterOutputStreamString() throws EDIStreamException {
		EDIOutputFactory factory = EDIOutputFactory.newFactory();
		OutputStream stream = System.out;
		String encoding = "US-ASCII";
		EDIStreamWriter writer = factory.createEDIStreamWriter(stream, encoding);
		Assert.assertNotNull("Writer was null", writer);
	}

	@Test
	public void testIsPropertySupported() {
		EDIOutputFactory factory = EDIOutputFactory.newFactory();
		Assert.assertTrue("FOO supported", !factory.isPropertySupported("FOO"));
	}

	@Test(expected = IllegalArgumentException.class)
	public void testGetPropertyUnsupported() {
		EDIOutputFactory factory = EDIOutputFactory.newFactory();
		factory.getProperty("FOO");
	}

	@Test()
	public void testGetPropertySupported() {
		EDIOutputFactory factory = EDIOutputFactory.newFactory();
		Assert.assertNull(factory.getProperty(EDIStreamConstants.Delimiters.DATA_ELEMENT));
	}

	@Test(expected = IllegalArgumentException.class)
	public void testSetPropertyUnsupported() {
		EDIOutputFactory factory = EDIOutputFactory.newFactory();
		factory.setProperty("FOO", null);
	}

	@Test
	public void testSetPropertySupported() {
		EDIOutputFactory factory = EDIOutputFactory.newFactory();
		factory.setProperty(EDIStreamConstants.Delimiters.REPETITION, '`');
		Assert.assertEquals('`', factory.getProperty(EDIStreamConstants.Delimiters.REPETITION));
	}
}
