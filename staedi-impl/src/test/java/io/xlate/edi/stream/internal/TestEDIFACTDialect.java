package io.xlate.edi.stream.internal;

import org.junit.Assert;
import org.junit.Test;

@SuppressWarnings("static-method")
public class TestEDIFACTDialect {

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
		Assert.assertEquals("Invalid version", "10000", edifact.getVersion());
	}

	@Test
	public void testGetVersionB() throws EDIException {
		EDIFACTDialect edifact = (EDIFACTDialect) DialectFactory.getDialect("UNB".toCharArray(), 0, 3);
		edifact.header = new StringBuilder("UNB+UNOA:1+005435656:1+006415160:1+060515:1434+00000000000778'");
		CharacterSet characters = new CharacterSet();
		edifact.initialize(characters);
		Assert.assertEquals("Invalid version", "10000", edifact.getVersion());
	}

}
