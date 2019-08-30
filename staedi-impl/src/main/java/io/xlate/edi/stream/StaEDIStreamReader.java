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

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.CharBuffer;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;

import io.xlate.edi.schema.EDISchemaException;
import io.xlate.edi.schema.Schema;
import io.xlate.edi.stream.internal.Dialect;
import io.xlate.edi.stream.internal.InternalLocation;
import io.xlate.edi.stream.internal.Lexer;
import io.xlate.edi.stream.internal.ProxyEventHandler;

public class StaEDIStreamReader implements EDIStreamReader {

	private final InputStream stream;
	@SuppressWarnings("unused")
	private final String encoding;
	private Schema schema;
	private final Map<String, Object> properties;
	private final InternalLocation location = new InternalLocation();
	private final ProxyEventHandler proxy;
	private final Lexer lexer;

	private boolean complete = false;
	private boolean closed = false;

	public StaEDIStreamReader(
			InputStream stream,
			String encoding,
			Map<String, Object> properties) {
		this(stream, encoding, null, properties);
	}

	public StaEDIStreamReader(
			InputStream stream,
			String encoding,
			Schema schema,
			Map<String, Object> properties) {
		if (stream instanceof BufferedInputStream) {
			this.stream = stream;
		} else {
			this.stream = new BufferedInputStream(stream);
		}

		this.encoding = encoding;
		this.schema = schema;
		this.properties = new HashMap<>(properties);
		this.proxy = new ProxyEventHandler(location, this.schema);
		this.lexer = new Lexer(this.stream, proxy, location);
	}

	private void ensureOpen() {
		if (closed) {
			throw new IllegalStateException("Reader is closed");
		}
	}

	private void ensureIncomplete() {
		if (complete) {
			throw new NoSuchElementException("Reader is complete");
		}
	}

	private CharBuffer getBuffer() {
		return proxy.getCharacters();
	}

	@Override
	public Object getProperty(String name) {
		if (name == null) {
			throw new IllegalArgumentException("Name must not be null");
		}
		return properties.get(name);
	}

	@Override
	public Map<String, Character> getDelimiters() {
		Dialect dialect = lexer.getDialect();

		if (dialect == null) {
			throw new IllegalStateException(
					"getDelimiters must be called "
					+ "within an interchange");
		}

		Map<String, Character> delimiters = new HashMap<>(5);
		delimiters.put(Delimiters.SEGMENT, dialect.getSegmentTerminator());
		delimiters.put(Delimiters.DATA_ELEMENT, dialect.getDataElementSeparator());
		delimiters.put(Delimiters.COMPONENT_ELEMENT, dialect.getComponentElementSeparator());

		if (dialect.getRepetitionSeparator() != '\0') {
			delimiters.put(Delimiters.REPETITION, dialect.getRepetitionSeparator());
		}

		if (dialect.getReleaseIndicator() != '\0') {
			delimiters.put(Delimiters.RELEASE, dialect.getReleaseIndicator());
		}

		return Collections.unmodifiableMap(delimiters);
	}

	@Override
	public int next() throws EDIStreamException {
		ensureOpen();
		ensureIncomplete();

		if (proxy.nextEvent()) {
			return proxy.getEvent();
		}
		proxy.resetEvents();

		try {
			lexer.parse();
		} catch (IOException e) {
			Location where = getLocation();
			throw new EDIStreamException("Error parsing input", where, e);
		}

		final int event = proxy.getEvent();

		if (event == Events.END_INTERCHANGE) {
			complete = true;
		}

		return event;
	}

	@Override
	public int nextTag() throws EDIStreamException {
		int event = 0;

		do {
			event = next();
		} while (!complete && event != Events.START_SEGMENT);

		if (event != Events.START_SEGMENT) {
			throw new NoSuchElementException("No additional tags in stream");
		}

		return event;
	}

	@Override
	public boolean hasNext() throws EDIStreamException {
		ensureOpen();
		return !complete;
	}

	@Override
	public void close() throws IOException {
		this.closed = true;
	}

	@Override
	public int getEventType() {
		ensureOpen();
		return proxy.getEvent();
	}

	@Override
	public String getStandard() {
		if (getEventType() != Events.START_INTERCHANGE) {
			throw new IllegalStateException("version not accessible");
		}

		return lexer.getDialect().getStandard();
	}

	@Override
	public String[] getVersion() {
		if (getEventType() != Events.START_INTERCHANGE) {
			throw new IllegalStateException("version not accessible");
		}

		return lexer.getDialect().getVersion();
	}

	@Override
	public void setSchema(Schema schema) {
		if (getEventType() != Events.START_INTERCHANGE) {
			throw new IllegalStateException(
					"schema set after interchange start");
		}

		if (this.schema != null) {
			throw new IllegalStateException("schema already set");
		}

		this.schema = schema;
		proxy.setSchema(schema);
	}

	@Override
	public void addSchema(Schema additionalSchema) throws EDISchemaException {

		if (this.schema == null) {
			throw new IllegalStateException("previous schema not set");
		}

		if (getEventType() != Events.END_SEGMENT) {
			throw new IllegalStateException("schema added not at segment end");
		}

		this.schema = proxy.addSchema(additionalSchema);
	}

	@Override
	public String getReferenceCode() {
		// FIXME: add state check
		return proxy.getReferenceCode();
	}

	@Override
	public EDIStreamValidationError getErrorType() {
		switch (getEventType()) {
		case Events.ELEMENT_DATA_ERROR:
		case Events.ELEMENT_OCCURRENCE_ERROR:
		case Events.SEGMENT_ERROR:
			return proxy.getErrorType();
		default:
			throw new IllegalStateException("not a valid text state");
		}
	}

	private void checkTextState() {
		int event = getEventType();

		switch (event) {
		case Events.ELEMENT_DATA:
		case Events.ELEMENT_DATA_ERROR:
		case Events.START_LOOP:
		case Events.START_SEGMENT:
		case Events.SEGMENT_ERROR:
			break;
		default:
			throw new IllegalStateException("not a valid text state");
		}
	}

	@Override
	public String getText() {
		ensureOpen();
		checkTextState();
		final CharBuffer buffer = getBuffer();

		return buffer.toString();
	}

	@Override
	public char[] getTextCharacters() {
		ensureOpen();
		checkTextState();
		final CharBuffer buffer = getBuffer();

		return buffer.array();
	}

	@Override
	public int getTextCharacters(
			int sourceStart,
			char[] target,
			int targetStart,
			int length) {

		ensureOpen();
		checkTextState();

		if (target == null) {
			throw new NullPointerException("Null target array");
		}
		if (targetStart < 0) {
			throw new IndexOutOfBoundsException("targetStart < 0");
		}
		if (targetStart > target.length) {
			throw new IndexOutOfBoundsException("targetStart > target.length");
		}
		if (length < 0) {
			throw new IndexOutOfBoundsException("length < 0");
		}
		if (length > target.length) {
			throw new IndexOutOfBoundsException("length > target.length");
		}

		final CharBuffer buffer = getBuffer();
		final char[] contents = buffer.array();
		final int count = buffer.remaining();

		if (sourceStart < 0) {
			throw new IndexOutOfBoundsException("sourceStart < 0");
		}

		if (sourceStart > count) {
			throw new IndexOutOfBoundsException("sourceStart > source length");
		}

		int toCopy = Math.min(count - sourceStart, length);
		System.arraycopy(contents, sourceStart, target, targetStart, toCopy);

		return toCopy;
	}

	@Override
	public int getTextStart() {
		ensureOpen();
		checkTextState();
		final CharBuffer buffer = getBuffer();

		return buffer.position();
	}

	@Override
	public int getTextLength() {
		ensureOpen();
		checkTextState();
		final CharBuffer buffer = getBuffer();

		return buffer.limit();
	}

	@Override
	public Location getLocation() {
		ensureOpen();
		return proxy.getLocation();
	}

	@Override
	public void setBinaryDataLength(long length)
			throws EDIStreamException {
		ensureOpen();

		switch (getEventType()) {
		case Events.START_SEGMENT:
		case Events.ELEMENT_DATA:
		case Events.END_COMPOSITE:
			break;
		default:
			throw new IllegalStateException();
		}

		lexer.setBinaryLength(length);
	}

	@Override
	public InputStream getBinaryData() {
		ensureOpen();

		if (getEventType() != Events.ELEMENT_DATA_BINARY) {
			throw new IllegalStateException();
		}

		return proxy.getBinary();
	}

	/**************************************************************************/
}
