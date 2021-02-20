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

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CoderResult;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.logging.Logger;

import io.xlate.edi.internal.stream.LocationView;
import io.xlate.edi.internal.stream.StaEDIStreamLocation;
import io.xlate.edi.stream.Location;

public class Lexer {

    private static final Logger LOGGER = Logger.getLogger(Lexer.class.getName());

    private enum Mode {
        INTERCHANGE,
        SEGMENT,
        COMPOSITE
    }

    private final Deque<Mode> modes = new ArrayDeque<>();
    private int input = 0;
    private State state = State.INITIAL;
    private State previous;

    private interface Notifier {
        boolean execute(State state, int start, int length);
    }

    private final Deque<Notifier> events = new ArrayDeque<>(20);
    private final Deque<State> stateQueue = new ArrayDeque<>(20);
    private final Deque<Integer> startQueue = new ArrayDeque<>(20);
    private final Deque<Integer> lengthQueue = new ArrayDeque<>(20);

    private final InputStream stream;
    private CharsetDecoder decoder;
    private char[] readChar = new char[1];
    private CharBuffer readCharBuf = CharBuffer.wrap(readChar);
    private ByteBuffer readByteBuf = ByteBuffer.allocate(4);

    private final StaEDIStreamLocation location;
    private final CharacterSet characters;
    private CharBuffer buffer = CharBuffer.allocate(4096);
    private Dialect dialect;

    private long binaryRemain = -1;
    private InputStream binaryStream = null;

    private Notifier isn;
    private Notifier ien;
    private Notifier ssn;
    private Notifier sen;
    private Notifier csn;
    private Notifier cen;
    private Notifier en;
    private Notifier bn;

    public Lexer(InputStream stream, Charset charset, EventHandler handler, StaEDIStreamLocation location, boolean extraneousIgnored) {
        if (stream.markSupported()) {
            this.stream = stream;
        } else {
            this.stream = new BufferedInputStream(stream);
        }

        this.decoder = charset.newDecoder();

        this.location = location;
        this.characters = new CharacterSet(extraneousIgnored);

        isn = (notifyState, start, length) -> {
            handler.interchangeBegin(dialect);
            return true;
        };

        ien = (notifyState, start, length) -> {
            handler.interchangeEnd();
            dialect = null;
            characters.reset();
            return true;
        };

        ssn = (notifyState, start, length) -> {
            String segmentTag = new String(buffer.array(), start, length);
            location.incrementSegmentPosition(segmentTag);
            return handler.segmentBegin(segmentTag);
        };

        sen = (notifyState, start, length) -> {
            boolean eventsReady = handler.segmentEnd();
            location.clearSegmentLocations();
            return eventsReady;
        };

        csn = (notifyState, start, length) -> {
            if (location.isRepeated()) {
                location.incrementElementOccurrence();
            } else {
                location.incrementElementPosition();
            }

            return handler.compositeBegin(false);
        };

        cen = (notifyState, start, length) -> {
            boolean eventsReady = handler.compositeEnd(false);
            location.clearComponentPosition();
            return eventsReady;
        };

        en = (notifyState, start, length) -> {
            updateLocation(notifyState, location);
            return handler.elementData(buffer.array(), start, length);
        };

        bn = (notifyState, start, length) -> {
            updateLocation(notifyState, location);
            return handler.binaryData(binaryStream);
        };
    }

    public Dialect getDialect() {
        return dialect;
    }

    public void setBinaryLength(long binaryLength) {
        this.binaryRemain = binaryLength;

        this.binaryStream = new InputStream() {
            @Override
            public int read() throws IOException {
                int binaryInput = -1;

                if (binaryRemain-- < 1 || (binaryInput = stream.read()) < 0) {
                    state = State.ELEMENT_END_BINARY;
                } else {
                    location.incrementOffset(binaryInput);
                }

                return binaryInput;
            }
        };

        enqueue(bn, 0);
        state = State.ELEMENT_DATA_BINARY;
    }

    public void parse() throws IOException, EDIException {
        if (nextEvent()) {
            return;
        }

        if (state == State.INVALID) {
            // Unable to proceed once the state becomes invalid
            throw invalidStateError();
        }

        boolean eventsReady = false;

        while (!eventsReady && (input = readCharacter()) > -1) {
            location.incrementOffset(input);

            CharacterClass clazz = characters.getClass(input);
            previous = state;
            state = State.transition(state, dialect, clazz);
            LOGGER.finer(() -> String.format("State %s(%s, %s) -> %s", previous, clazz, Dialect.getStandard(dialect), state));

            switch (state) {
            case INITIAL:
            case TAG_SEARCH:
            case HEADER_EDIFACT_UNB_SEARCH:
                break;
            case HEADER_X12_I:
            case HEADER_X12_S:
            case HEADER_EDIFACT_N:
            case HEADER_EDIFACT_U:
            case HEADER_TRADACOMS_S:
            case HEADER_TRADACOMS_T:
            case TAG_1:
            case TAG_2:
            case TAG_3:
            case TRAILER_X12_I:
            case TRAILER_X12_E:
            case TRAILER_X12_A:
            case TRAILER_EDIFACT_U:
            case TRAILER_EDIFACT_N:
            case TRAILER_EDIFACT_Z:
            case TRAILER_TRADACOMS_E:
            case TRAILER_TRADACOMS_N:
            case TRAILER_TRADACOMS_D:
            case ELEMENT_DATA:
            case TRAILER_ELEMENT_DATA:
                buffer.put((char) input);
                break;
            case ELEMENT_INVALID_DATA:
                if (!characters.isIgnored(input)) {
                    buffer.put((char) input);
                }
                break;
            case HEADER_EDIFACT_UNB_1: // U - When UNA is present
            case HEADER_EDIFACT_UNB_2: // N - When UNA is present
            case HEADER_EDIFACT_UNB_3: // B - When UNA is present
                handleStateHeaderTag(input);
                break;
            case HEADER_RELEASE:
            case DATA_RELEASE:
                // Skip this character - next character will be literal value
                break;
            case ELEMENT_DATA_BINARY:
                handleStateElementDataBinary();
                break;
            case INTERCHANGE_CANDIDATE:
                // ISA, UNA, or UNB was found
                handleStateInterchangeCandidate(input);
                break;
            case HEADER_DATA:
            case HEADER_INVALID_DATA:
                handleStateHeaderData(input);
                eventsReady = dialectConfirmed(State.TAG_SEARCH);
                break;
            case HEADER_SEGMENT_BEGIN:
                dialect.appendHeader(characters, (char) input);
                openSegment();
                eventsReady = dialectConfirmed(State.ELEMENT_END);
                break;
            case HEADER_ELEMENT_END:
                dialect.appendHeader(characters, (char) input);
                handleElement();
                eventsReady = dialectConfirmed(State.ELEMENT_END);
                break;
            case HEADER_COMPONENT_END:
                dialect.appendHeader(characters, (char) input);
                handleComponent();
                eventsReady = dialectConfirmed(State.COMPONENT_END);
                break;
            case SEGMENT_BEGIN:
            case TRAILER_BEGIN:
                openSegment();
                eventsReady = nextEvent();
                break;
            case SEGMENT_END:
                closeSegment();
                eventsReady = nextEvent();
                break;
            case SEGMENT_EMPTY:
                emptySegment();
                eventsReady = nextEvent();
                break;
            case COMPONENT_END:
                handleComponent();
                eventsReady = nextEvent();
                break;
            case ELEMENT_END:
            case TRAILER_ELEMENT_END:
            case ELEMENT_REPEAT:
                handleElement();
                eventsReady = nextEvent();
                break;
            case INTERCHANGE_END:
                closeInterchange();
                eventsReady = nextEvent();
                break;
            default:
                if (characters.isIgnored(input)) {
                    state = previous;
                } else if (clazz != CharacterClass.INVALID) {
                    throw invalidStateError();
                } else {
                    throw error(EDIException.INVALID_CHARACTER);
                }
            }
        }

        if (input < 0) {
            throw error(EDIException.INCOMPLETE_STREAM);
        }
    }

    int readCharacter() throws IOException {
        int next = stream.read();

        if (next < 0) {
            return -1;
        }

        boolean endOfInput = false;
        boolean complete = false;
        int position = 0;

        readCharBuf.clear();
        readByteBuf.clear();
        readByteBuf.put((byte) next);

        do {
            readByteBuf.flip();
            CoderResult cr = decoder.decode(readByteBuf, readCharBuf, endOfInput);

            if (!cr.isUnderflow()) {
                cr.throwException();
            }

            if (endOfInput) {
                complete = true;
            } else if (readCharBuf.position() > 0) {
                // Single character successfully written to the CharBuffer
                complete = true;
            } else {
                next = stream.read();

                if (next < 0) {
                    endOfInput = true;
                    decoder.reset();
                } else {
                    readByteBuf.limit(readByteBuf.capacity());
                    readByteBuf.position(++position);
                    readByteBuf.put((byte) next);
                }
            }
        } while (!complete);

        if (endOfInput) {
            decoder.reset();
        }

        if (readCharBuf.position() == 0 && endOfInput) {
            // Nothing was written to the CharBuffer
            return -1;
        }

        return readChar[0];
    }

    void handleStateHeaderTag(int input) {
        buffer.put((char) input);
        dialect.appendHeader(characters, (char) input);
    }

    void handleStateElementDataBinary() {
        /*
         * Not all of the binary data has been consumed. I.e. #next was
         * called before completion.
         */
        if (--binaryRemain < 1) {
            state = State.ELEMENT_END_BINARY;
        }
    }

    void handleStateInterchangeCandidate(int input) throws EDIException {
        buffer.put((char) input);
        final char[] header = buffer.array();
        final int length = buffer.position();
        dialect = DialectFactory.getDialect(header, 0, length);
        for (int i = 0; i < length; i++) {
            dialect.appendHeader(characters, header[i]);
        }
        openInterchange();
        openSegment();
    }

    void handleStateHeaderData(int input) throws EDIException {
        dialect.appendHeader(characters, (char) input);

        switch (characters.getClass(input)) {
        case SEGMENT_DELIMITER:
            closeSegment();
            state = dialect.getTagSearchState();
            break;
        case SEGMENT_TAG_DELIMITER:
        case ELEMENT_DELIMITER:
        case ELEMENT_REPEATER:
        case COMPONENT_DELIMITER:
        case RELEASE_CHARACTER:
            break;
        default:
            if (dialect.getDecimalMark() != input && !characters.isIgnored(input)) {
                buffer.put((char) input);
            }
            break;
        }
    }

    /**
     * Determine if the input text has been confirmed by the dialect as being
     * initially accepted. If so, transition to the state given by the
     * <code>confirmed</code> parameter.
     *
     * @param confirmed the state to transition to if the dialect is confirmed.
     * @return true if the dialect is confirmed, otherwise false.
     * @throws EDIException when the input text has been rejected by the dialect.
     */
    private boolean dialectConfirmed(State confirmed) throws EDIException {
        if (dialect.isConfirmed()) {
            state = confirmed;
            nextEvent();
            return true;
        } else if (dialect.isRejected()) {
            buffer.clear();
            clearQueues();
            dialect = null;
            state = State.INITIAL;
            throw error(EDIException.INVALID_STATE, "Invalid header segment");
        }

        return false;
    }

    private EDIException invalidStateError() {
        StringBuilder message = new StringBuilder();
        message.append(state);
        message.append(" (previous: ");
        message.append(previous);
        message.append("); input: '");
        message.append((char) input);
        message.append('\'');
        return error(EDIException.INVALID_STATE, message);
    }

    private EDIException error(int code, CharSequence message) {
        Location where = new LocationView(location);
        return new EDIException(code, message.toString(), where);
    }

    private EDIException error(int code) {
        Location where = new LocationView(location);
        return new EDIException(code, where);
    }

    private static void updateLocation(State state, StaEDIStreamLocation location) {
        if (state == State.ELEMENT_REPEAT) {
            if (location.isRepeated()) {
                updateElementOccurrence(location);
            } else {
                location.setElementOccurrence(1);
            }
            location.setRepeated(true);
        } else if (location.isRepeated()) {
            if (state != State.COMPONENT_END) {
                updateElementOccurrence(location);
                location.setRepeated(false);
            }
        } else {
            location.setElementOccurrence(1);
        }

        switch (state) {
        case COMPONENT_END:
        case HEADER_COMPONENT_END:
            location.incrementComponentPosition();
            break;

        default:
            if (location.getComponentPosition() > 0) {
                location.incrementComponentPosition();
            } else if (location.getElementOccurrence() == 1) {
                location.incrementElementPosition();
            }
            break;
        }
    }

    static void updateElementOccurrence(StaEDIStreamLocation location) {
        /*
         * Only increment the position if we have not yet started
         * the composite - i.e, only a single component is present.
         */
        if (location.getComponentPosition() < 1) {
            location.incrementElementOccurrence();
        }
    }

    private boolean nextEvent() {
        Notifier event = events.peek();
        boolean eventsReady = false;

        if (event != null) {
            events.remove();
            State nextState = stateQueue.remove();
            int start = startQueue.remove();
            int length = lengthQueue.remove();
            eventsReady = event.execute(nextState, start, length);
        }

        if (events.isEmpty()) {
            buffer.clear();
        }

        return eventsReady;
    }

    private void enqueue(Notifier task, int position) {
        int start;
        int length;

        if (startQueue.isEmpty()) {
            start = 0;
            length = position;
        } else {
            start = startQueue.peekLast() + lengthQueue.peekLast();
            length = position > 0 ? position - start : 0;
        }

        events.add(task);
        stateQueue.add(this.state);
        startQueue.add(start);
        lengthQueue.add(length);
    }

    private void clearQueues() {
        events.clear();
        stateQueue.clear();
        startQueue.clear();
        lengthQueue.clear();
    }

    private void openInterchange() {
        modes.push(Mode.INTERCHANGE);
        enqueue(isn, 0);
    }

    private void closeInterchange() throws EDIException {
        closeSegment();
        popMode(Mode.INTERCHANGE);
        enqueue(ien, 0);
    }

    private void openSegment() {
        modes.push(Mode.SEGMENT);
        enqueue(ssn, buffer.position());
    }

    private void closeSegment() throws EDIException {
        handleElement();
        popMode(Mode.SEGMENT);
        enqueue(sen, 0);
    }

    private void emptySegment() throws EDIException {
        openSegment();
        popMode(Mode.SEGMENT);
        enqueue(sen, 0);
    }

    private void handleElement() throws EDIException {
        if (previous != State.ELEMENT_END_BINARY) {
            addElementEvent();
        }

        if (inComposite()) {
            closeComposite();
        }
    }

    private void openComposite() {
        modes.push(Mode.COMPOSITE);
        enqueue(csn, 0);
    }

    private void handleComponent() {
        if (!inComposite()) {
            openComposite();
        }

        addElementEvent();
    }

    private void addElementEvent() {
        enqueue(en, buffer.position());
    }

    private boolean inComposite() {
        return modes.peek() == Mode.COMPOSITE;
    }

    private void closeComposite() throws EDIException {
        popMode(Mode.COMPOSITE);
        enqueue(cen, 0);
    }

    void popMode(Mode expected) throws EDIException {
        if (modes.pop() != expected) {
            throw error(EDIException.INVALID_STATE);
        }
    }
}
