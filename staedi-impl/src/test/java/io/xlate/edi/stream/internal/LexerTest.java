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
package io.xlate.edi.stream.internal;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

import io.xlate.edi.stream.ConstantsTest;

public class LexerTest {

	class TestLexerEventHandler implements EventHandler, ConstantsTest {
		final Map<String, Object> content = new HashMap<>(2);

		@Override
		public void interchangeBegin() {
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
		public void segmentBegin(char[] text, int start, int length) {
			content.put("LAST", "segmentBegin");
			content.put("SEGMENT", new String(text, start, length));
		}

		@Override
		public void segmentEnd() {
			content.put("LAST", "segmentEnd");
		}

		@Override
		public void compositeBegin(boolean isNil) {
			content.put("LAST", "compositeBegin");
		}

		@Override
		public void compositeEnd(boolean isNil) {
			content.put("LAST", "compositeEnd");
		}

		@Override
		public void elementData(char[] text, int start, int length) {
			content.put("LAST", "elementData");
			content.put("ELEMENT", new String(text, start, length));
		}

		@Override
		public void binaryData(InputStream binary) {}

		@Override
		public void segmentError(CharSequence token, int error) {}

		@Override
		public void elementError(
				int event,
				int error,
				int elem,
				int component,
				int repetition) {}
	}

	EventHandler handler = new EventHandler() {
		@Override
		public void interchangeBegin() {
			interchangeStarted = true;
		}

		@Override
		public void interchangeEnd() {
			interchangeEnded = true;
		}

		@Override
		public void loopBegin(CharSequence id) {}

		@Override
		public void loopEnd(CharSequence id) {}

		@Override
		public void segmentBegin(char[] text, int start, int length) {
			segment = new String(text, start, length);
		}

		@Override
		public void segmentEnd() {}

		@Override
		public void compositeBegin(boolean isNil) {
			compositeStarted = true;
		}

		@Override
		public void compositeEnd(boolean isNil) {
			compositeEnded = true;
		}

		@Override
		public void elementData(char[] text, int start, int length) {
			element = new String(text, start, length);
		}

		@Override
		public void binaryData(InputStream binary) {}

		@Override
		public void segmentError(CharSequence token, int error) {}

		@Override
		public void elementError(
				int event,
				int error,
				int elem,
				int component,
				int repetition) {}
	};

	boolean interchangeStarted = false;
	boolean interchangeEnded = false;
	boolean compositeStarted = false;
	boolean compositeEnded = false;
	String segment;
	String element;

	@Test
	public void testParseX12() throws EDIException, IOException {
		@SuppressWarnings("resource")
		InputStream stream =
				getClass().getClassLoader().getResourceAsStream(
						"x12/simple997.edi");
		interchangeStarted = false;
		interchangeEnded = false;
		compositeStarted = false;
		compositeEnded = false;
		segment = null;
		element = null;

		final InternalLocation location = new InternalLocation();
		final Lexer lexer = new Lexer(stream, handler, location);

		lexer.parse();
		Assert.assertTrue("Interchange not started", interchangeStarted);
		lexer.parse();
		Assert.assertEquals("ISA not received", "ISA", segment);
		lexer.parse();
		Assert.assertEquals("00 not received", "00", element);
	}

	@Test
	public void testParseEDIFACT() throws EDIException, IOException {
		@SuppressWarnings("resource")
		InputStream stream =
				getClass().getClassLoader().getResourceAsStream(
						"EDIFACT/invoic_d97b.edi");
		interchangeStarted = false;
		interchangeEnded = false;
		compositeStarted = false;
		compositeEnded = false;
		segment = null;
		element = null;

		final InternalLocation location = new InternalLocation();
		final Lexer lexer = new Lexer(stream, handler, location);

		lexer.parse();
		Assert.assertTrue("Interchange not started", interchangeStarted);
		lexer.parse();
		Assert.assertEquals("UNB not received", "UNB", segment);
		lexer.parse();
		Assert.assertTrue("Composite not started", compositeStarted);
		lexer.parse();
		Assert.assertEquals("UNOA not received", "UNOA", element);
	}

	@Test
	public void testParseTagsX12() throws EDIException, IOException {
		@SuppressWarnings("resource")
		InputStream stream =
				getClass().getClassLoader().getResourceAsStream(
						"x12/simple997.edi");

		TestLexerEventHandler eventHandler = new TestLexerEventHandler();

		final InternalLocation location = new InternalLocation();
		final Lexer lexer = new Lexer(stream, eventHandler, location);
		String last;
		int s = -1;

		do {
			lexer.parse();
			last = (String) eventHandler.content.get("LAST");

			if ("segmentBegin".equals(last)) {
				String tag = (String) eventHandler.content.get("SEGMENT");

				if (++s < ConstantsTest.simple997tags.length) {
					Assert.assertEquals("Unexpected segment", ConstantsTest.simple997tags[s], tag);
				} else {
					Assert.fail("Unexpected segment: " + tag);
				}


			}
		} while (!"interchangeEnd".equals(last));

		Assert.assertTrue("No events", s > 0);
	}

	@Test
	public void testParseTagsEDIFACTA() throws EDIException, IOException {
		@SuppressWarnings("resource")
		InputStream stream =
				getClass().getClassLoader().getResourceAsStream(
						"EDIFACT/invoic_d97b_una.edi");
		TestLexerEventHandler eventHandler = new TestLexerEventHandler();
		final InternalLocation location = new InternalLocation();
		final Lexer lexer = new Lexer(stream, eventHandler, location);
		String last;
		int s = -1;

		do {
			lexer.parse();
			last = (String) eventHandler.content.get("LAST");

			if ("segmentBegin".equals(last)) {
				String tag = (String) eventHandler.content.get("SEGMENT");

				if (++s < ConstantsTest.invoic_d97b_unatags.length) {
					Assert.assertEquals("Unexpected segment", ConstantsTest.invoic_d97b_unatags[s], tag);
				} else {
					Assert.fail("Unexpected segment: " + tag);
				}
			}
		} while (!"interchangeEnd".equals(last));

		Assert.assertTrue("No events", s > 0);
	}

	@Test
	public void testParseTagsEDIFACTB() throws EDIException, IOException {
		@SuppressWarnings("resource")
		InputStream stream =
				getClass().getClassLoader().getResourceAsStream(
						"EDIFACT/invoic_d97b.edi");
		TestLexerEventHandler eventHandler = new TestLexerEventHandler();
		final InternalLocation location = new InternalLocation();
		final Lexer lexer = new Lexer(stream, eventHandler, location);
		String last;
		int s = -1;

		do {
			lexer.parse();
			last = (String) eventHandler.content.get("LAST");

			if ("segmentBegin".equals(last)) {
				String tag = (String) eventHandler.content.get("SEGMENT");

				if (++s < ConstantsTest.invoic_d97btags.length) {
					Assert.assertEquals("Unexpected segment", ConstantsTest.invoic_d97btags[s], tag);
				} else {
					Assert.fail("Unexpected segment: " + tag);
				}
			}
		} while (!"interchangeEnd".equals(last));

		Assert.assertTrue("No events", s > 0);
	}
}
