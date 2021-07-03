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
package io.xlate.edi.internal.stream;

import java.io.IOException;
import java.io.InputStream;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import io.xlate.edi.internal.schema.SchemaUtils;
import io.xlate.edi.internal.stream.tokenization.Dialect;
import io.xlate.edi.internal.stream.tokenization.Lexer;
import io.xlate.edi.internal.stream.tokenization.ProxyEventHandler;
import io.xlate.edi.schema.EDIReference;
import io.xlate.edi.schema.EDISchemaException;
import io.xlate.edi.schema.Schema;
import io.xlate.edi.stream.EDIInputErrorReporter;
import io.xlate.edi.stream.EDIInputFactory;
import io.xlate.edi.stream.EDIStreamEvent;
import io.xlate.edi.stream.EDIStreamException;
import io.xlate.edi.stream.EDIStreamReader;
import io.xlate.edi.stream.EDIStreamValidationError;
import io.xlate.edi.stream.Location;

public class StaEDIStreamReader implements EDIStreamReader, Configurable {

    private static final Logger LOGGER = Logger.getLogger(StaEDIStreamReader.class.getName());

    private Schema controlSchema;
    private final Map<String, Object> properties;
    private final EDIInputErrorReporter reporter;
    private final StaEDIStreamLocation location = new StaEDIStreamLocation();
    private final ProxyEventHandler proxy;
    private final Lexer lexer;

    private boolean complete = false;
    private boolean closed = false;

    public StaEDIStreamReader(
            InputStream stream,
            Charset charset,
            Schema schema,
            Map<String, Object> properties,
            EDIInputErrorReporter reporter) {

        this.controlSchema = schema;
        this.properties = new HashMap<>(properties);
        this.reporter = reporter;
        this.proxy = new ProxyEventHandler(location, this.controlSchema, nestHierarchicalLoops());
        this.lexer = new Lexer(stream, charset, proxy, location, ignoreExtraneousCharacters());
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

    void ensureValueAvailable(Function<Dialect, Object> valueSupplier, String valueType) {
        if (lexer.getDialect() == null || valueSupplier.apply(lexer.getDialect()) == null) {
            throw new IllegalStateException(valueType + " not accessible");
        }
    }

    void requireEvent(String message, EDIStreamEvent... events) {
        EDIStreamEvent current = proxy.getEvent();

        for (EDIStreamEvent e : events) {
            if (current == e) {
                return;
            }
        }

        throw new IllegalStateException(message);
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
            throw new IllegalStateException("getDelimiters must be called "
                    + "within an interchange");
        }

        Map<String, Character> delimiters = new HashMap<>(5);
        delimiters.put(Delimiters.SEGMENT, dialect.getSegmentTerminator());
        delimiters.put(Delimiters.DATA_ELEMENT, dialect.getDataElementSeparator());
        delimiters.put(Delimiters.COMPONENT_ELEMENT, dialect.getComponentElementSeparator());

        if (dialect.getDecimalMark() != '\0') {
            delimiters.put(Delimiters.DECIMAL, dialect.getDecimalMark());
        }

        if (dialect.getRepetitionSeparator() != '\0') {
            delimiters.put(Delimiters.REPETITION, dialect.getRepetitionSeparator());
        }

        if (dialect.getReleaseIndicator() != '\0') {
            delimiters.put(Delimiters.RELEASE, dialect.getReleaseIndicator());
        }

        return Collections.unmodifiableMap(delimiters);
    }

    private EDIStreamEvent nextEvent() throws EDIStreamException {
        ensureOpen();
        ensureIncomplete();

        if (EDIStreamEvent.START_INTERCHANGE == proxy.getEvent() && useInternalControlSchema()) {
            try {
                LOGGER.finer(() -> "Setting control schema: " + getStandard() + ", " + getVersion());
                setControlSchema(SchemaUtils.getControlSchema(getStandard(), getVersion()));
                LOGGER.finer(() -> "Done setting control schema: " + getStandard() + ", " + getVersion());
            } catch (EDISchemaException e) {
                LOGGER.log(Level.WARNING,
                           String.format("Exception loading controlSchema for standard %s, version %s: %s",
                                         getStandard(),
                                         Arrays.stream(getVersion()).map(Object::toString)
                                               .collect(Collectors.joining(", ")),
                                         e.getMessage()),
                           e);
            }
        }

        if (!proxy.nextEvent()) {
            proxy.resetEvents();

            try {
                lexer.parse();
            } catch (IOException e) {
                Location where = getLocation();
                throw new EDIStreamException("Error parsing input", where, e);
            }
        }

        final EDIStreamEvent event = proxy.getEvent();

        LOGGER.finer(() -> "EDI event: " + event);

        if (event == EDIStreamEvent.END_INTERCHANGE) {
            complete = true;
        }

        if (event == EDIStreamEvent.ELEMENT_DATA && proxy.isBinaryElementLength()) {
            try {
                this.setBinaryDataLength(Long.parseLong(getText()));
            } catch (NumberFormatException e) {
                throw new EDIStreamException("Failed to parse binary element length", location, e);
            }
        }

        return event;
    }

    @Override
    public EDIStreamEvent next() throws EDIStreamException {
        EDIStreamEvent event = null;
        boolean eventFound = false;

        do {
            event = nextEvent();

            if (this.reporter != null && event.isError()) {
                reporter.report(getErrorType(), this);
            } else {
                eventFound = true;
            }
        } while (!complete && !eventFound);

        return event;
    }

    @Override
    public EDIStreamEvent nextTag() throws EDIStreamException {
        EDIStreamEvent event = null;
        boolean tagFound = false;

        do {
            event = next();
            switch (event) {
            case START_GROUP:
            case START_TRANSACTION:
            case START_LOOP:
            case START_SEGMENT:
                tagFound = true;
                break;
            default:
                break;
            }
        } while (!complete && !tagFound);

        if (!tagFound) {
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
        // Do not close the stream
    }

    @Override
    public EDIStreamEvent getEventType() {
        ensureOpen();
        return proxy.getEvent();
    }

    @Override
    public String getStandard() {
        if (lexer.getDialect() == null) {
            throw new IllegalStateException("standard not accessible");
        }

        return lexer.getDialect().getStandard();
    }

    @Override
    public String[] getVersion() {
        ensureValueAvailable(Dialect::getVersion, "version");
        String[] version = lexer.getDialect().getVersion();
        return Arrays.copyOf(version, version.length);
    }

    @Override
    public String[] getTransactionVersion() {
        ensureValueAvailable(Dialect::getTransactionVersion, "transaction version");
        String[] version = lexer.getDialect().getTransactionVersion();
        return Arrays.copyOf(version, version.length);
    }

    @Override
    public String getTransactionVersionString() {
        ensureValueAvailable(Dialect::getTransactionVersion, "transaction version");
        return lexer.getDialect().getTransactionVersionString();
    }

    @Override
    public String getTransactionType() {
        ensureValueAvailable(Dialect::getTransactionType, "transaction type");
        return lexer.getDialect().getTransactionType();
    }

    @Override
    public Schema getControlSchema() {
        return this.controlSchema;
    }

    @Override
    public void setControlSchema(Schema schema) {
        if (getEventType() != EDIStreamEvent.START_INTERCHANGE) {
            throw new IllegalStateException("control schema set after interchange start");
        }

        if (this.controlSchema != null) {
            throw new IllegalStateException("control schema already set");
        }

        this.controlSchema = schema;
        proxy.setControlSchema(schema, validateControlCodeValues());
    }

    @Override
    public Schema getTransactionSchema() {
        return proxy.getTransactionSchema();
    }

    @Override
    public void setTransactionSchema(Schema schema) {
        if (proxy.isTransactionSchemaAllowed()) {
            proxy.setTransactionSchema(schema);
        } else {
            throw new IllegalStateException("Transaction schema can only be set during transaction start");
        }
    }

    @Override
    public String getReferenceCode() {
        return proxy.getReferenceCode();
    }

    @Override
    public EDIStreamValidationError getErrorType() {
        requireEvent("not a valid error state",
                     EDIStreamEvent.ELEMENT_DATA_ERROR,
                     EDIStreamEvent.ELEMENT_OCCURRENCE_ERROR,
                     EDIStreamEvent.SEGMENT_ERROR);
        return proxy.getErrorType();
    }

    private void checkTextState() {
        EDIStreamEvent event = getEventType();

        if (event == null) {
            throw new IllegalStateException("not a valid text state [" + event + ']');
        }

        switch (event) {
        case START_GROUP:
        case START_TRANSACTION:
        case START_LOOP:
        case START_SEGMENT:
        case END_GROUP:
        case END_TRANSACTION:
        case END_LOOP:
        case END_SEGMENT:
        case ELEMENT_DATA:
        case ELEMENT_DATA_ERROR:
        case ELEMENT_OCCURRENCE_ERROR:
        case SEGMENT_ERROR:
            break;
        default:
            throw new IllegalStateException("not a valid text state [" + event + ']');
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

        return Arrays.copyOf(buffer.array(), buffer.length());
    }

    @Override
    public int getTextCharacters(int sourceStart,
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
            throw new IndexOutOfBoundsException("length (" + length + ") > target.length (" + target.length + ")");
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
    public void setBinaryDataLength(long length) throws EDIStreamException {
        ensureOpen();
        requireEvent("invalid state for setting binary length",
                     EDIStreamEvent.START_SEGMENT,
                     EDIStreamEvent.ELEMENT_DATA,
                     EDIStreamEvent.END_COMPOSITE);
        lexer.setBinaryLength(length);
    }

    @Override
    public InputStream getBinaryData() {
        ensureOpen();
        requireEvent("not binary data element", EDIStreamEvent.ELEMENT_DATA_BINARY);
        return proxy.getBinary();
    }

    @Override
    public EDIReference getSchemaTypeReference() {
        return proxy.getSchemaTypeReference();
    }

    /**************************************************************************/

    boolean validateControlCodeValues() {
        return getProperty(EDIInputFactory.EDI_VALIDATE_CONTROL_CODE_VALUES, Boolean::parseBoolean, true);
    }

    boolean useInternalControlSchema() {
        if (this.controlSchema != null) {
            return false;
        }

        return getProperty(EDIInputFactory.EDI_VALIDATE_CONTROL_STRUCTURE, Boolean::parseBoolean, true);
    }

    boolean ignoreExtraneousCharacters() {
        return getProperty(EDIInputFactory.EDI_IGNORE_EXTRANEOUS_CHARACTERS, Boolean::parseBoolean, false);
    }

    boolean nestHierarchicalLoops() {
        return getProperty(EDIInputFactory.EDI_NEST_HIERARCHICAL_LOOPS, Boolean::parseBoolean, false);
    }
}
