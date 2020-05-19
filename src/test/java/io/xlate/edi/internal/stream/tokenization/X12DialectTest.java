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
package io.xlate.edi.internal.stream.tokenization;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import org.junit.jupiter.api.Test;

class X12DialectTest {

    @Test
    void testX12Dialect() throws EDIException {
        Dialect x12 = DialectFactory.getDialect("ISA".toCharArray(), 0, 3);
        assertTrue(x12 instanceof X12Dialect, "Incorrect type");
    }

    @Test
    void testGetEnvelopeTag() throws EDIException {
        Dialect x12 = DialectFactory.getDialect("ISA".toCharArray(), 0, 3);
        assertEquals("ISA", x12.getHeaderTag(), "Incorrect header tag");
    }

    @Test
    void testInitialize() {
        try {
            X12Dialect x12 = (X12Dialect) DialectFactory.getDialect("ISA".toCharArray(), 0, 3);
            x12.header = "ISA*00*          *00*          *ZZ*ReceiverID     *ZZ*Sender         *050812*1953*^*00501*508121953*0*P*:~".toCharArray();
            x12.initialize(new CharacterSet());
        } catch (Exception e) {
            fail("Unexpected exception: " + e.getMessage());
        }
    }

    @Test
    void testGetVersion() throws EDIException {
        X12Dialect x12 = (X12Dialect) DialectFactory.getDialect("ISA".toCharArray(), 0, 3);
        x12.header = "ISA*00*          *00*          *ZZ*ReceiverID     *ZZ*Sender         *050812*1953*^*00501*508121953*0*P*:~".toCharArray();
        x12.initialize(new CharacterSet());
        assertArrayEquals(new String[] { "00501" }, x12.getVersion(), "Invalid version");
    }

}
