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

import java.io.InputStream;
import java.nio.CharBuffer;
import java.util.Deque;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Queue;
import java.util.function.Function;

import io.xlate.edi.internal.stream.CharArraySequence;
import io.xlate.edi.internal.stream.StaEDIStreamLocation;
import io.xlate.edi.internal.stream.validation.UsageError;
import io.xlate.edi.internal.stream.validation.Validator;
import io.xlate.edi.schema.EDIReference;
import io.xlate.edi.schema.EDIType;
import io.xlate.edi.schema.Schema;
import io.xlate.edi.stream.EDIStreamEvent;
import io.xlate.edi.stream.EDIStreamValidationError;
import io.xlate.edi.stream.Location;

public class ProxyEventHandler implements EventHandler {

    private final StaEDIStreamLocation location;
    private final boolean nestHierarchicalLoops;

    private Schema controlSchema;
    private Validator controlValidator;

    private Schema transactionSchema;
    private Validator transactionValidator;

    private boolean transactionSchemaAllowed = false;
    private boolean transaction = false;

    private InputStream binary;
    private String segmentTag;
    private CharArraySequence elementHolder = new CharArraySequence();

    private final Queue<StreamEvent> eventPool = new LinkedList<>();
    private final Deque<StreamEvent> eventQueue;
    private final List<StreamEvent> eventList;

    private final Deque<HierarchicalLevel> openLevels = new LinkedList<>();

    static class HierarchicalLevel {
        final String id;
        final StreamEvent event;

        HierarchicalLevel(String id, StreamEvent event) {
            this.id = id;
            this.event = event;
        }
    }

    private boolean levelCheckPending;
    private StreamEvent currentSegmentBegin;
    private StreamEvent startedLevel;
    private String startedLevelId;
    private String startedLevelParentId;

    private Dialect dialect;

    public ProxyEventHandler(StaEDIStreamLocation location, Schema controlSchema, boolean nestHierarchicalLoops) {
        this.location = location;
        this.nestHierarchicalLoops = nestHierarchicalLoops;

        LinkedList<StreamEvent> events = new LinkedList<>();
        this.eventQueue = events;
        this.eventList = events;

        setControlSchema(controlSchema, true);
    }

    public void setControlSchema(Schema controlSchema, boolean validateCodeValues) {
        if (controlValidator != null) {
            throw new IllegalStateException("control validator already created");
        }

        this.controlSchema = controlSchema;
        controlValidator = Validator.forSchema(controlSchema, null, validateCodeValues, false);
    }

    public boolean isTransactionSchemaAllowed() {
        return transactionSchemaAllowed;
    }

    public Schema getTransactionSchema() {
        return this.transactionSchema;
    }

    public void setTransactionSchema(Schema transactionSchema) {
        if (!Objects.equals(this.transactionSchema, transactionSchema)) {
            this.transactionSchema = transactionSchema;
            transactionValidator = Validator.forSchema(transactionSchema, controlSchema, true, false);
        }
    }

    public void resetEvents() {
        eventPool.addAll(eventQueue);
        eventQueue.clear();
    }

    public EDIStreamEvent getEvent() {
        return current(StreamEvent::getType, false, null);
    }

    public CharBuffer getCharacters() {
        return current(StreamEvent::getData, true, null);
    }

    public boolean nextEvent() {
        if (eventQueue.isEmpty()) {
            return false;
        }

        eventPool.add(eventQueue.removeFirst());
        return !eventQueue.isEmpty();
    }

    public EDIStreamValidationError getErrorType() {
        return current(StreamEvent::getErrorType, false, null);
    }

    public String getReferenceCode() {
        return current(StreamEvent::getReferenceCode, false, null);
    }

    public Location getLocation() {
        return current(StreamEvent::getLocation, false, this.location);
    }

    <T> T current(Function<StreamEvent, T> mapper, boolean required, T defaultValue) {
        final T value;

        if (eventQueue.isEmpty()) {
            if (required) {
                throw new IllegalStateException();
            }
            value = defaultValue;
        } else {
            value = mapper.apply(eventQueue.getFirst());
        }

        return value;
    }

    StreamEvent getPooledEvent() {
        return eventPool.isEmpty() ? new StreamEvent() : eventPool.remove();
    }

    public InputStream getBinary() {
        return binary;
    }

    public void setBinary(InputStream binary) {
        this.binary = binary;
    }

    public EDIReference getSchemaTypeReference() {
        return current(StreamEvent::getTypeReference, false, null);
    }

    @Override
    public void interchangeBegin(Dialect dialect) {
        this.dialect = dialect;
        enqueueEvent(EDIStreamEvent.START_INTERCHANGE, EDIStreamValidationError.NONE, "", null, location);
    }

    @Override
    public void interchangeEnd() {
        Validator validator = validator();

        if (validator != null) {
            validator.validateLoopSyntax(this);
        }

        enqueueEvent(EDIStreamEvent.END_INTERCHANGE, EDIStreamValidationError.NONE, "", null, location);
    }

    @Override
    public void loopBegin(EDIReference typeReference) {
        final String loopCode = typeReference.getReferencedType().getCode();

        if (EDIType.Type.TRANSACTION.toString().equals(loopCode)) {
            transaction = true;
            transactionSchemaAllowed = true;
            enqueueEvent(EDIStreamEvent.START_TRANSACTION, EDIStreamValidationError.NONE, loopCode, typeReference, location);
            if (transactionValidator != null) {
                transactionValidator.reset();
            }
        } else if (EDIType.Type.GROUP.toString().equals(loopCode)) {
            enqueueEvent(EDIStreamEvent.START_GROUP, EDIStreamValidationError.NONE, loopCode, typeReference, location);
        } else {
            enqueueEvent(EDIStreamEvent.START_LOOP, EDIStreamValidationError.NONE, loopCode, typeReference, location);

            if (nestHierarchicalLoops && dialect.isHierarchicalLoop(typeReference.getReferencedType())) {
                startedLevel = eventQueue.getLast();
                levelCheckPending = true;
            }
        }
    }

    @Override
    public void loopEnd(EDIReference typeReference) {
        final String loopCode = typeReference.getReferencedType().getCode();

        // Validator can not be null when a loopEnd event has been signaled.
        validator().validateLoopSyntax(this);

        if (EDIType.Type.TRANSACTION.toString().equals(loopCode)) {
            transaction = false;
            dialect.transactionEnd();
            enqueueEvent(EDIStreamEvent.END_TRANSACTION, EDIStreamValidationError.NONE, loopCode, typeReference, location);
        } else if (EDIType.Type.GROUP.toString().equals(loopCode)) {
            dialect.groupEnd();
            enqueueEvent(EDIStreamEvent.END_GROUP, EDIStreamValidationError.NONE, loopCode, typeReference, location);
        } else if (nestHierarchicalLoops && dialect.isHierarchicalLoop(typeReference.getReferencedType())) {
            levelCheckPending = true;
        } else {
            enqueueEvent(EDIStreamEvent.END_LOOP, EDIStreamValidationError.NONE, loopCode, typeReference, location);
        }
    }

    @Override
    public boolean segmentBegin(String segmentTag) {
        this.segmentTag = segmentTag;

        /*
         * If this is the start of a transaction, loopStart will be called from the validator and
         * transactionSchemaAllowed will be `true` for the duration of the start-transaction segment.
         */
        transactionSchemaAllowed = false;
        Validator validator = validator();
        boolean eventsReady = true;
        EDIReference typeReference = null;
        clearLevelCheck();

        if (validator != null && !dialect.isServiceAdviceSegment(segmentTag)) {
            validator.validateSegment(this, segmentTag);
            typeReference = validator.getSegmentReference();
            eventsReady = !validator.isPendingDiscrimination();
        }

        if (exitTransaction(segmentTag)) {
            // Validate the syntax for the elements directly within the transaction loop
            if (validator != null) {
                validator.validateLoopSyntax(this);
            }

            transaction = false;

            // Now the control validator after setting transaction to false
            validator = validator();
            validator.validateSegment(this, segmentTag);
            typeReference = validator().getSegmentReference();
        }

        enqueueEvent(EDIStreamEvent.START_SEGMENT, EDIStreamValidationError.NONE, segmentTag, typeReference, location);
        currentSegmentBegin = eventQueue.getLast();
        return !levelCheckPending && eventsReady;
    }

    boolean exitTransaction(CharSequence tag) {
        return transaction && !transactionSchemaAllowed && controlSchema != null
                && controlSchema.containsSegment(tag.toString());
    }

    @Override
    public boolean segmentEnd() {
        Validator validator = validator();
        EDIReference typeReference = null;

        if (validator != null) {
            validator.validateSyntax(dialect, this, this, location, false);
            validator.validateVersionConstraints(dialect, this, null);
            typeReference = validator.getSegmentReference();
        }

        if (levelCheckPending) {
            performLevelCheck();
        }

        location.clearSegmentLocations();
        enqueueEvent(EDIStreamEvent.END_SEGMENT, EDIStreamValidationError.NONE, segmentTag, typeReference, location);
        return true;
    }

    void clearLevelCheck() {
        levelCheckPending = false;
        startedLevel = null;
        startedLevelId = null;
        startedLevelParentId = null;
    }

    void setLevelIdentifiers() {
        if (dialect.isHierarchicalId(location)) {
            startedLevelId = elementHolder.toString();
        }

        if (dialect.isHierarchicalParentId(location)) {
            startedLevelParentId = elementHolder.toString();
        }
    }

    void performLevelCheck() {
        if (startedLevel != null) {
            StreamEvent bookmark = getPooledEvent();
            bookmark.type = EDIStreamEvent.END_LOOP;
            bookmark.errorType = startedLevel.errorType;
            bookmark.setData(startedLevel.data);
            bookmark.setTypeReference(startedLevel.typeReference);
            bookmark.setLocation(startedLevel.location);

            HierarchicalLevel started = new HierarchicalLevel(startedLevelId, bookmark);

            while (!openLevels.isEmpty() && !openLevels.getLast().id.equals(startedLevelParentId)) {
                HierarchicalLevel completed = openLevels.removeLast();
                completed.event.location.set(location);
                completed.event.location.clearSegmentLocations();
                int position = eventList.indexOf(startedLevel);
                eventList.add(position, completed.event);
            }

            openLevels.addLast(started);
        } else {
            while (!openLevels.isEmpty()) {
                HierarchicalLevel completed = openLevels.removeLast();
                completed.event.location.set(location);
                completed.event.location.clearSegmentLocations();
                int position = this.eventList.indexOf(this.currentSegmentBegin);
                eventList.add(position, completed.event);
            }
        }

        clearLevelCheck();
    }

    @Override
    public boolean compositeBegin(boolean isNil) {
        EDIReference typeReference = null;
        boolean eventsReady = true;
        Validator validator = validator();

        if (validator != null && !isNil) {
            boolean invalid = !validator.validCompositeOccurrences(dialect, location);
            typeReference = validator.getCompositeReference();

            if (invalid) {
                List<UsageError> errors = validator.getElementErrors();

                for (UsageError error : errors) {
                    enqueueEvent(error.getError().getCategory(), error.getError(), "", error.getTypeReference(), location);
                }
            }

            eventsReady = !validator.isPendingDiscrimination();
        }

        enqueueEvent(EDIStreamEvent.START_COMPOSITE, EDIStreamValidationError.NONE, "", typeReference, location);
        return !levelCheckPending && eventsReady;
    }

    @Override
    public boolean compositeEnd(boolean isNil) {
        boolean eventsReady = true;

        if (validator() != null && !isNil) {
            validator().validateSyntax(dialect, this, this, location, true);
            eventsReady = !validator().isPendingDiscrimination();
        }

        location.clearComponentPosition();
        enqueueEvent(EDIStreamEvent.END_COMPOSITE, EDIStreamValidationError.NONE, "", null, location);
        return !levelCheckPending && eventsReady;
    }

    @Override
    public boolean elementData(char[] text, int start, int length) {
        boolean derivedComposite;
        EDIReference typeReference;
        boolean eventsReady = true;
        final boolean compositeFromStream = location.getComponentPosition() > -1;

        elementHolder.set(text, start, length);
        dialect.elementData(elementHolder, location);
        Validator validator = validator();
        boolean valid;

        if (levelCheckPending) {
            setLevelIdentifiers();
        }

        if (validator != null) {
            valid = validator.validateElement(dialect, location, elementHolder, null);
            derivedComposite = !compositeFromStream && validator.isComposite();
            typeReference = validator.getElementReference();
            enqueueElementOccurrenceErrors(validator, valid);
        } else {
            valid = true;
            derivedComposite = false;
            typeReference = null;
        }

        if (derivedComposite && elementHolder.getText() != null/* Not an empty composite */) {
            this.compositeBegin(elementHolder.length() == 0);
            location.incrementComponentPosition();
        }

        enqueueElementErrors(validator, valid);

        if (text != null && (!derivedComposite || length > 0) /* Not an inferred element */) {
            enqueueEvent(EDIStreamEvent.ELEMENT_DATA,
                         EDIStreamValidationError.NONE,
                         elementHolder,
                         typeReference,
                         location);

            if (validator != null && validator.isPendingDiscrimination()) {
                eventsReady = validator.selectImplementation(eventQueue, this);
            }
        }

        if (derivedComposite && text != null /* Not an empty composite */) {
            this.compositeEnd(length == 0);
            location.clearComponentPosition();
        }

        return !levelCheckPending && eventsReady;
    }

    void enqueueElementOccurrenceErrors(Validator validator, boolean valid) {
        if (valid) {
            return;
        }

        /*
         * Process element-level errors before possibly starting a
         * composite or reporting other data-related errors.
         */
        List<UsageError> errors = validator.getElementErrors();
        Iterator<UsageError> cursor = errors.iterator();

        while (cursor.hasNext()) {
            UsageError error = cursor.next();

            switch (error.getError()) {
            case TOO_MANY_DATA_ELEMENTS:
            case TOO_MANY_REPETITIONS:
                enqueueEvent(error.getError().getCategory(),
                             error.getError(),
                             elementHolder,
                             error.getTypeReference(),
                             location);
                cursor.remove();
                //$FALL-THROUGH$
            default:
                continue;
            }
        }
    }

    void enqueueElementErrors(Validator validator, boolean valid) {
        if (valid) {
            return;
        }

        List<UsageError> errors = validator.getElementErrors();

        for (UsageError error : errors) {
            enqueueEvent(error.getError().getCategory(),
                         error.getError(),
                         elementHolder,
                         error.getTypeReference(),
                         location);
        }
    }

    public boolean isBinaryElementLength() {
        return validator() != null && validator().isBinaryElementLength();
    }

    @Override
    public boolean binaryData(InputStream binaryStream) {
        enqueueEvent(EDIStreamEvent.ELEMENT_DATA_BINARY, EDIStreamValidationError.NONE, "", null, location);
        setBinary(binaryStream);
        return true;
    }

    @Override
    public void segmentError(CharSequence token, EDIReference typeReference, EDIStreamValidationError error) {
        enqueueEvent(EDIStreamEvent.SEGMENT_ERROR, error, token, typeReference, location);
    }

    @Override
    public void elementError(final EDIStreamEvent event,
                             final EDIStreamValidationError error,
                             final EDIReference typeReference,
                             final CharSequence data,
                             final int element,
                             final int component,
                             final int repetition) {

        StaEDIStreamLocation copy = location.copy();
        copy.setElementPosition(element);
        copy.setElementOccurrence(repetition);
        copy.setComponentPosition(component);

        enqueueEvent(event, error, data, typeReference, copy);
    }

    private Validator validator() {
        // Do not use the transactionValidator in the period where it may be set/mutated by the user
        return transaction && !transactionSchemaAllowed ? transactionValidator : controlValidator;
    }

    private void enqueueEvent(EDIStreamEvent event,
                              EDIStreamValidationError error,
                              CharSequence data,
                              EDIReference typeReference,
                              Location location) {

        StreamEvent target = getPooledEvent();
        EDIStreamEvent associatedEvent = eventQueue.isEmpty() ? null : getAssociatedEvent(error);

        if (eventExists(associatedEvent)) {
            /*
             * Ensure segment errors occur before other event types
             * when the array has other events already present.
             */
            int offset = eventQueue.size();
            boolean complete = false;

            while (!complete) {
                StreamEvent enqueuedEvent = eventList.get(offset - 1);

                if (enqueuedEvent.type == associatedEvent) {
                    complete = true;
                } else {
                    if (eventList.size() == offset) {
                        eventList.add(offset, enqueuedEvent);
                    } else {
                        eventList.set(offset, enqueuedEvent);
                    }
                    offset--;
                }
            }

            eventList.set(offset, target);
        } else {
            eventQueue.add(target);
        }

        target.type = event;
        target.errorType = error;
        target.setData(data);
        target.setTypeReference(typeReference);
        target.setLocation(location);
    }

    private boolean eventExists(EDIStreamEvent associatedEvent) {
        int offset = eventQueue.size();

        while (associatedEvent != null && offset > 0) {
            if (eventList.get(offset - 1).type == associatedEvent) {
                return true;
            }
            offset--;
        }

        return false;
    }

    private static EDIStreamEvent getAssociatedEvent(EDIStreamValidationError error) {
        final EDIStreamEvent event;

        switch (error) {
        case IMPLEMENTATION_LOOP_OCCURS_UNDER_MINIMUM_TIMES:
            event = EDIStreamEvent.END_LOOP;
            break;
        case MANDATORY_SEGMENT_MISSING:
        case IMPLEMENTATION_SEGMENT_BELOW_MINIMUM_USE:
            event = null;
            break;
        default:
            event = null;
            break;
        }

        return event;
    }
}
