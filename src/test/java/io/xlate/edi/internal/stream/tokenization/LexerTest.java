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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import io.xlate.edi.internal.stream.ConstantsTest;
import io.xlate.edi.internal.stream.StaEDIStreamLocation;
import io.xlate.edi.stream.EDIStreamEvent;
import io.xlate.edi.stream.EDIStreamValidationError;

@SuppressWarnings("resource")
public class LexerTest {

    class TestLexerEventHandler implements EventHandler, ConstantsTest {
        final Map<String, Object> content = new HashMap<>(2);

        @Override
        public void interchangeBegin(Dialect dialect) {
            content.put("LAST", "interchangeBegin");
            content.put("INTERCHANGE_START", true);
        }

        @Override
        public void interchangeEnd() {
            content.put("LAST", "interchangeEnd");
        }

        @Override
        public void loopBegin(CharSequence id) {
            content.put("LAST", "loopBegin");
        }

        @Override
        public void loopEnd(CharSequence id) {
            content.put("LAST", "loopEnd");
        }

        @Override
        public boolean segmentBegin(char[] text, int start, int length) {
            content.put("LAST", "segmentBegin");
            content.put("SEGMENT", new String(text, start, length));
            return true;
        }

        @Override
        public boolean segmentEnd() {
            content.put("LAST", "segmentEnd");
            return true;
        }

        @Override
        public boolean compositeBegin(boolean isNil) {
            content.put("LAST", "compositeBegin");
            return true;
        }

        @Override
        public boolean compositeEnd(boolean isNil) {
            content.put("LAST", "compositeEnd");
            return true;
        }

        @Override
        public boolean elementData(char[] text, int start, int length) {
            content.put("LAST", "elementData");
            content.put("ELEMENT", new String(text, start, length));
            return true;
        }

        @Override
        public boolean binaryData(InputStream binary) {
            return true;
        }

        @Override
        public void segmentError(CharSequence token, EDIStreamValidationError error) {
        }

        @Override
        public void elementError(
                                 EDIStreamEvent event,
                                 EDIStreamValidationError error,
                                 int elem,
                                 int component,
                                 int repetition) {
        }
    }

    EventHandler handler = new EventHandler() {
        @Override
        public void interchangeBegin(Dialect dialect) {
            interchangeStarted = true;
        }

        @Override
        public void interchangeEnd() {
            interchangeEnded = true;
        }

        @Override
        public void loopBegin(CharSequence id) {
        }

        @Override
        public void loopEnd(CharSequence id) {
        }

        @Override
        public boolean segmentBegin(char[] text, int start, int length) {
            segment = new String(text, start, length);
            return true;
        }

        @Override
        public boolean segmentEnd() {
            return true;
        }

        @Override
        public boolean compositeBegin(boolean isNil) {
            compositeStarted = true;
            return true;
        }

        @Override
        public boolean compositeEnd(boolean isNil) {
            compositeEnded = true;
            return true;
        }

        @Override
        public boolean elementData(char[] text, int start, int length) {
            element = new String(text, start, length);
            return true;
        }

        @Override
        public boolean binaryData(InputStream binary) {
            return true;
        }

        @Override
        public void segmentError(CharSequence token, EDIStreamValidationError error) {
        }

        @Override
        public void elementError(
                                 EDIStreamEvent event,
                                 EDIStreamValidationError error,
                                 int elem,
                                 int component,
                                 int repetition) {
        }
    };

    boolean interchangeStarted = false;
    boolean interchangeEnded = false;
    boolean compositeStarted = false;
    boolean compositeEnded = false;
    String segment;
    String element;

    @Test
    public void testParseX12() throws EDIException, IOException {
        InputStream stream = getClass().getResourceAsStream("/x12/simple997.edi");
        interchangeStarted = false;
        interchangeEnded = false;
        compositeStarted = false;
        compositeEnded = false;
        segment = null;
        element = null;

        final StaEDIStreamLocation location = new StaEDIStreamLocation();
        final Lexer lexer = new Lexer(stream, handler, location);

        lexer.parse();
        assertTrue(interchangeStarted, "Interchange not started");
        lexer.parse();
        assertEquals("ISA", segment, "ISA not received");
        lexer.parse();
        assertEquals("00", element, "00 not received");
    }

    @Test
    public void testParseEDIFACT() throws EDIException, IOException {
        InputStream stream = getClass().getResourceAsStream("/EDIFACT/invoic_d97b.edi");
        interchangeStarted = false;
        interchangeEnded = false;
        compositeStarted = false;
        compositeEnded = false;
        segment = null;
        element = null;

        final StaEDIStreamLocation location = new StaEDIStreamLocation();
        final Lexer lexer = new Lexer(stream, handler, location);

        lexer.parse();
        assertTrue(interchangeStarted, "Interchange not started");
        lexer.parse();
        assertEquals("UNB", segment, "UNB not received");
        lexer.parse();
        assertTrue(compositeStarted, "Composite not started");
        lexer.parse();
        assertEquals("UNOA", element, "UNOA not received");
    }

    @Test
    public void testParseTagsX12() throws EDIException, IOException {
        InputStream stream = getClass().getResourceAsStream("/x12/simple997.edi");
        TestLexerEventHandler eventHandler = new TestLexerEventHandler();
        final StaEDIStreamLocation location = new StaEDIStreamLocation();
        final Lexer lexer = new Lexer(stream, eventHandler, location);
        String last;
        int s = -1;

        do {
            lexer.parse();
            last = (String) eventHandler.content.get("LAST");

            if ("segmentBegin".equals(last)) {
                String tag = (String) eventHandler.content.get("SEGMENT");

                if (++s < ConstantsTest.simple997tags.length) {
                    assertEquals(ConstantsTest.simple997tags[s], tag, "Unexpected segment");
                } else {
                    fail("Unexpected segment: " + tag);
                }

            }
        } while (!"interchangeEnd".equals(last));

        assertTrue(s > 0, "No events");
    }

    @Test
    public void testParseTagsEDIFACTA() throws EDIException, IOException {
        InputStream stream = getClass().getResourceAsStream("/EDIFACT/invoic_d97b_una.edi");
        TestLexerEventHandler eventHandler = new TestLexerEventHandler();
        final StaEDIStreamLocation location = new StaEDIStreamLocation();
        final Lexer lexer = new Lexer(stream, eventHandler, location);
        String last;
        int s = -1;

        do {
            lexer.parse();
            last = (String) eventHandler.content.get("LAST");

            if ("segmentBegin".equals(last)) {
                String tag = (String) eventHandler.content.get("SEGMENT");

                if (++s < ConstantsTest.invoic_d97b_unatags.length) {
                    assertEquals(ConstantsTest.invoic_d97b_unatags[s], tag, "Unexpected segment");
                } else {
                    fail("Unexpected segment: " + tag);
                }
            }
        } while (!"interchangeEnd".equals(last));

        assertTrue(s > 0, "No events");
    }

    @Test
    public void testParseTagsEDIFACTB() throws EDIException, IOException {
        InputStream stream = getClass().getResourceAsStream("/EDIFACT/invoic_d97b.edi");
        TestLexerEventHandler eventHandler = new TestLexerEventHandler();
        final StaEDIStreamLocation location = new StaEDIStreamLocation();
        final Lexer lexer = new Lexer(stream, eventHandler, location);
        String last;
        int s = -1;

        do {
            lexer.parse();
            last = (String) eventHandler.content.get("LAST");

            if ("segmentBegin".equals(last)) {
                String tag = (String) eventHandler.content.get("SEGMENT");

                if (++s < ConstantsTest.invoic_d97btags.length) {
                    assertEquals(ConstantsTest.invoic_d97btags[s], tag, "Unexpected segment");
                } else {
                    fail("Unexpected segment: " + tag);
                }
            }
        } while (!"interchangeEnd".equals(last));

        assertTrue(s > 0, "No events");
    }
}
