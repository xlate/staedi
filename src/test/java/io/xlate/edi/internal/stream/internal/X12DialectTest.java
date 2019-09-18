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
package io.xlate.edi.internal.stream.internal;

import static org.junit.Assert.fail;

import org.junit.Assert;
import org.junit.Test;

import io.xlate.edi.internal.stream.internal.CharacterSet;
import io.xlate.edi.internal.stream.internal.Dialect;
import io.xlate.edi.internal.stream.internal.DialectFactory;
import io.xlate.edi.internal.stream.internal.EDIException;
import io.xlate.edi.internal.stream.internal.X12Dialect;

public class X12DialectTest {

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
    public void testInitialize() {
        try {
            X12Dialect x12 = (X12Dialect) DialectFactory.getDialect("ISA".toCharArray(), 0, 3);
            x12.header = "ISA*00*          *00*          *ZZ*ReceiverID     *ZZ*Sender         *050812*1953*^*00501*508121953*0*P*:~".toCharArray();
            x12.initialize(new CharacterSet());
        } catch (Exception e) {
            fail("Unexpected exception: " + e.getMessage());
        }
    }

    @Test
    public void testGetVersion() throws EDIException {
        X12Dialect x12 = (X12Dialect) DialectFactory.getDialect("ISA".toCharArray(), 0, 3);
        x12.header = "ISA*00*          *00*          *ZZ*ReceiverID     *ZZ*Sender         *050812*1953*^*00501*508121953*0*P*:~".toCharArray();
        x12.initialize(new CharacterSet());
        Assert.assertArrayEquals("Invalid version", new String[] { "00501" }, x12.getVersion());
    }

}
