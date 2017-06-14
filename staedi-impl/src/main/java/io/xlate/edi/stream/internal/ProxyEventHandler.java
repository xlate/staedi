package io.xlate.edi.stream.internal;

import java.io.InputStream;
import java.nio.CharBuffer;
import java.util.Iterator;
import java.util.List;

import io.xlate.edi.schema.EDISchemaException;
import io.xlate.edi.schema.Schema;
import io.xlate.edi.stream.EDIStreamConstants.ElementOccurrenceErrors;
import io.xlate.edi.stream.EDIStreamConstants.Events;
import io.xlate.edi.stream.Location;
import io.xlate.edi.stream.validation.Validator;

public class ProxyEventHandler implements EventHandler {

	private final InternalLocation location;
	private Validator validator;

	private InputStream binary;
	private CharArraySequence holder = new CharArraySequence();

	private int[] events = new int[99];
	private int[] errorTypes = new int[99];
	private CharBuffer[] eventData = new CharBuffer[99];
	private String[] referenceCodes = new String[99];
	private Location[] locations = new Location[99];
	private int eventCount = 0;
	private int eventIndex = 0;

	public ProxyEventHandler(InternalLocation location, Schema schema) {
		this.location = location;
		setSchema(schema);
	}

	public void setSchema(Schema schema) {
		if (validator != null) {
			throw new IllegalStateException("validator already created");
		}

		validator = schema != null ? new Validator(schema) : null;
	}

	public Schema addSchema(Schema schema) throws EDISchemaException {
		if (validator == null) {
			throw new IllegalStateException("validator not created");
		}

		return validator.addSchema(schema);
	}

	public void resetEvents() {
		eventCount = 0;
		eventIndex = 0;
	}

	public int getEvent() {
		if (hasEvents()) {
			return events[eventIndex];
		}
		return 0;
		//throw new NoSuchElementException();
	}

	public CharBuffer getCharacters() {
		if (hasEvents()) {
			return eventData[eventIndex];
		}
		throw new IllegalStateException();
	}

	public boolean hasEvents() {
		return eventIndex < eventCount;
	}

	public boolean nextEvent() {
		if (eventCount < 1) {
			return false;
		}
		return ++eventIndex < eventCount;
	}

	public int getErrorType() {
		return errorTypes[eventIndex];
	}

	public String getReferenceCode() {
		return referenceCodes[eventIndex];
	}

	public Location getLocation() {
		if (hasEvents()) {
			if (locations[eventIndex] != null) {
				return locations[eventIndex];
			}
		}
		return location;
	}

	public InputStream getBinary() {
		return binary;
	}

	public void setBinary(InputStream binary) {
		this.binary = binary;
	}

	@Override
	public void interchangeBegin() {
		enqueueEvent(Events.START_INTERCHANGE, -1, "", null);
	}

	@Override
	public void interchangeEnd() {
		enqueueEvent(Events.END_INTERCHANGE, -1, "", null);
	}

	@Override
	public void loopBegin(CharSequence id) {
		enqueueEvent(Events.START_LOOP, -1, id, null);
	}

	@Override
	public void loopEnd(CharSequence id) {
		enqueueEvent(Events.END_LOOP, -1, id, null);
	}

	@Override
	public void segmentBegin(char[] text, int start, int length) {
		if (validator != null) {
			holder.array = text;
			holder.start = start;
			holder.length = length;
			validator.validateSegment(this, holder);
		}

		enqueueEvent(Events.START_SEGMENT, -1, text, start, length, null, null);
	}

	@Override
	public void segmentEnd() {
		if (validator != null) {
			validator.validateSyntax(this, location, false);
		}
		enqueueEvent(Events.END_SEGMENT, -1, "", null);
	}

	@Override
	public void compositeBegin(boolean isNil) {
		String code = null;

		if (validator != null && !isNil) {
			boolean invalid = !validator.validCompositeOccurrences(location);

			if (invalid) {
				List<Integer> errors = validator.getElementErrors();

				for (Integer error : errors) {
					enqueueEvent(Events.ELEMENT_OCCURRENCE_ERROR, error, "", null);
				}
			} else {
				code = validator.getCompositeReferenceCode();
			}
		}

		enqueueEvent(Events.START_COMPOSITE, -1, "", code);
	}

	@Override
	public void compositeEnd(boolean isNil) {
		if (validator != null && !isNil) {
			validator.validateSyntax(this, location, true);
		}
		enqueueEvent(Events.END_COMPOSITE, -1, "", null);
	}

	@Override
	public void elementData(char[] text, int start, int length) {
		boolean derivedComposite = false;
		Location savedLocation = null;
		String code = null;

		if (validator != null) {
			final boolean composite = location.getComponentPosition() > -1;

			holder.array = text;
			holder.start = start;
			holder.length = length;
			boolean valid = validator.validateElement(location, holder);
			derivedComposite = !composite && validator.isComposite();

			code = validator.getElementReferenceNumber();

			if (!valid) {
				/*
				 * Process element-level errors before possibly starting a
				 * composite or reporting other data-related errors.
				 */
				List<Integer> errors = validator.getElementErrors();
				Iterator<Integer> cursor = errors.iterator();

				if (derivedComposite) {
					savedLocation = new ImmutableLocation(location);
				}

				while (cursor.hasNext()) {
					int error = cursor.next();

					switch (error) {
					case ElementOccurrenceErrors.TOO_MANY_DATA_ELEMENTS:
					case ElementOccurrenceErrors.TOO_MANY_REPETITIONS:
						enqueueEvent(
								Events.ELEMENT_OCCURRENCE_ERROR,
								error,
								text,
								start,
								length,
								code,
								savedLocation);
						cursor.remove();
						//$FALL-THROUGH$
					default:
						continue;
					}
				}
			}

			if (derivedComposite && text != null/* Not an empty composite */) {
				this.compositeBegin(length == 0);
				location.incrementComponentPosition();
				savedLocation = new ImmutableLocation(location);
			}

			if (!valid) {
				List<Integer> errors = validator.getElementErrors();
				savedLocation = new ImmutableLocation(location);

				for (Integer error : errors) {
					enqueueEvent(
							getErrorEventType(error),
							error,
							text,
							start,
							length,
							code,
							savedLocation);
				}
			}
		}

		if (text != null && (!derivedComposite || length > 0) /* Not an inferred element */) {
			enqueueEvent(Events.ELEMENT_DATA, -1, text, start, length, code, savedLocation);
		}

		if (derivedComposite && text != null /* Not an empty composite */) {
			this.compositeEnd(length == 0);
			location.clearComponentPosition();
		}
	}

	private int getErrorEventType(int error) {
		switch (error) {
		case ElementOccurrenceErrors.REQUIRED_DATA_ELEMENT_MISSING:
		case ElementOccurrenceErrors.CONDITIONAL_REQUIRED_DATA_ELEMENT_MISSING:
		case ElementOccurrenceErrors.TOO_MANY_DATA_ELEMENTS:
		case ElementOccurrenceErrors.EXCLUSION_CONDITION_VIOLATED:
		case ElementOccurrenceErrors.TOO_MANY_REPETITIONS:
		case ElementOccurrenceErrors.TOO_MANY_COMPONENTS:
			return Events.ELEMENT_OCCURRENCE_ERROR;
		default:
			return Events.ELEMENT_DATA_ERROR;
		}
	}

	@Override
	public void binaryData(InputStream binaryStream) {
		enqueueEvent(Events.ELEMENT_DATA_BINARY, -1, "", null);
		setBinary(binaryStream);
	}

	@Override
	public void segmentError(CharSequence token, int error) {
		enqueueEvent(Events.SEGMENT_ERROR, error, token, null);
	}

	@Override
	public void elementError(
			final int event,
			final int error,
			final int element,
			final int component,
			final int repetition) {

		InternalLocation copy = location.clone();
		copy.setElementPosition(element);
		copy.setElementOccurrence(repetition);
		copy.setComponentPosition(component);

		enqueueEvent(event, error, null, 0, 0, null, copy);
	}

	private void enqueueEvent(
			int event,
			int error,
			char[] text,
			int start,
			int length,
			String code,
			Location savedLocation) {

		if (eventCount > 0 && event == Events.ELEMENT_OCCURRENCE_ERROR) {
			if (events[eventCount] == Events.START_COMPOSITE) {
				switch (error) {
				case ElementOccurrenceErrors.TOO_MANY_DATA_ELEMENTS:
				case ElementOccurrenceErrors.TOO_MANY_REPETITIONS:
					/*
					 * We have an element-level error with a pending start
					 * composite event. Move the element error before the start
					 * of the composite.
					 */
					events[eventCount] = events[eventCount - 1];
					eventData[eventCount] = eventData[eventCount - 1];
					errorTypes[eventCount] = errorTypes[eventCount - 1];
					referenceCodes[eventCount] = referenceCodes[eventCount - 1];
					locations[eventCount] = locations[eventCount - 1];

					events[eventCount - 1] = event;
					errorTypes[eventCount - 1] = error;
					eventData[eventCount - 1] = put(eventData[eventCount], text, start, length);
					referenceCodes[eventCount - 1] = code;

					if (savedLocation != null) {
						locations[eventCount - 1] = savedLocation;
					} else {
						locations[eventCount - 1] = null;
					}

					eventCount++;
					return;
				default:
					break;
				}
			}
		}

		events[eventCount] = event;
		errorTypes[eventCount] = error;
		eventData[eventCount] = put(eventData[eventCount], text, start, length);
		referenceCodes[eventCount] = code;

		if (savedLocation != null) {
			locations[eventCount] = savedLocation;
		} else {
			locations[eventCount] = null;
		}

		eventCount++;
	}

	private void enqueueEvent(int event, int error, CharSequence text, String code) {
		events[eventCount] = event;
		errorTypes[eventCount] = error;
		eventData[eventCount] = put(eventData[eventCount], text);
		referenceCodes[eventCount] = code;
		eventCount++;
	}

	private static CharBuffer put(
			CharBuffer buffer,
			char[] text,
			int start,
			int length) {

		if (buffer == null || buffer.capacity() < length) {
			buffer = CharBuffer.allocate(length);
		}

		buffer.clear();

		if (text != null) {
			buffer.put(text, start, length);
		}

		buffer.flip();

		return buffer;
	}

	private static CharBuffer put(CharBuffer buffer, CharSequence text) {
		int length = text.length();

		if (buffer == null || buffer.capacity() < length) {
			buffer = CharBuffer.allocate(length);
		}

		buffer.clear();
		for (int i = 0; i < length; i++) {
			buffer.put(text.charAt(i));
		}
		buffer.flip();

		return buffer;
	}

	private static class CharArraySequence implements CharSequence,
			Comparable<CharSequence> {

		char[] array;
		int start;
		int length;

		@Override
		public int length() {
			return length;
		}

		@Override
		public char charAt(int index) {
			if (index < 0 || index >= length) {
				throw new StringIndexOutOfBoundsException(index);
			}

			return array[start + index];
		}

		@Override
		public CharSequence subSequence(@SuppressWarnings("hiding") int start, int end) {
			if (start < 0) {
				throw new IndexOutOfBoundsException(Integer.toString(start));
			}
			if (end > length) {
				throw new IndexOutOfBoundsException(Integer.toString(end));
			}
			if (start > end) {
				throw new IndexOutOfBoundsException(Integer.toString(end - start));
			}
			return ((start == 0) && (end == length)) ? this : new String(
					array,
					this.start + start,
					end - start);
		}

		@Override
		public int compareTo(CharSequence other) {
			int len1 = length;
			int len2 = other.length();
			int n = Math.min(len1, len2);
			char v1[] = array;
			int i = start;
			int j = 0;

			while (n-- != 0) {
				char c1 = v1[i++];
				char c2 = other.charAt(j++);
				if (c1 != c2) {
					return c1 - c2;
				}
			}

			return len1 - len2;
		}

		@Override
		public String toString() {
			return new String(array, start, length);
		}
	}
}
