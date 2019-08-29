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
package io.xlate.edi.stream;

import io.xlate.edi.stream.EDIStreamConstants.Delimiters;
import io.xlate.edi.stream.internal.CharacterClass;
import io.xlate.edi.stream.internal.CharacterSet;
import io.xlate.edi.stream.internal.Dialect;
import io.xlate.edi.stream.internal.DialectFactory;
import io.xlate.edi.stream.internal.EDIException;
import io.xlate.edi.stream.internal.EDIFACTDialect;
import io.xlate.edi.stream.internal.State;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

public class StaEDIStreamWriter implements EDIStreamWriter {

	private static final int LEVEL_INITIAL = 0;
	private static final int LEVEL_INTERCHANGE = 1;
	private static final int LEVEL_SEGMENT = 2;
	private static final int LEVEL_ELEMENT = 3;
	private static final int LEVEL_COMPOSITE = 4;
	private static final int LEVEL_COMPONENT = 5;

	private int level;

	private State state = State.INITIAL;
	private CharacterSet characters = new CharacterSet();

	private final OutputStream stream;
	@SuppressWarnings("unused")
	private final String encoding;
	private final Map<String, Object> properties;
	private Dialect dialect;

	private char segmentTerminator;
	private char dataElementSeparator;
	private char componentElementSeparator;
	private char releaseIndicator;
	private char repetitionSeparator;

	private final boolean prettyPrint;
	private final String lineSeparator;

	public StaEDIStreamWriter(OutputStream stream, Map<String, Object> properties) {
		this(stream, "US-ASCII", properties);
	}

	public StaEDIStreamWriter(OutputStream stream, String encoding, Map<String, Object> properties) {
		this.stream = stream;
		this.encoding = encoding;
		this.properties = new HashMap<>(properties);
		this.prettyPrint = property(StaEDIOutputFactory.PRETTY_PRINT);

		if (prettyPrint) {
			lineSeparator = System.getProperty("line.separator");
		} else {
			lineSeparator = null;
		}
	}

	@SuppressWarnings("unchecked")
	private <T> T property(String key) {
		return (T) properties.get(key);
	}

	private void setupDelimiters() {
		if (properties.containsKey(Delimiters.SEGMENT)) {
			segmentTerminator = property(Delimiters.SEGMENT);
		} else {
			segmentTerminator = dialect.getSegmentTerminator();
		}

		if (properties.containsKey(Delimiters.DATA_ELEMENT)) {
			dataElementSeparator = property(Delimiters.DATA_ELEMENT);
		} else {
			dataElementSeparator = dialect.getDataElementSeparator();
		}

		if (properties.containsKey(Delimiters.COMPONENT_ELEMENT)) {
			componentElementSeparator = property(Delimiters.COMPONENT_ELEMENT);
		} else {
			componentElementSeparator = dialect.getComponentElementSeparator();
		}

		if (properties.containsKey(Delimiters.REPETITION)) {
			repetitionSeparator = property(Delimiters.REPETITION);
		} else {
			repetitionSeparator = dialect.getRepetitionSeparator();
		}

		if (properties.containsKey(Delimiters.RELEASE)) {
			releaseIndicator = property(Delimiters.RELEASE);
		} else {
			releaseIndicator = dialect.getReleaseIndicator();
		}
	}

	private static void ensureArgs(int arrayLength, int start, int end) {
		if (start < 0 || start > arrayLength) {
			throw new IndexOutOfBoundsException();
		}

		if (end > arrayLength) {
			throw new IndexOutOfBoundsException();
		}

		if (end < start) {
			throw new IllegalArgumentException();
		}
	}

	private void ensureState(State s) {
		if (this.state != s) {
			throw new IllegalStateException();
		}
	}

	private void ensureLevel(int l) {
		if (this.level != l) {
			throw new IllegalStateException();
		}
	}

	private void ensureLevelAtLeast(int l) {
		if (this.level < l) {
			throw new IllegalStateException();
		}
	}

	private void ensureLevelBetween(int min, int max) {
		if (this.level < min || this.level > max) {
			throw new IllegalStateException();
		}
	}

	@Override
	public Object getProperty(String name) {
		if (name == null) {
			throw new IllegalArgumentException("Name must not be null");
		}
		return properties.get(name);
	}

	@Override
	public void close() throws EDIStreamException {}

	@Override
	public void flush() throws EDIStreamException {
		try {
			stream.flush();
		} catch (IOException e) {
			throw new EDIStreamException(e);
		}
	}

	private void write(int output) throws EDIStreamException {
		CharacterClass clazz;

		clazz = characters.getClass(output);

		if (clazz == CharacterClass.INVALID) {
			StringBuilder message = new StringBuilder();
			message.append("Invalid character: 0x");
			message.append(Integer.toHexString(output));
			// FIXME
			throw new EDIException(message.toString());
		}

		state = state.transition(clazz);

		switch (state) {
		case HEADER_TAG_I:
		case HEADER_TAG_S:
		case HEADER_TAG_U:
		case HEADER_TAG_N:
		//case HEADER_TAG_A:
		case INTERCHANGE_CANDIDATE:
		case HEADER_DATA:
		case HEADER_ELEMENT_END:
		case HEADER_COMPONENT_END:
			if (dialect.appendHeader(characters, (char) output)) {
				if (dialect.isConfirmed()) {
					switch (state) {
					case HEADER_DATA:
						state = State.TAG_SEARCH;
						break;
					case HEADER_ELEMENT_END:
						state = State.ELEMENT_END;
						break;
					case HEADER_COMPONENT_END:
						state = State.COMPONENT_END;
						break;
					default:
						throw new IllegalStateException("Confirmed at state " + state);
					}
				}
			} else {
				throw new EDIException("Unexpected header character: '" + (char) output + "'");
			}
			break;
		case INVALID:
			StringBuilder message = new StringBuilder();
			message.append("Invalid state: ");
			message.append(state);
			message.append("; output: '");
			message.append((char) output);
			message.append('\'');
			// FIXME
			throw new EDIException(message.toString());
		default:
			break;
		}

		try {
			stream.write(output);
		} catch (IOException e) {
			throw new EDIStreamException(e);
		}
	}

	@Override
	public EDIStreamWriter startInterchange() throws EDIStreamException {
		ensureLevel(LEVEL_INITIAL);
		ensureState(State.INITIAL);
		level = LEVEL_INTERCHANGE;
		return this;
	}

	@Override
	public EDIStreamWriter endInterchange() throws EDIStreamException {

		ensureLevel(LEVEL_INTERCHANGE);
		level = LEVEL_INITIAL;
		return this;
	}

	@Override
	public EDIStreamWriter writeStartSegment(String name) throws EDIStreamException {

		ensureLevel(LEVEL_INTERCHANGE);

		if (state == State.INITIAL) {
			dialect = DialectFactory.getDialect(name);
			setupDelimiters();
			writeString(name);

			if (dialect instanceof EDIFACTDialect && "UNA".equals(name)) {
				write(this.componentElementSeparator);
				write(this.dataElementSeparator);
				write('.'); //FIXME make dynamic
				write(this.releaseIndicator);
				write(this.repetitionSeparator);
			}
		} else {
			writeString(name);
		}

		level = LEVEL_SEGMENT;
		return this;
	}

	private void writeString(String value) throws EDIStreamException {
		for (int i = 0, m = value.length(); i < m; i++) {
			write(value.charAt(i));
		}
	}

	@Override
	public EDIStreamWriter writeEndSegment() throws EDIStreamException {
		ensureLevelAtLeast(LEVEL_SEGMENT);

		if (state == State.ELEMENT_DATA_BINARY) {
			state = State.ELEMENT_END_BINARY;
		}

		write(this.segmentTerminator);
		if (prettyPrint) {
			writeString(lineSeparator);
		}
		level = LEVEL_INTERCHANGE;
		return this;
	}

	@Override
	public EDIStreamWriter writeStartElement() throws EDIStreamException {
		ensureLevel(LEVEL_SEGMENT);
		write(this.dataElementSeparator);
		level = LEVEL_ELEMENT;
		return this;
	}

	@Override
	public EDIStreamWriter writeStartElementBinary() throws EDIStreamException {
		writeStartElement();
		state = State.ELEMENT_DATA_BINARY;
		return this;
	}

	@Override
	public EDIStreamWriter endElement() throws EDIStreamException {
		ensureLevelAtLeast(LEVEL_ELEMENT);
		level = LEVEL_SEGMENT;

		if (state == State.ELEMENT_DATA_BINARY) {
			state = State.ELEMENT_END_BINARY;
		}

		return this;
	}

	@Override
	public EDIStreamWriter startComponent() throws EDIStreamException {
		ensureLevelBetween(LEVEL_ELEMENT, LEVEL_COMPOSITE);

		if (state == State.ELEMENT_DATA_BINARY) {
			throw new IllegalStateException();
		}

		if (LEVEL_COMPOSITE == level) {
			write(this.componentElementSeparator);
		}

		level = LEVEL_COMPONENT;
		return this;
	}

	@Override
	public EDIStreamWriter endComponent() throws EDIStreamException {
		ensureLevel(LEVEL_COMPONENT);
		level = LEVEL_COMPOSITE;
		return this;
	}

	@Override
	public EDIStreamWriter writeRepeatElement() throws EDIStreamException {
		ensureLevelAtLeast(LEVEL_SEGMENT);
		write(this.repetitionSeparator);
		level = LEVEL_ELEMENT;
		return this;
	}

	@Override
	public EDIStreamWriter writeElement(CharSequence text) throws EDIStreamException {
		writeStartElement();
		writeElementData(text);
		endElement();
		return this;
	}

	@Override
	public EDIStreamWriter writeElement(char[] text, int start, int end) throws EDIStreamException {
		writeStartElement();
		writeElementData(text, start, end);
		endElement();
		return this;
	}

	@Override
	public EDIStreamWriter writeEmptyElement() throws EDIStreamException {
		writeStartElement();
		endElement();
		return this;
	}

	@Override
	public EDIStreamWriter writeComponent(CharSequence text)
			throws EDIStreamException {
		startComponent();
		writeElementData(text);
		endComponent();
		return this;
	}

	@Override
	public EDIStreamWriter writeComponent(char[] text, int start, int end)
			throws EDIStreamException {
		startComponent();
		writeElementData(text, start, end);
		endComponent();
		return this;
	}

	@Override
	public EDIStreamWriter writeEmptyComponent() throws EDIStreamException {
		startComponent();
		endComponent();
		return this;
	}

	@Override
	public EDIStreamWriter writeElementData(CharSequence text)
			throws EDIStreamException {
		ensureLevelAtLeast(LEVEL_ELEMENT);
		for (int i = 0, m = text.length(); i < m; i++) {
			char curr = text.charAt(i);
			if (characters.isDelimiter(curr)) {
				throw new IllegalArgumentException("Value contains separator");
			}
			write(curr);
		}
		return this;
	}

	@Override
	public EDIStreamWriter writeElementData(char[] text, int start, int end)
			throws EDIStreamException {

		ensureLevelAtLeast(LEVEL_ELEMENT);
		ensureArgs(text.length, start, end);

		for (int i = 0, m = text.length; i < m; i++) {
			char curr = text[i];
			if (characters.isDelimiter(curr)) {
				throw new IllegalArgumentException("Value contains separator");
			}
			write(curr);
		}

		return this;
	}

	@Override
	public EDIStreamWriter writeBinaryData(InputStream binaryStream)
			throws EDIStreamException {
		ensureLevel(LEVEL_ELEMENT);
		ensureState(State.ELEMENT_DATA_BINARY);
		int input;

		try {
			while ((input = binaryStream.read()) != -1) {
			    stream.write(input);
			}
		} catch (IOException e) {
			throw new EDIStreamException(e);
		}

		return this;
	}

	@Override
	public EDIStreamWriter writeBinaryData(byte[] binary, int start, int end)
			throws EDIStreamException {

		ensureLevel(LEVEL_ELEMENT);
		ensureState(State.ELEMENT_DATA_BINARY);
		ensureArgs(binary.length, start, end);

		for (int i = start; i < end; i++) {
			write(binary[i]);
		}

		return this;
	}

	@Override
	public EDIStreamWriter writeBinaryData(ByteBuffer binary)
			throws EDIStreamException {

		ensureLevel(LEVEL_ELEMENT);
		ensureState(State.ELEMENT_DATA_BINARY);

		while (binary.hasRemaining()) {
			write(binary.get());
		}

		return this;
	}
}
