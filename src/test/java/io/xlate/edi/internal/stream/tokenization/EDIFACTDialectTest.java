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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class EDIFACTDialectTest {

    @Test
    void testEDIFACTADialect() throws EDIException {
        Dialect edifact = DialectFactory.getDialect("UNA".toCharArray(), 0, 3);
        assertTrue(edifact instanceof EDIFACTDialect, "Incorrect type");
    }

    @Test
    void testEDIFACTBDialect() throws EDIException {
        Dialect edifact = DialectFactory.getDialect("UNB".toCharArray(), 0, 3);
        assertTrue(edifact instanceof EDIFACTDialect, "Incorrect type");
    }

    @Test
    void testGetEnvelopeTagA() throws EDIException {
        Dialect edifact = DialectFactory.getDialect("UNA".toCharArray(), 0, 3);
        assertEquals("UNA", edifact.getHeaderTag(), "Incorrect header tag");
    }

    @Test
    void testGetEnvelopeTagB() throws EDIException {
        Dialect edifact = DialectFactory.getDialect("UNB".toCharArray(), 0, 3);
        assertEquals("UNB", edifact.getHeaderTag(), "Incorrect header tag");
    }

    @Test
    void testGetVersionA() throws EDIException {
        EDIFACTDialect edifact = (EDIFACTDialect) DialectFactory.getDialect("UNA".toCharArray(), 0, 3);
        CharacterSet characters = new CharacterSet();
        "UNA:+. *'UNB+UNOA:1+111111111:1+222222222:1+200726:1455+1'".chars().forEach(c -> edifact.appendHeader(characters, (char) c));
        edifact.initialize(characters);
        assertArrayEquals(new String[] { "UNOA", "1" }, edifact.getVersion(), "Invalid version");
    }

    @Test
    void testGetVersionB() throws EDIException {
        EDIFACTDialect edifact = (EDIFACTDialect) DialectFactory.getDialect("UNB".toCharArray(), 0, 3);
        edifact.header = new StringBuilder("UNB+UNOA:1+005435656:1+006415160:1+060515:1434+00000000000778'");
        CharacterSet characters = new CharacterSet();
        edifact.initialize(characters);
        assertArrayEquals(new String[] { "UNOA", "1" }, edifact.getVersion(), "Invalid version");
    }

    @Test
    void testBlankReleaseCharPreVersion4() throws EDIException {
        EDIFACTDialect edifact = (EDIFACTDialect) DialectFactory.getDialect("UNA".toCharArray(), 0, 3);
        CharacterSet characters = new CharacterSet();
        "UNA:+. *'UNB+UNOA:1+111111111:1+222222222:1+200726:1455+1'".chars().forEach(c -> edifact.appendHeader(characters, (char) c));

        assertTrue(edifact.initialize(characters));
        assertEquals('\'', edifact.getSegmentTerminator());
        assertEquals('+', edifact.getDataElementSeparator());
        assertEquals(':', edifact.getComponentElementSeparator());
        assertEquals('.', edifact.getDecimalMark());
        assertEquals('\0', edifact.getRepetitionSeparator());
        assertEquals('\0', edifact.getReleaseIndicator());
    }

    @Test
    void testBlankReleaseCharVersion4() throws EDIException {
        EDIFACTDialect edifact = (EDIFACTDialect) DialectFactory.getDialect("UNA".toCharArray(), 0, 3);
        CharacterSet characters = new CharacterSet();
        "UNA:+. *'UNB+UNOA:4+111111111:1+222222222:1+200726:1455+1'".chars().forEach(c -> edifact.appendHeader(characters, (char) c));

        assertTrue(edifact.initialize(characters));
        assertEquals('\'', edifact.getSegmentTerminator());
        assertEquals('+', edifact.getDataElementSeparator());
        assertEquals(':', edifact.getComponentElementSeparator());
        assertEquals('.', edifact.getDecimalMark()); // UNA value ignored, per spec
        assertEquals('*', edifact.getRepetitionSeparator());
        assertEquals(' ', edifact.getReleaseIndicator());
    }

    @Test
    void testDecimalMarkIgnoredVersion4() throws EDIException {
        EDIFACTDialect edifact = (EDIFACTDialect) DialectFactory.getDialect("UNA".toCharArray(), 0, 3);
        CharacterSet characters = new CharacterSet();
        "UNA:+_ *'UNB+UNOA:4+111111111:1+222222222:1+200726:1455+1'".chars().forEach(c -> edifact.appendHeader(characters, (char) c));

        assertTrue(edifact.initialize(characters));
        assertTrue(edifact.isDecimalMark('.'));
        assertTrue(edifact.isDecimalMark(','));
        assertFalse(edifact.isDecimalMark('_'));
    }

    @Test
    void testBlankSegmentTermPreVersion4() throws EDIException {
        EDIFACTDialect edifact = (EDIFACTDialect) DialectFactory.getDialect("UNA".toCharArray(), 0, 3);
        CharacterSet characters = new CharacterSet();
        "UNA:+.\\* UNB+UNOA:3+111111111:1+222222222:1+200726:1455+1 ".chars().forEach(c -> edifact.appendHeader(characters, (char) c));

        assertTrue(edifact.initialize(characters));
        assertEquals(' ', edifact.getSegmentTerminator());
        assertEquals('+', edifact.getDataElementSeparator());
        assertEquals(':', edifact.getComponentElementSeparator());
        assertEquals('.', edifact.getDecimalMark());
        assertEquals('\0', edifact.getRepetitionSeparator());
        assertEquals('\\', edifact.getReleaseIndicator());
    }

    @Test
    void testDecimalMarkUsedPreVersion4() throws EDIException {
        EDIFACTDialect edifact = (EDIFACTDialect) DialectFactory.getDialect("UNA".toCharArray(), 0, 3);
        CharacterSet characters = new CharacterSet();
        "UNA:+,\\* UNB+UNOA:3+111111111:1+222222222:1+200726:1455+1 ".chars().forEach(c -> edifact.appendHeader(characters, (char) c));

        assertTrue(edifact.initialize(characters));
        assertFalse(edifact.isDecimalMark('.'));
        assertTrue(edifact.isDecimalMark(','));
    }

    @Test
    void testBlankVersionUNA() throws EDIException {
        EDIFACTDialect edifact = (EDIFACTDialect) DialectFactory.getDialect("UNA".toCharArray(), 0, 3);
        CharacterSet characters = new CharacterSet();
        "UNA:+.?*'UNB+".chars().forEach(c -> assertTrue(edifact.appendHeader(characters, (char) c)));
        assertFalse(edifact.appendHeader(characters, '+'));
    }

    @Test
    void testBlankVersionUNB() throws EDIException {
        EDIFACTDialect edifact = (EDIFACTDialect) DialectFactory.getDialect("UNB".toCharArray(), 0, 3);
        CharacterSet characters = new CharacterSet();
        "UNB++111111111:1+222222222:1+200726:1455+1".chars().forEach(c -> assertTrue(edifact.appendHeader(characters, (char) c)));
        assertFalse(edifact.appendHeader(characters, '\''));
    }

    @Test
    void testInvalidState_HeaderUBN() throws EDIException {
        EDIFACTDialect edifact = (EDIFACTDialect) DialectFactory.getDialect("UNA".toCharArray(), 0, 3);
        CharacterSet characters = new CharacterSet();
        "UNA:+.?*'".chars().forEach(c -> edifact.appendHeader(characters, (char) c));
        assertTrue(edifact.appendHeader(characters, 'U'));
        assertFalse(edifact.appendHeader(characters, 'B'));
    }

    @Test
    void testInvalidState_HeaderUNC() throws EDIException {
        EDIFACTDialect edifact = (EDIFACTDialect) DialectFactory.getDialect("UNA".toCharArray(), 0, 3);
        CharacterSet characters = new CharacterSet();
        "UNA:+.?*'".chars().forEach(c -> edifact.appendHeader(characters, (char) c));
        assertTrue(edifact.appendHeader(characters, 'U'));
        assertTrue(edifact.appendHeader(characters, 'N'));
        assertTrue(edifact.appendHeader(characters, 'C'));
        assertFalse(edifact.appendHeader(characters, '+'));
    }
}
