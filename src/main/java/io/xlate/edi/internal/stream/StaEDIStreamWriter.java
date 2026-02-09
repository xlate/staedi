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
import java.util.Deque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
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
import io.xlate.edi.internal.stream.tokenization.StreamEvent;
import io.xlate.edi.internal.stream.tokenization.ValidationEventHandler;
import io.xlate.edi.internal.stream.tokenization.X12Dialect;
import io.xlate.edi.internal.stream.validation.UsageError;
import io.xlate.edi.internal.stream.validation.Validator;
import io.xlate.edi.internal.stream.validation.ValidatorConfig;
import io.xlate.edi.schema.EDIReference;
import io.xlate.edi.schema.EDIType;
import io.xlate.edi.schema.Schema;
import io.xlate.edi.stream.EDIOutputErrorReporter;
import io.xlate.edi.stream.EDIOutputFactory;
import io.xlate.edi.stream.EDIStreamConstants.Delimiters;
import io.xlate.edi.stream.EDIStreamEvent;
import io.xlate.edi.stream.EDIStreamException;
import io.xlate.edi.stream.EDIStreamValidationError;
import io.xlate.edi.stream.EDIStreamWriter;
import io.xlate.edi.stream.EDIValidationException;
import io.xlate.edi.stream.Location;

public class StaEDIStreamWriter implements EDIStreamWriter, ElementDataHandler, ValidationEventHandler, ValidatorConfig {

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
    private final EDIOutputErrorReporter reporter;
    private Dialect dialect;
    CharBuffer unconfirmedBuffer = CharBuffer.allocate(500);

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
    private final StringBuilder formattedElement = new StringBuilder();
    private List<EDIValidationException> errors = new ArrayList<>();
    private CharArraySequence elementHolder = new CharArraySequence();

    private final StreamEvent currentEvent = new StreamEvent();
    private final Deque<StreamEvent> currentEventQueue = new LinkedList<>(Arrays.asList(currentEvent));

    private char segmentTerminator;
    private char segmentTagTerminator;
    private char dataElementSeparator;
    private char componentElementSeparator;
    private char repetitionSeparator;
    private char decimalMark;
    private char releaseIndicator;

    final boolean emptyElementTruncation;
    final boolean formatElements;
    private final boolean prettyPrint;
    private String prettyPrintString;

    private long elementLength = 0;

    private int emptyElements = 0;
    private boolean unterminatedElement = false;

    private int emptyComponents = 0;
    private boolean unterminatedComponent = false;

    public StaEDIStreamWriter(OutputStream stream, Charset charset, Map<String, Object> properties, EDIOutputErrorReporter reporter) {
        this.stream = stream;
        this.writer = new OutputStreamWriter(stream, charset);
        this.properties = new HashMap<>(properties);
        this.reporter = reporter;
        this.emptyElementTruncation = booleanValue(properties.get(EDIOutputFactory.TRUNCATE_EMPTY_ELEMENTS));
        this.prettyPrint = booleanValue(properties.get(EDIOutputFactory.PRETTY_PRINT));
        this.formatElements = booleanValue(properties.get(EDIOutputFactory.FORMAT_ELEMENTS));
        this.location = new StaEDIStreamLocation();
    }

    @Override
    public boolean formatElements() {
        return formatElements;
    }

    @Override
    public boolean trimDiscriminatorValues() {
        return false;
    }

    @Override
    public boolean validateControlCodeValues() {
        return true;
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
        segmentTagTerminator = dialect.getSegmentTagTerminator(); // Not configurable - TRADACOMS
        dataElementSeparator = getDelimiter(properties, Delimiters.DATA_ELEMENT, dialect::getDataElementSeparator);
        componentElementSeparator = getDelimiter(properties, Delimiters.COMPONENT_ELEMENT, dialect::getComponentElementSeparator);
        decimalMark = getDelimiter(properties, Delimiters.DECIMAL, dialect::getDecimalMark);
        releaseIndicator = getDelimiter(properties, Delimiters.RELEASE, dialect::getReleaseIndicator);
        repetitionSeparator = getDelimiter(properties, Delimiters.REPETITION, dialect::getRepetitionSeparator);

        String lineSeparator = System.getProperty("line.separator");

        if (prettyPrint && lineSeparator.indexOf(segmentTerminator) < 0) {
            // Do not add the line separator after the segment terminator if they conflict. I.e., the separator contains the terminator
            prettyPrintString = lineSeparator;
        } else {
            prettyPrintString = "";
        }
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

    char getDelimiter(Map<String, Object> properties, String key, Supplier<Character> dialectSupplier) {
        if (properties.containsKey(key) && !dialect.isConfirmed()) {
            return (char) properties.get(key);
        }
        return dialectSupplier.get();
    }

    static void putDelimiter(String key, char value, Map<String, Character> delimiters) {
        if (value != '\0') {
            delimiters.put(key, value);
        }
    }

    private static void ensureArgs(int arrayLength, int start, int end) {
        if (start < 0 || (start > 0 && start >= arrayLength) || end > arrayLength) {
            throw new IndexOutOfBoundsException();
        }

        if (end < start) {
            throw new IllegalArgumentException();
        }
    }

    private void ensureFalse(boolean illegalState) {
        if (illegalState) {
            throw new IllegalStateException();
        }
    }

    private void ensureState(State s) {
        ensureFalse(this.state != s);
    }

    private void ensureLevel(int l) {
        ensureFalse(this.level != l);
    }

    private void ensureLevelAtLeast(int lvl) {
        ensureFalse(this.level < lvl);
    }

    private void ensureLevelBetween(int min, int max) {
        ensureFalse(this.level < min || this.level > max); // NOSONAR - ISE should be thrown when in an illegal state
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
    public Schema getControlSchema() {
        return this.controlSchema;
    }

    @Override
    public void setControlSchema(Schema controlSchema) {
        ensureLevel(LEVEL_INITIAL);
        this.controlSchema = controlSchema;
        controlValidator = Validator.forSchema(controlSchema, null, this);
    }

    @Override
    public void setTransactionSchema(Schema transactionSchema) {
        if (!Objects.equals(this.transactionSchema, transactionSchema)) {
            this.transactionSchema = transactionSchema;
            transactionValidator = Validator.forSchema(transactionSchema, controlSchema, this);
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

    private Optional<Validator> validator() {
        Validator validator;

        // Do not use the transactionValidator in the period where it may be set/mutated by the user
        if (transaction && !transactionSchemaAllowed) {
            validator = transactionValidator;
        } else {
            validator = controlValidator;
        }

        return Optional.ofNullable(validator);
    }

    private void write(int output) throws EDIStreamException {
        write(output, false);
    }

    private void write(int output, boolean isPrettyPrint) throws EDIStreamException {
        CharacterClass clazz;

        clazz = characters.getClass(output);

        if (clazz == CharacterClass.INVALID) {
            throw new EDIStreamException(String.format("Invalid character: 0x%04X", output), location);
        }

        state = State.transition(state, dialect, clazz);

        switch (state) {
        case HEADER_X12_I: // I(SA)
        case HEADER_EDIFACT_U: // U(NA) or U(NB)
        case HEADER_TRADACOMS_S: // S(TX)
            unconfirmedBuffer.clear();
            writeHeader((char) output, isPrettyPrint);
            break;
        case HEADER_X12_S:
        case HEADER_EDIFACT_N:
        case HEADER_TRADACOMS_T:
        case INTERCHANGE_CANDIDATE:
        case HEADER_DATA:
        case HEADER_ELEMENT_END:
        case HEADER_COMPONENT_END:
        case HEADER_SEGMENT_END:
            writeHeader((char) output, isPrettyPrint);
            break;
        case INVALID:
            throw new EDIException(String.format("Invalid state: %s; output 0x%04X", state, output));
        default:
            writeOutput(output);
            break;
        }
    }

    void writeHeader(char output, boolean isPrettyPrint) throws EDIStreamException {
        if (!isPrettyPrint && !dialect.appendHeader(characters, output)) {
            throw new EDIStreamException(String.format("Failed writing %s header: %s", dialect.getStandard(), dialect.getRejectionMessage()));
        }

        unconfirmedBuffer.append(output);

        if (dialect.isConfirmed()) {
            // Set up the delimiters again once the dialect has confirmed them
            setupDelimiters();

            // Switching to non-header states to proceed after dialect is confirmed
            switch (state) {
            case HEADER_DATA:
                state = State.TAG_SEARCH;
                break;
            case HEADER_ELEMENT_END:
                state = State.ELEMENT_END;
                break;
            case HEADER_SEGMENT_END:
                state = State.SEGMENT_END;
                break;
            default:
                throw new IllegalStateException("Confirmed at state " + state);
            }

            unconfirmedBuffer.flip();

            if (EDIFACTDialect.UNA.equals(dialect.getHeaderTag())) {
                // Overlay the UNA segment repetition separator now that it has be confirmed
                unconfirmedBuffer.put(7, this.repetitionSeparator > 0 ? this.repetitionSeparator : ' ');
            }

            while (unconfirmedBuffer.hasRemaining()) {
                writeOutput(unconfirmedBuffer.get());
            }
        }
    }

    void writeOutput(int output) throws EDIStreamException {
        try {
            location.incrementOffset(output);
            writer.write(output);
        } catch (IOException e) {
            throw new EDIStreamException("Exception to output stream", location, e);
        }
    }

    @Override
    public EDIStreamWriter startInterchange() {
        ensureLevel(LEVEL_INITIAL);
        ensureState(State.INITIAL);
        level = LEVEL_INTERCHANGE;
        if (controlSchema == null) {
            LOGGER.warning("Starting interchange without control structure validation. See EDIStreamWriter#setControlSchema");
        }
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

        if (state == State.INITIAL) {
            dialect = DialectFactory.getDialect(name, location);
            setupDelimiters();

            if (dialect instanceof EDIFACTDialect) {
                if (EDIFACTDialect.UNB.equals(name) && areDelimitersSpecified()) {
                    /*
                     * Writing the EDIFACT header when delimiters were given via properties requires that
                     * a UNA is written first.
                     */
                    dialect = DialectFactory.getDialect(EDIFACTDialect.UNA, location);
                    writeServiceAdviceString();
                    segmentValidation(name);
                    // Now write the UNB
                    writeString(name);
                } else {
                    if (EDIFACTDialect.UNA.equals(name)) {
                        writeString(name);
                        writeServiceAdviceCharacters();
                    } else {
                        segmentValidation(name);
                        writeString(name);
                    }
                }
            } else {
                segmentValidation(name);
                writeString(name);
            }
        } else {
            segmentValidation(name);
            writeString(name);
        }

        countSegment(name);
        level = LEVEL_SEGMENT;
        emptyElements = 0;
        terminateSegmentTag();

        return this;
    }

    void countSegment(String name) {
        if (controlValidator != null) {
            controlValidator.countSegment(name);
        }
    }

    void segmentValidation(String name) {
        validate(validator -> validator.validateSegment(this, name));

        if (exitTransaction(name)) {
            transaction = false;
            validate(validator -> validator.validateSegment(this, name));
        }
    }

    void terminateSegmentTag() throws EDIStreamException {
        if (this.segmentTagTerminator != '\0') {
            write(this.segmentTagTerminator);
            // TRADACOMS - Empty segments not supported due to fixed segment tag terminator requirement
            unterminatedElement = false;
        } else {
            // Treat the segment tag as an unterminated element that must be closed when element data is encountered
            unterminatedElement = true;
        }
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
        // This will be re-written once the dialect version is detected
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
            for (int i = 0, m = prettyPrintString.length(); i < m; i++) {
                write(prettyPrintString.charAt(i), true);
            }
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
        level = LEVEL_SEGMENT;
        validate(validator -> {
            validator.clearImplementationCandidates(this);
            validator.validateSyntax(dialect, this, this, location, false);
        });

        if (state == State.ELEMENT_DATA_BINARY) {
            state = State.ELEMENT_END_BINARY;
        }

        writeSegmentTerminator();

        switch (state) {
        case SEGMENT_END:
        case HEADER_SEGMENT_END:
        case INITIAL: // Ending final segment of the interchange
            break;
        default:
            if (state.isHeaderState() && dialect instanceof X12Dialect) {
                throw new EDIStreamException("Invalid X12 ISA segment: too short or elements missing");
            }
            break;
        }

        level = LEVEL_INTERCHANGE;
        location.clearSegmentLocations(true);
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

        if (!emptyElementTruncation && unterminatedElement) {
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
        ensureFalse(state == State.ELEMENT_DATA_BINARY);

        if (LEVEL_ELEMENT == level) {
            // Level is LEVEL_ELEMENT only for the first component
            validateCompositeOccurrence();
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
        CharSequence value = validateElement(() -> {}, text);
        writeElementData(value);
        endElement();
        atomicElementWrite = false;
        return this;
    }

    @Override
    public EDIStreamWriter writeElement(char[] text, int start, int end) throws EDIStreamException {
        atomicElementWrite = true;
        writeStartElement();
        CharSequence value = validateElement(() -> dataHolder.set(text, start, start + end), dataHolder);
        writeElementData(value);
        endElement();
        atomicElementWrite = false;
        return this;
    }

    @Override
    public EDIStreamWriter writeEmptyElement() throws EDIStreamException {
        atomicElementWrite = true;
        writeStartElement();
        // Ignore possibly-formatted value
        validateElement(dataHolder::clear, dataHolder);
        endElement();
        atomicElementWrite = false;
        return this;
    }

    @Override
    public EDIStreamWriter writeComponent(CharSequence text) throws EDIStreamException {
        atomicElementWrite = true;
        startComponent();
        CharSequence value = validateElement(() -> {}, text);
        writeElementData(value);
        endComponent();
        atomicElementWrite = false;
        return this;
    }

    @Override
    public EDIStreamWriter writeComponent(char[] text, int start, int end) throws EDIStreamException {
        atomicElementWrite = true;
        startComponent();
        CharSequence value = validateElement(() -> dataHolder.set(text, start, start + end), dataHolder);
        writeElementData(value);
        endComponent();
        atomicElementWrite = false;
        return this;
    }

    @Override
    public EDIStreamWriter writeEmptyComponent() throws EDIStreamException {
        atomicElementWrite = true;
        startComponent();
        // Ignore possibly-formatted value
        validateElement(dataHolder::clear, dataHolder);
        endComponent();
        atomicElementWrite = false;
        return this;
    }

    void writeRequiredSeparators(int dataLength) throws EDIStreamException {
        if (dataLength < 1 || !emptyElementTruncation) {
            return;
        }

        writeRequiredSeparator(emptyElements, unterminatedElement, this.dataElementSeparator);
        emptyElements = 0;
        unterminatedElement = false;

        if (level == LEVEL_COMPONENT) {
            writeRequiredSeparator(emptyComponents, unterminatedComponent, this.componentElementSeparator);
            emptyComponents = 0;
            unterminatedComponent = false;
        }
    }

    void writeRequiredSeparator(int emptyCount, boolean unterminated, char separator) throws EDIStreamException {
        for (int i = 0; i < emptyCount; i++) {
            write(separator);
        }

        if (unterminated) {
            write(separator);
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
                writeBinary(output);
            }
        } catch (IOException e) {
            throw new EDIStreamException("Exception reading binary data source for output", location, e);
        }

        return this;
    }

    @Override
    public EDIStreamWriter writeBinaryData(byte[] binary, int start, int end) throws EDIStreamException {
        ensureLevel(LEVEL_ELEMENT);
        ensureState(State.ELEMENT_DATA_BINARY);
        ensureArgs(binary.length, start, end);
        writeRequiredSeparators(end - start);
        flush(); // Write `Writer` buffers to stream before writing binary

        for (int i = start; i < end; i++) {
            writeBinary(binary[i]);
        }

        return this;
    }

    @Override
    public EDIStreamWriter writeBinaryData(ByteBuffer binary) throws EDIStreamException {
        ensureLevel(LEVEL_ELEMENT);
        ensureState(State.ELEMENT_DATA_BINARY);
        writeRequiredSeparators(binary.remaining());
        flush(); // Write `Writer` buffers to stream before writing binary

        while (binary.hasRemaining()) {
            writeBinary(binary.get());
        }

        return this;
    }

    private void writeBinary(int output) throws EDIStreamException {
        location.incrementOffset(output);

        try {
            stream.write(output);
        } catch (IOException e) {
            throw new EDIStreamException("Exception writing binary element data", location, e);
        }

        elementLength++;
    }

    @Override
    public boolean binaryData(InputStream binary) {
        // No operation
        return true;
    }

    @Override
    public boolean elementData(CharSequence text, boolean fromStream) {
        if (level > LEVEL_ELEMENT) {
            location.incrementComponentPosition();
        } else {
            location.incrementElementPosition();
        }

        dialect.elementData(elementHolder, location);

        validator().ifPresent(validator -> {
            if (!validator.validateElement(dialect, location, elementHolder, derivedComposite(validator), null)) {
                reportElementErrors(validator, elementHolder);
            }
        });

        return true;
    }

    @Override
    public void loopBegin(EDIReference typeReference) {
        final String loopCode = typeReference.getReferencedType().getCode();

        if (EDIType.Type.TRANSACTION.toString().equals(loopCode)) {
            transaction = true;
            transactionSchemaAllowed = true;
            if (transactionValidator != null) {
                transactionValidator.reset();
            }
        }
    }

    @Override
    public void loopEnd(EDIReference typeReference) {
        final String loopCode = typeReference.getReferencedType().getCode();

        if (EDIType.Type.TRANSACTION.toString().equals(loopCode)) {
            transaction = false;
            dialect.transactionEnd();
        } else if (EDIType.Type.GROUP.toString().equals(loopCode)) {
            dialect.groupEnd();
        }
    }

    @Override
    public void elementError(EDIStreamEvent event,
                             EDIStreamValidationError error,
                             EDIReference typeReference,
                             CharSequence data,
                             int element,
                             int component,
                             int repetition) {

        StaEDIStreamLocation copy = location.copy();
        copy.setElementPosition(element);
        copy.setElementOccurrence(repetition);
        copy.setComponentPosition(component);

        if (this.reporter != null) {
            this.reporter.report(error, this, copy, data, typeReference);
        } else {
            errors.add(new EDIValidationException(event, error, copy, data));
        }
    }

    @Override
    public void segmentError(CharSequence token, EDIReference typeReference, EDIStreamValidationError error) {
        if (this.reporter != null) {
            this.reporter.report(error, this, this.getLocation(), token, typeReference);
        } else {
            errors.add(new EDIValidationException(EDIStreamEvent.SEGMENT_ERROR, error, location, token));
        }
    }

    private boolean derivedComposite(Validator validator) {
        if (location.getComponentPosition() > -1) {
            return false;
        }

        return validator.isComposite(dialect, location);
    }

    private void validate(Consumer<Validator> command) {
        validator().ifPresent(validator -> {
            errors.clear();
            command.accept(validator);

            if (!errors.isEmpty()) {
                throw validationExceptionChain(errors);
            }
        });
    }

    private void validateCompositeOccurrence() {
        validator().ifPresent(validator -> {
            errors.clear();

            if (!validator.validCompositeOccurrences(dialect, location)) {
                reportElementErrors(validator, "");
            }

            if (!errors.isEmpty()) {
                throw validationExceptionChain(errors);
            }
        });
    }

    private CharSequence validateElement(Runnable setupCommand, CharSequence data) {
        return validator()
                .map(validator -> validateElement(setupCommand, data, validator))
                .orElse(data);
    }

    CharSequence validateElement(Runnable setupCommand, CharSequence data, Validator validator) {
        CharSequence elementData;

        if (this.formatElements) {
            elementData = this.formattedElement;
            this.formattedElement.setLength(0);
            this.formattedElement.append(data); // Validator will clear and re-format if configured
        } else {
            elementData = data;
        }

        errors.clear();
        setupCommand.run();

        if (!validator.validateElement(dialect, location, data, derivedComposite(validator), this.formattedElement)) {
            reportElementErrors(validator, elementData);
        }

        if (validator.isPendingDiscrimination()) {
            currentEvent.update(EDIStreamEvent.ELEMENT_DATA, null, data, null, location);
            validator.selectImplementation(currentEventQueue, this);
        }

        if (!errors.isEmpty()) {
            throw validationExceptionChain(errors);
        }

        dialect.elementData(elementData, location);
        return elementData;
    }

    void reportElementErrors(Validator validator, CharSequence data) {
        for (UsageError error : validator.getElementErrors()) {
            elementError(error.getError().getCategory(),
                         error.getError(),
                         error.getTypeReference(),
                         data,
                         location.getElementPosition(),
                         location.getComponentPosition(),
                         location.getElementOccurrence());
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
