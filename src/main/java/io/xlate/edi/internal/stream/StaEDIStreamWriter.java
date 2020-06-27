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
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.logging.Logger;

import io.xlate.edi.internal.stream.tokenization.CharacterClass;
import io.xlate.edi.internal.stream.tokenization.CharacterSet;
import io.xlate.edi.internal.stream.tokenization.Dialect;
import io.xlate.edi.internal.stream.tokenization.DialectFactory;
import io.xlate.edi.internal.stream.tokenization.EDIException;
import io.xlate.edi.internal.stream.tokenization.EDIFACTDialect;
import io.xlate.edi.internal.stream.tokenization.ElementDataHandler;
import io.xlate.edi.internal.stream.tokenization.State;
import io.xlate.edi.internal.stream.tokenization.ValidationEventHandler;
import io.xlate.edi.internal.stream.validation.UsageError;
import io.xlate.edi.internal.stream.validation.Validator;
import io.xlate.edi.schema.EDIType;
import io.xlate.edi.schema.Schema;
import io.xlate.edi.stream.EDIOutputFactory;
import io.xlate.edi.stream.EDIStreamConstants.Delimiters;
import io.xlate.edi.stream.EDIStreamEvent;
import io.xlate.edi.stream.EDIStreamException;
import io.xlate.edi.stream.EDIStreamValidationError;
import io.xlate.edi.stream.EDIStreamWriter;
import io.xlate.edi.stream.EDIValidationException;
import io.xlate.edi.stream.Location;

public class StaEDIStreamWriter implements EDIStreamWriter, ElementDataHandler, ValidationEventHandler {

    static final Logger LOGGER = Logger.getLogger(StaEDIStreamWriter.class.getName());

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
    private final OutputStreamWriter writer;
    private final Map<String, Object> properties;
    private Dialect dialect;

    private final StaEDIStreamLocation location;
    private Schema controlSchema;
    private Validator controlValidator;
    private boolean transactionSchemaAllowed = false;
    private boolean transaction = false;
    private Schema transactionSchema;
    private Validator transactionValidator;
    private CharArraySequence dataHolder = new CharArraySequence();
    private boolean atomicElementWrite = false;
    private CharBuffer elementBuffer = CharBuffer.allocate(500);
    private List<EDIValidationException> errors = new ArrayList<>();

    private char segmentTerminator;
    private char dataElementSeparator;
    private char componentElementSeparator;
    private char repetitionSeparator;
    private char decimalMark;
    private char releaseIndicator;

    final boolean emptyElementTruncation;
    private final boolean prettyPrint;
    private final String lineSeparator;

    private long elementLength = 0;

    private int emptyElements = 0;
    private boolean unterminatedElement = false;

    private int emptyComponents = 0;
    private boolean unterminatedComponent = false;

    public StaEDIStreamWriter(OutputStream stream, Charset charset, Map<String, Object> properties) {
        this.stream = stream;
        this.writer = new OutputStreamWriter(stream, charset);
        this.properties = new HashMap<>(properties);
        this.emptyElementTruncation = booleanValue(properties.get(EDIOutputFactory.TRUNCATE_EMPTY_ELEMENTS));
        this.prettyPrint = booleanValue(properties.get(EDIOutputFactory.PRETTY_PRINT));

        if (prettyPrint) {
            lineSeparator = System.getProperty("line.separator");
        } else {
            lineSeparator = null;
        }
        this.location = new StaEDIStreamLocation();
    }

    boolean booleanValue(Object value) {
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        if (value instanceof String) {
            return Boolean.valueOf(value.toString());
        }
        if (value == null) {
            return false;
        }
        LOGGER.warning(() -> "Value [" + value + "] could not be converted to boolean");
        return false;
    }

    private void setupDelimiters() {
        segmentTerminator = getDelimiter(properties, Delimiters.SEGMENT, dialect::getSegmentTerminator);
        dataElementSeparator = getDelimiter(properties, Delimiters.DATA_ELEMENT, dialect::getDataElementSeparator);
        componentElementSeparator = getDelimiter(properties, Delimiters.COMPONENT_ELEMENT, dialect::getComponentElementSeparator);
        repetitionSeparator = getDelimiter(properties, Delimiters.REPETITION, dialect::getRepetitionSeparator);
        decimalMark = getDelimiter(properties, Delimiters.DECIMAL, dialect::getDecimalMark);
        releaseIndicator = getDelimiter(properties, Delimiters.RELEASE, dialect::getReleaseIndicator);
    }

    private boolean areDelimitersSpecified() {
        return Arrays.asList(Delimiters.SEGMENT,
                             Delimiters.DATA_ELEMENT,
                             Delimiters.COMPONENT_ELEMENT,
                             Delimiters.REPETITION,
                             Delimiters.DECIMAL,
                             Delimiters.RELEASE)
                .stream()
                .anyMatch(properties::containsKey);
    }

    static char getDelimiter(Map<String, Object> properties, String key, Supplier<Character> defaultSupplier) {
        if (properties.containsKey(key)) {
            return (char) properties.get(key);
        }
        return defaultSupplier.get();
    }

    static void putDelimiter(String key, char value, Map<String, Character> delimiters) {
        if (value != '\0') {
            delimiters.put(key, value);
        }
    }

    private static void ensureArgs(int arrayLength, int start, int end) {
        if (start < 0 || start >= arrayLength || end > arrayLength) {
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

    private void ensureLevelAtLeast(int lvl) {
        if (this.level < lvl) {
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
    public void close() throws EDIStreamException {
        flush();
        // Do not close the stream
    }

    @Override
    public void flush() throws EDIStreamException {
        try {
            writer.flush();
            stream.flush();
        } catch (IOException e) {
            throw new EDIStreamException("Exception flushing output stream", location, e);
        }
    }

    @Override
    public void setControlSchema(Schema controlSchema) {
        ensureLevel(LEVEL_INITIAL);
        this.controlSchema = controlSchema;
        controlValidator = controlSchema != null ? new Validator(controlSchema, true, null) : null;
    }

    @Override
    public void setTransactionSchema(Schema transactionSchema) {
        if (!Objects.equals(this.transactionSchema, transactionSchema)) {
            this.transactionSchema = transactionSchema;
            transactionValidator = transactionSchema != null ? new Validator(transactionSchema, true, controlSchema) : null;
        }
    }

    @Override
    public Location getLocation() {
        return location;
    }

    @Override
    public String getStandard() {
        if (dialect == null) {
            throw new IllegalStateException("standard not accessible");
        }

        return dialect.getStandard();
    }

    @Override
    public Map<String, Character> getDelimiters() {
        if (dialect == null) {
            throw new IllegalStateException("standard not accessible");
        }

        Map<String, Character> delimiters = new HashMap<>(6);
        putDelimiter(Delimiters.SEGMENT, segmentTerminator, delimiters);
        putDelimiter(Delimiters.DATA_ELEMENT, dataElementSeparator, delimiters);
        putDelimiter(Delimiters.COMPONENT_ELEMENT, componentElementSeparator, delimiters);
        putDelimiter(Delimiters.REPETITION, repetitionSeparator, delimiters);
        putDelimiter(Delimiters.DECIMAL, decimalMark, delimiters);
        putDelimiter(Delimiters.RELEASE, releaseIndicator, delimiters);

        return delimiters;
    }

    private Validator validator() {
        // Do not use the transactionValidator in the period where it may be set/mutated by the user
        return transaction && !transactionSchemaAllowed ? transactionValidator : controlValidator;
    }

    private void write(int output) throws EDIStreamException {
        CharacterClass clazz;

        clazz = characters.getClass(output);

        if (clazz == CharacterClass.INVALID) {
            throw new EDIStreamException(String.format("Invalid character: 0x%04X", output), location);
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
                    // Set up the delimiters again once the dialect has confirmed them
                    setupDelimiters();

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
                throw new EDIStreamException(String.format("Unexpected header character: 0x%04X [%s]", output, (char) output), location);
            }
            break;
        case INVALID:
            throw new EDIException(String.format("Invalid state: %s; output 0x%04X", state, output));
        default:
            break;
        }

        try {
            location.incrementOffset(output);
            writer.write(output);
        } catch (IOException e) {
            throw new EDIStreamException("Exception to output stream", location, e);
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
        flush();
        return this;
    }

    @Override
    public EDIStreamWriter writeStartSegment(String name) throws EDIStreamException {
        ensureLevel(LEVEL_INTERCHANGE);
        location.incrementSegmentPosition(name);
        validate(validator -> validator.validateSegment(this, name));

        if (exitTransaction(name)) {
            transaction = false;
            validate(validator -> validator.validateSegment(this, name));
        }

        if (state == State.INITIAL) {
            dialect = DialectFactory.getDialect(name);
            setupDelimiters();

            if (dialect instanceof EDIFACTDialect) {
                if (EDIFACTDialect.UNB.equals(name) && areDelimitersSpecified()) {
                    /*
                     * Writing the EDIFACT header when delimiters were given via properties requires that
                     * a UNA is written first.
                     */
                    dialect = DialectFactory.getDialect(EDIFACTDialect.UNA);
                    writeServiceAdviceString();
                    // Now write the UNB
                    writeString(name);
                } else {
                    writeString(name);

                    if (EDIFACTDialect.UNA.equals(name)) {
                        writeServiceAdviceCharacters();
                    }
                }
            } else {
                writeString(name);
            }
        } else {
            writeString(name);
        }

        level = LEVEL_SEGMENT;
        emptyElements = 0;
        // Treat the segment tag as an unterminated element that must be closed when element data is encountered
        unterminatedElement = true;

        return this;
    }

    void writeServiceAdviceString() throws EDIStreamException {
        writeString(EDIFACTDialect.UNA);
        writeServiceAdviceCharacters();
        writeSegmentTerminator();
    }

    void writeServiceAdviceCharacters() throws EDIStreamException {
        write(this.componentElementSeparator);
        write(this.dataElementSeparator);
        write(this.decimalMark);
        write(this.releaseIndicator);
        write(this.repetitionSeparator);
    }

    private void writeString(String value) throws EDIStreamException {
        for (int i = 0, m = value.length(); i < m; i++) {
            write(value.charAt(i));
        }
    }

    void writeSegmentTerminator() throws EDIStreamException {
        write(this.segmentTerminator);

        if (prettyPrint) {
            writeString(lineSeparator);
        }
    }

    boolean exitTransaction(CharSequence tag) {
        return transaction && !transactionSchemaAllowed && controlSchema != null
                && controlSchema.containsSegment(tag.toString());
    }

    @Override
    public EDIStreamWriter writeEndSegment() throws EDIStreamException {
        ensureLevelAtLeast(LEVEL_SEGMENT);
        if (level > LEVEL_SEGMENT) {
            validateElement(this.elementBuffer::flip, this.elementBuffer);
        }
        validate(validator -> validator.validateSyntax(dialect, this, this, location, false));

        if (state == State.ELEMENT_DATA_BINARY) {
            state = State.ELEMENT_END_BINARY;
        }

        writeSegmentTerminator();

        level = LEVEL_INTERCHANGE;
        location.clearSegmentLocations();
        transactionSchemaAllowed = false;

        return this;
    }

    @Override
    public EDIStreamWriter writeStartElement() throws EDIStreamException {
        ensureLevel(LEVEL_SEGMENT);
        level = LEVEL_ELEMENT;
        location.incrementElementPosition();
        elementBuffer.clear();
        elementLength = 0;
        emptyComponents = 0;
        unterminatedComponent = false;

        if (!emptyElementTruncation) {
            write(this.dataElementSeparator);
        }

        return this;
    }

    @Override
    public EDIStreamWriter writeStartElementBinary() throws EDIStreamException {
        writeStartElement();
        state = State.ELEMENT_DATA_BINARY;
        return this;
    }

    @Override
    public EDIStreamWriter writeRepeatElement() throws EDIStreamException {
        ensureLevelAtLeast(LEVEL_SEGMENT);
        write(this.repetitionSeparator);
        // The repetition separator was used instead of the data element separator
        unterminatedElement = false;
        level = LEVEL_ELEMENT;
        location.incrementElementOccurrence();
        elementLength = 0;
        emptyComponents = 0;
        unterminatedComponent = false;
        return this;
    }

    @Override
    public EDIStreamWriter endElement() throws EDIStreamException {
        ensureLevelAtLeast(LEVEL_ELEMENT);

        if (!atomicElementWrite) {
            if (level > LEVEL_ELEMENT) {
                validate(validator -> validator.validateSyntax(dialect, this, this, location, true));
            } else {
                validateElement(this.elementBuffer::flip, this.elementBuffer);
            }
        }

        location.clearComponentPosition();
        level = LEVEL_SEGMENT;

        if (elementLength > 0) {
            unterminatedElement = true;
        } else {
            emptyElements++;
        }

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

        if (LEVEL_COMPOSITE == level && !emptyElementTruncation) {
            write(this.componentElementSeparator);
        }

        level = LEVEL_COMPONENT;
        location.incrementComponentPosition();
        elementBuffer.clear();
        elementLength = 0;
        return this;
    }

    @Override
    public EDIStreamWriter endComponent() throws EDIStreamException {
        ensureLevel(LEVEL_COMPONENT);

        if (!atomicElementWrite) {
            validateElement(this.elementBuffer::flip, this.elementBuffer);
        }

        if (elementLength > 0) {
            unterminatedComponent = true;
        } else {
            emptyComponents++;
        }

        level = LEVEL_COMPOSITE;
        return this;
    }

    @Override
    public EDIStreamWriter writeElement(CharSequence text) throws EDIStreamException {
        atomicElementWrite = true;
        writeStartElement();
        validateElement(() -> {}, text);
        writeElementData(text);
        endElement();
        atomicElementWrite = false;
        return this;
    }

    @Override
    public EDIStreamWriter writeElement(char[] text, int start, int end) throws EDIStreamException {
        atomicElementWrite = true;
        writeStartElement();
        validateElement(() -> dataHolder.set(text, start, start + end), dataHolder);
        writeElementData(text, start, end);
        endElement();
        atomicElementWrite = false;
        return this;
    }

    @Override
    public EDIStreamWriter writeEmptyElement() throws EDIStreamException {
        atomicElementWrite = true;
        writeStartElement();
        validateElement(dataHolder::clear, dataHolder);
        endElement();
        atomicElementWrite = false;
        return this;
    }

    @Override
    public EDIStreamWriter writeComponent(CharSequence text) throws EDIStreamException {
        atomicElementWrite = true;
        startComponent();
        validateElement(() -> {}, text);
        writeElementData(text);
        endComponent();
        atomicElementWrite = false;
        return this;
    }

    @Override
    public EDIStreamWriter writeComponent(char[] text, int start, int end) throws EDIStreamException {
        atomicElementWrite = true;
        startComponent();
        validateElement(() -> dataHolder.set(text, start, start + end), dataHolder);
        writeElementData(text, start, end);
        endComponent();
        atomicElementWrite = false;
        return this;
    }

    @Override
    public EDIStreamWriter writeEmptyComponent() throws EDIStreamException {
        atomicElementWrite = true;
        startComponent();
        validateElement(dataHolder::clear, dataHolder);
        endComponent();
        atomicElementWrite = false;
        return this;
    }

    void writeRequiredSeparators(int dataLength) throws EDIStreamException {
        if (dataLength < 1 || !emptyElementTruncation) {
            return;
        }

        if (level >= LEVEL_ELEMENT) {
            for (int i = 0; i < emptyElements; i++) {
                write(this.dataElementSeparator);
            }

            if (unterminatedElement) {
                write(this.dataElementSeparator);
            }

            emptyElements = 0;
            unterminatedElement = false;
        }

        if (level == LEVEL_COMPONENT) {
            for (int i = 0; i < emptyComponents; i++) {
                write(this.componentElementSeparator);
            }

            if (unterminatedComponent) {
                write(this.componentElementSeparator);
            }

            emptyComponents = 0;
            unterminatedComponent = false;
        }
    }

    @Override
    public EDIStreamWriter writeElementData(CharSequence text) throws EDIStreamException {
        ensureLevelAtLeast(LEVEL_ELEMENT);
        writeRequiredSeparators(text.length());

        for (int i = 0, m = text.length(); i < m; i++) {
            char curr = text.charAt(i);

            if (characters.isDelimiter(curr)) {
                if (releaseIndicator > 0) {
                    write(releaseIndicator);
                } else {
                    throw new IllegalArgumentException("Value contains separator: " + curr);
                }
            }

            write(curr);
            elementBuffer.put(curr);
            elementLength++;
        }
        return this;
    }

    @Override
    public EDIStreamWriter writeElementData(char[] text, int start, int end) throws EDIStreamException {
        ensureLevelAtLeast(LEVEL_ELEMENT);
        ensureArgs(text.length, start, end);
        writeRequiredSeparators(end - start);

        for (int i = start, m = end; i < m; i++) {
            char curr = text[i];
            if (characters.isDelimiter(curr)) {
                throw new IllegalArgumentException("Value contains separator");
            }
            write(curr);
            elementBuffer.put(curr);
            elementLength++;
        }

        return this;
    }

    @Override
    public EDIStreamWriter writeBinaryData(InputStream binaryStream) throws EDIStreamException {
        ensureLevel(LEVEL_ELEMENT);
        ensureState(State.ELEMENT_DATA_BINARY);
        int output;

        try {
            writeRequiredSeparators(binaryStream.available());

            flush(); // Write `Writer` buffers to stream before writing binary

            while ((output = binaryStream.read()) != -1) {
                location.incrementOffset(output);
                stream.write(output);
                elementLength++;
            }
        } catch (IOException e) {
            throw new EDIStreamException("Exception writing binary element data", location, e);
        }

        return this;
    }

    @Override
    public EDIStreamWriter writeBinaryData(byte[] binary, int start, int end) throws EDIStreamException {
        ensureLevel(LEVEL_ELEMENT);
        ensureState(State.ELEMENT_DATA_BINARY);
        ensureArgs(binary.length, start, end);
        writeRequiredSeparators(end - start);

        try {
            flush(); // Write `Writer` buffers to stream before writing binary

            for (int i = start; i < end; i++) {
                location.incrementOffset(binary[i]);
                stream.write(binary[i]);
                elementLength++;
            }
        } catch (IOException e) {
            throw new EDIStreamException("Exception writing binary element data", location, e);
        }

        return this;
    }

    @Override
    public EDIStreamWriter writeBinaryData(ByteBuffer binary) throws EDIStreamException {
        ensureLevel(LEVEL_ELEMENT);
        ensureState(State.ELEMENT_DATA_BINARY);
        writeRequiredSeparators(binary.remaining());

        while (binary.hasRemaining()) {
            write(binary.get());
            elementLength++;
        }

        return this;
    }

    @Override
    public boolean binaryData(InputStream binary) {
        // No operation
        return true;
    }

    @Override
    public boolean elementData(char[] text, int start, int length) {
        // No operation
        return true;
    }

    @Override
    public void loopBegin(CharSequence id) {
        if (EDIType.Type.TRANSACTION.toString().equals(id)) {
            transaction = true;
            transactionSchemaAllowed = true;
            if (transactionValidator != null) {
                transactionValidator.reset();
            }
        }
    }

    @Override
    public void loopEnd(CharSequence id) {
        if (EDIType.Type.TRANSACTION.toString().equals(id)) {
            transaction = false;
            dialect.transactionEnd();
        } else if (EDIType.Type.GROUP.toString().equals(id)) {
            dialect.groupEnd();
        }
    }

    @Override
    public void elementError(EDIStreamEvent event,
                             EDIStreamValidationError error,
                             CharSequence referenceCode,
                             CharSequence data,
                             int element,
                             int component,
                             int repetition) {

        StaEDIStreamLocation copy = location.copy();
        copy.setElementPosition(element);
        copy.setElementOccurrence(repetition);
        copy.setComponentPosition(component);

        errors.add(new EDIValidationException(event, error, copy, data));
    }

    @Override
    public void segmentError(CharSequence token, EDIStreamValidationError error) {
        errors.add(new EDIValidationException(EDIStreamEvent.SEGMENT_ERROR, error, location, token));
    }

    private void validate(Consumer<Validator> command) {
        Validator validator = validator();

        if (validator != null) {
            command.accept(validator);

            if (!errors.isEmpty()) {
                throw validationExceptionChain(errors);
            }
        }
    }

    private void validateElement(Runnable setupCommand, CharSequence data) {
        Validator validator = validator();

        if (validator != null) {
            setupCommand.run();

            if (!validator.validateElement(dialect, location, data)) {
                for (UsageError error : validator.getElementErrors()) {
                    elementError(error.getError().getCategory(),
                                 error.getError(),
                                 error.getCode(),
                                 data,
                                 location.getElementPosition(),
                                 location.getComponentPosition(),
                                 location.getElementOccurrence());
                }

                throw validationExceptionChain(errors);
            }

            dialect.elementData(data, location);
        }
    }

    EDIValidationException validationExceptionChain(List<EDIValidationException> errors) {
        Iterator<EDIValidationException> iter = errors.iterator();
        EDIValidationException first = iter.next();
        EDIValidationException e = first;

        while (iter.hasNext()) {
            EDIValidationException next = iter.next();
            e.setNextException(next);
            e = next;
        }

        return first;
    }
}
