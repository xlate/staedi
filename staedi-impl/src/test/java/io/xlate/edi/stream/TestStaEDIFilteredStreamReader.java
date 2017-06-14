package io.xlate.edi.stream;

import io.xlate.edi.stream.EDIStreamConstants.Events;

import java.io.IOException;
import java.io.InputStream;
import java.util.NoSuchElementException;

import org.junit.Assert;
import org.junit.Test;

@SuppressWarnings("resource")
public class TestStaEDIFilteredStreamReader implements TestConstants {

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
				if (reader.getEventType() != Events.ELEMENT_DATA) {
					return false;
				}
				Location location = reader.getLocation();
				return location.getComponentPosition() > 1 ||
						location.getElementOccurrence() > 1;
			}
		};
		EDIStreamReader reader = factory.createEDIStreamReader(stream);
		reader = factory.createFilteredReader(reader, filter);

		int event;
		int matches = 0;

		while (reader.hasNext()) {
			event = reader.next();

			if (event != Events.ELEMENT_DATA) {
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
				if (reader.getEventType() != Events.START_SEGMENT) {
					return false;
				}
				String tag = reader.getText();
				return tag.matches("^.{0,2}[SG5].{0,2}$");
			}
		};
		EDIStreamReader reader = factory.createEDIStreamReader(stream);
		reader = factory.createFilteredReader(reader, filter);

		int event;
		int matches = 0;
		String tag = null;

		while (reader.hasNext()) {
			try {
				event = reader.nextTag();
			} catch (@SuppressWarnings("unused") NoSuchElementException e) {
				break;
			}

			if (event != Events.START_SEGMENT) {
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
				if (reader.getEventType() != Events.ELEMENT_DATA) {
					return false;
				}
				return reader.getTextLength() == 1;
			}
		};
		EDIStreamReader reader = factory.createEDIStreamReader(stream);
		reader = factory.createFilteredReader(reader, filter);

		int event;
		int matches = 0;

		while (reader.hasNext()) {
			event = reader.next();

			if (event != Events.ELEMENT_DATA) {
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
