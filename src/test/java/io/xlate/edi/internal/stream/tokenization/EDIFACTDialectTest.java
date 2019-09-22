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
package io.xlate.edi.internal.stream.tokenization;

import org.junit.Assert;
import org.junit.Test;

import io.xlate.edi.internal.stream.tokenization.CharacterSet;
import io.xlate.edi.internal.stream.tokenization.Dialect;
import io.xlate.edi.internal.stream.tokenization.DialectFactory;
import io.xlate.edi.internal.stream.tokenization.EDIException;
import io.xlate.edi.internal.stream.tokenization.EDIFACTDialect;

@SuppressWarnings("static-method")
public class EDIFACTDialectTest {

	@Test
	public void testEDIFACTADialect() throws EDIException {
		Dialect edifact = DialectFactory.getDialect("UNA".toCharArray(), 0, 3);
		Assert.assertTrue("Incorrect type", edifact instanceof EDIFACTDialect);
	}

	@Test
	public void testEDIFACTBDialect() throws EDIException {
		Dialect edifact = DialectFactory.getDialect("UNB".toCharArray(), 0, 3);
		Assert.assertTrue("Incorrect type", edifact instanceof EDIFACTDialect);
	}

	@Test
	public void testGetEnvelopeTagA() throws EDIException {
		Dialect edifact = DialectFactory.getDialect("UNA".toCharArray(), 0, 3);
		Assert.assertEquals("Incorrect header tag", "UNA", edifact.getHeaderTag());
	}

	@Test
	public void testGetEnvelopeTagB() throws EDIException {
		Dialect edifact = DialectFactory.getDialect("UNB".toCharArray(), 0, 3);
		Assert.assertEquals("Incorrect header tag", "UNB", edifact.getHeaderTag());
	}

	@Test
	public void testGetVersionA() throws EDIException {
		EDIFACTDialect edifact = (EDIFACTDialect) DialectFactory.getDialect("UNA".toCharArray(), 0, 3);
		edifact.header = new StringBuilder("UNA:+.?*'          UNB+UNOA:1+005435656:1+006415160:1+060515:1434+00000000000778'");
		CharacterSet characters = new CharacterSet();
		edifact.initialize(characters);
		Assert.assertArrayEquals("Invalid version", new String[] { "UNOA", "1" }, edifact.getVersion());
	}

	@Test
	public void testGetVersionB() throws EDIException {
		EDIFACTDialect edifact = (EDIFACTDialect) DialectFactory.getDialect("UNB".toCharArray(), 0, 3);
		edifact.header = new StringBuilder("UNB+UNOA:1+005435656:1+006415160:1+060515:1434+00000000000778'");
		CharacterSet characters = new CharacterSet();
		edifact.initialize(characters);
		Assert.assertArrayEquals("Invalid version", new String[] { "UNOA", "1" }, edifact.getVersion());
	}

}
