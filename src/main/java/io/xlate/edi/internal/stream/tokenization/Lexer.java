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
import java.nio.CharBuffer;
import java.util.ArrayDeque;
import java.util.Deque;

import io.xlate.edi.stream.Location;

public class Lexer {

    private enum Mode {
        INTERCHANGE,
        SEGMENT,
        COMPOSITE
    }

    private final Deque<Mode> modes = new ArrayDeque<>();
    private State state = State.INITIAL;
    private State previous;

    private interface Notifier {
        void execute(State state, int start, int length);
    }

    private final Deque<Notifier> events = new ArrayDeque<>(20);
    private final Deque<State> stateQueue = new ArrayDeque<>(20);
    private final Deque<Integer> startQueue = new ArrayDeque<>(20);
    private final Deque<Integer> lengthQueue = new ArrayDeque<>(20);

    private final InputStream stream;
    private final InternalLocation location;

    private CharacterSet characters = new CharacterSet();
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

    public Lexer(InputStream stream, EventHandler handler, InternalLocation location) {
        if (stream.markSupported()) {
            this.stream = stream;
        } else {
            this.stream = new BufferedInputStream(stream);
        }

        this.location = location;

        isn = (notifyState, start, length) -> handler.interchangeBegin();

        ien = (notifyState, start, length) -> {
            handler.interchangeEnd();
            dialect = null;
            characters.reset();
        };

        ssn = (notifyState, start, length) -> {
            location.incrementSegmentPosition();
            handler.segmentBegin(buffer.array(), start, length);
        };

        sen = (notifyState, start, length) -> {
            handler.segmentEnd();
            location.clearSegmentLocations();
        };

        csn = (notifyState, start, length) -> {
            if (location.isRepeated()) {
                location.incrementElementOccurrence();
            } else {
                location.incrementElementPosition();
            }

            handler.compositeBegin(false);
        };

        cen = (notifyState, start, length) -> {
            handler.compositeEnd(false);
            location.clearComponentPosition();
        };

        en = (notifyState, start, length) -> {
            updateLocation(notifyState, location);
            handler.elementData(buffer.array(), start, length);
        };

        bn = (notifyState, start, length) -> {
            updateLocation(notifyState, location);
            handler.binaryData(binaryStream);
        };
    }

    public boolean isInitialized() {
        return state.isInitial() && modes.isEmpty();
    }

    public Dialect getDialect() {
        return dialect;
    }

    public void setBinaryLength(long binaryLength) {
        this.binaryRemain = binaryLength;

        this.binaryStream = new InputStream() {
            @Override
            public int read() throws IOException {
                if (binaryRemain-- < 1) {
                    state = State.ELEMENT_END_BINARY;
                    return -1;
                }

                int input = stream.read();

                if (input != -1) {
                    location.incrementOffset();
                    return input;
                }
                state = State.ELEMENT_END_BINARY;
                return -1;
            }
        };

        enqueue(bn, 0);
        state = State.ELEMENT_DATA_BINARY;
    }

    public void parse() throws IOException, EDIException {
        if (nextEvent()) {
            return;
        }

        CharacterClass clazz;
        int input;
        boolean eventsReady = false;

        while (!eventsReady && (input = stream.read()) > -1) {
            location.incrementOffset();

            clazz = characters.getClass(input);
            previous = state;
            state = state.transition(clazz);

            switch (state) {
            case INITIAL:
            case TAG_SEARCH:
            case HEADER_TAG_SEARCH:
                break;
            case HEADER_TAG_I:
            case HEADER_TAG_N:
            case HEADER_TAG_S:
            case HEADER_TAG_U:
            case TAG_1:
            case TAG_2:
            case TAG_3:
            case TRAILER_TAG_I:
            case TRAILER_TAG_E:
            case TRAILER_TAG_A:
            case TRAILER_TAG_U:
            case TRAILER_TAG_N:
            case TRAILER_TAG_Z:
            case ELEMENT_DATA:
            case ELEMENT_INVALID_DATA:
            case TRAILER_ELEMENT_DATA:
                buffer.put((char) input);
                break;
            case HEADER_TAG_1:
            case HEADER_TAG_2:
            case HEADER_TAG_3:
            	handleStateHeaderTag(input);
                break;
            case DATA_RELEASE:
                // Skip this character - next character will be literal value
                break;
            case ELEMENT_DATA_BINARY:
            	handleStateElementDataBinary();
                break;
            case INTERCHANGE_CANDIDATE:
            	handleStateInterchangeCandidate(input);
                break;
            case HEADER_DATA:
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
                if (clazz != CharacterClass.INVALID) {
                    StringBuilder message = new StringBuilder();
                    message.append(": ");
                    message.append(state);
                    message.append(" (previous: ");
                    message.append(previous);
                    message.append("); input: '");
                    message.append((char) input);
                    message.append('\'');
                    error(EDIException.INVALID_STATE, message);
                } else {
                    error(EDIException.INVALID_CHARACTER);
                }
            }
        }
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
    	stream.mark(500);
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

        if (characters.isDelimiter(input)) {
            if (characters.getDelimiter(CharacterClass.SEGMENT_DELIMITER) == input) {
                closeSegment();
                state = State.HEADER_TAG_SEARCH;
            }
        } else if (!characters.isRelease(input) && dialect.getDecimalMark() != input) {
            buffer.put((char) input);
        }
    }

    private boolean dialectConfirmed(State confirmed) throws IOException {
        if (dialect.isConfirmed()) {
            state = confirmed;
            nextEvent();
            return true;
        } else if (dialect.isRejected()) {
            stream.reset();
            buffer.clear();
            clearQueues();
            state = State.INITIAL;
        }

        return false;
    }

    private void error(int code, CharSequence message) throws EDIException {
        Location where = new ImmutableLocation(location);
        throw new EDIException(code, message.toString(), where);
    }

    private void error(int code) throws EDIException {
        Location where = new ImmutableLocation(location);
        throw new EDIException(code, where);
    }

    private static void updateLocation(State state, InternalLocation location) {
        if (state == State.ELEMENT_REPEAT) {
            if (location.isRepeated()) {
                location.incrementElementOccurrence();
            } else {
                location.setElementOccurrence(1);
            }
            location.setRepeated(true);
        } else if (location.isRepeated()) {
            if (state != State.COMPONENT_END) {
                /*
                 * Only increment the position if we have not yet started
                 * the composite - i.e, only a single component is present.
                 */
                if (location.getComponentPosition() < 1) {
                    location.incrementElementOccurrence();
                }

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

    private boolean nextEvent() {
        Notifier event = events.peek();

        if (event != null) {
            events.remove();
            State nextState = stateQueue.remove();
            int start = startQueue.remove();
            int length = lengthQueue.remove();

            event.execute(nextState, start, length);
            return true;
        }

        buffer.clear();
        return false;
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
        if (modes.pop() != Mode.INTERCHANGE) {
            error(EDIException.INVALID_STATE);
        }
        enqueue(ien, 0);
    }

    private void openSegment() {
        modes.push(Mode.SEGMENT);
        enqueue(ssn, buffer.position());
    }

    private void closeSegment() throws EDIException {
        handleElement();
        if (modes.pop() != Mode.SEGMENT) {
            error(EDIException.INVALID_STATE);
        }
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
        if (modes.pop() != Mode.COMPOSITE) {
            error(EDIException.INVALID_STATE);
        }
        enqueue(cen, 0);
    }
}
