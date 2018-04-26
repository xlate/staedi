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
package io.xlate.edi.stream;

import java.io.InputStream;

import org.junit.Assert;
import org.junit.Test;

import io.xlate.edi.schema.EDISchemaException;
import io.xlate.edi.schema.Schema;
import io.xlate.edi.schema.SchemaFactory;

@SuppressWarnings({ "static-method", "resource" })
public class StaEDIInputFactoryTest {

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
