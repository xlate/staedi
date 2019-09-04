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

import java.io.IOException;
import java.io.InputStream;
import java.util.NoSuchElementException;

import org.junit.Assert;
import org.junit.Test;

import io.xlate.edi.stream.EDIInputFactory;
import io.xlate.edi.stream.EDIStreamEvent;
import io.xlate.edi.stream.EDIStreamException;
import io.xlate.edi.stream.EDIStreamFilter;
import io.xlate.edi.stream.EDIStreamReader;
import io.xlate.edi.stream.Location;

@SuppressWarnings("resource")
public class StaEDIFilteredStreamReaderTest implements ConstantsTest {

	@Test
	/**
	 * Filter all except repeat > 1 of an element or component elements where
	 * the position within the composite > 1.
	 *
	 * @throws EDIStreamException
	 */
	public void testNext() throws EDIStreamException {
		EDIInputFactory factory = EDIInputFactory.newFactory();
		InputStream stream = getClass().getClassLoader().getResourceAsStream(
				"x12/extraDelimiter997.edi");
		EDIStreamFilter filter = new EDIStreamFilter() {
			@Override
			public boolean accept(EDIStreamReader reader) {
				if (reader.getEventType() != EDIStreamEvent.ELEMENT_DATA) {
					return false;
				}
				Location location = reader.getLocation();
				return location.getComponentPosition() > 1 ||
						location.getElementOccurrence() > 1;
			}
		};
		EDIStreamReader reader = factory.createEDIStreamReader(stream);
		reader = factory.createFilteredReader(reader, filter);

		EDIStreamEvent event;
		int matches = 0;

		while (reader.hasNext()) {
			event = reader.next();

			if (event != EDIStreamEvent.ELEMENT_DATA) {
				Assert.fail("Unexpected event: " + event);
			}

			String text = reader.getText();
			Assert.assertTrue("Not matched: " + text, text.matches(".*(R[2-9]|COMP[2-9]).*"));
			matches++;
		}

		Assert.assertEquals(9, matches);
	}

	@Test
	/**
	 * Only allow segment tags containing S, G, or 5 to pass the filter.
	 * @throws EDIStreamException
	 */
	public void testNextTag() throws EDIStreamException {
		EDIInputFactory factory = EDIInputFactory.newFactory();
		InputStream stream = getClass().getClassLoader().getResourceAsStream(
				"x12/simple997.edi");
		EDIStreamFilter filter = new EDIStreamFilter() {
			@Override
			public boolean accept(EDIStreamReader reader) {
				if (reader.getEventType() != EDIStreamEvent.START_SEGMENT) {
					return false;
				}
				String tag = reader.getText();
				return tag.matches("^.{0,2}[SG5].{0,2}$");
			}
		};
		EDIStreamReader reader = factory.createEDIStreamReader(stream);
		reader = factory.createFilteredReader(reader, filter);

		EDIStreamEvent event;
		int matches = 0;
		String tag = null;

		while (reader.hasNext()) {
			try {
				event = reader.nextTag();
			} catch (@SuppressWarnings("unused") NoSuchElementException e) {
				break;
			}

			if (event != EDIStreamEvent.START_SEGMENT) {
				Assert.fail("Unexpected event: " + event);
			}

			tag = reader.getText();
			Assert.assertTrue(
					tag.indexOf('S') > -1 ||
					tag.indexOf('G') > -1 ||
					tag.indexOf('5') > -1);
			matches++;
		}

		Assert.assertEquals("Unexpected last segment", "GE", tag);
		Assert.assertEquals(6, matches);
	}

	@Test
	/**
	 * Filter all except single character element events
	 * @throws EDIStreamException
	 */
	public void testHasNext() throws EDIStreamException, IOException {
		EDIInputFactory factory = EDIInputFactory.newFactory();
		InputStream stream = getClass().getClassLoader().getResourceAsStream(
				"x12/extraDelimiter997.edi");
		EDIStreamFilter filter = new EDIStreamFilter() {
			@Override
			public boolean accept(EDIStreamReader reader) {
				if (reader.getEventType() != EDIStreamEvent.ELEMENT_DATA) {
					return false;
				}
				return reader.getTextLength() == 1;
			}
		};
		EDIStreamReader reader = factory.createEDIStreamReader(stream);
		reader = factory.createFilteredReader(reader, filter);

		EDIStreamEvent event;
		int matches = 0;

		while (reader.hasNext()) {
			event = reader.next();

			if (event != EDIStreamEvent.ELEMENT_DATA) {
				Assert.fail("Unexpected event: " + event);
			}

			String text = reader.getText();
			Assert.assertTrue("Wrong length: " + text, text.length() == 1);
			matches++;
		}

		reader.close();

		Assert.assertEquals(16, matches);
	}

}
