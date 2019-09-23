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
package io.xlate.edi.internal.stream;

import java.io.OutputStream;

import org.junit.Assert;
import org.junit.Test;

import io.xlate.edi.internal.stream.StaEDIOutputFactory;
import io.xlate.edi.stream.EDIOutputFactory;
import io.xlate.edi.stream.EDIStreamConstants;
import io.xlate.edi.stream.EDIStreamException;
import io.xlate.edi.stream.EDIStreamWriter;

public class StaEDIOutputFactoryTest {

	@Test
	public void testNewFactory() {
		EDIOutputFactory factory = EDIOutputFactory.newFactory();
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
