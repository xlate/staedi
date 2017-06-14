package io.xlate.edi.stream.internal;

import org.junit.Assert;
import org.junit.Test;

@SuppressWarnings("static-method")
public class TestX12Dialect {

	@Test
	public void testX12Dialect() throws EDIException {
		Dialect x12 = DialectFactory.getDialect("ISA".toCharArray(), 0, 3);
		Assert.assertTrue("Incorrect type", x12 instanceof X12Dialect);
	}

	@Test
	public void testGetEnvelopeTag() throws EDIException {
		Dialect x12 = DialectFactory.getDialect("ISA".toCharArray(), 0, 3);
		Assert.assertEquals("Incorrect header tag", "ISA", x12.getHeaderTag());
	}

	@Test
	public void testInitialize() throws EDIException {
		X12Dialect x12 = (X12Dialect) DialectFactory.getDialect("ISA".toCharArray(), 0, 3);
		x12.header = "ISA*00*          *00*          *ZZ*ReceiverID     *ZZ*Sender         *050812*1953*^*00501*508121953*0*P*:~".toCharArray();
		x12.initialize(new CharacterSet());
	}

	@Test
	public void testGetVersion() throws EDIException {
		X12Dialect x12 = (X12Dialect) DialectFactory.getDialect("ISA".toCharArray(), 0, 3);
		x12.header = "ISA*00*          *00*          *ZZ*ReceiverID     *ZZ*Sender         *050812*1953*^*00501*508121953*0*P*:~".toCharArray();
		x12.initialize(new CharacterSet());
		Assert.assertEquals("Invalid version", "00501", x12.getVersion());
	}

}
