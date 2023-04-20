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

import io.xlate.edi.internal.stream.StaEDIStreamLocation;
import io.xlate.edi.internal.stream.validation.UsageError;
import io.xlate.edi.internal.stream.validation.Validator;
import io.xlate.edi.schema.EDIElementPosition;
import io.xlate.edi.schema.EDILoopType;
import io.xlate.edi.schema.EDIReference;
import io.xlate.edi.schema.EDIType;
import io.xlate.edi.schema.Schema;
import io.xlate.edi.stream.EDIStreamEvent;
import io.xlate.edi.stream.EDIStreamValidationError;
import io.xlate.edi.stream.Location;

public class ProxyEventHandler implements EventHandler {

    public static final String LOOP_CODE_GROUP = EDIType.Type.GROUP.toString();
    public static final String LOOP_CODE_TRANSACTION = EDIType.Type.TRANSACTION.toString();

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

    private final Queue<StreamEvent> eventPool = new LinkedList<>();
    // Use implementation to access as both Deque & List
    private final LinkedList<StreamEvent> eventQueue = new LinkedList<>();

    private final Deque<HierarchicalLevel> openLevels = new LinkedList<>();

    static class HierarchicalLevel {
        final String id;
        final StreamEvent event;

        HierarchicalLevel(String id, StreamEvent event) {
            this.id = id;
            this.event = event;
        }

        boolean isParentOf(String parentId) {
            return !parentId.isEmpty() && parentId.equals(id);
        }
    }

    private boolean levelCheckPending;
    private StreamEvent currentSegmentBegin;
    private StreamEvent startedLevel;
    private EDIElementPosition levelIdPosition;
    private String startedLevelId;
    private EDIElementPosition parentIdPosition;
    private String startedLevelParentId;

    private Dialect dialect;

    public ProxyEventHandler(StaEDIStreamLocation location, Schema controlSchema, boolean nestHierarchicalLoops) {
        this.location = location;
        this.nestHierarchicalLoops = nestHierarchicalLoops;

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
        return current(StreamEvent::getType, null);
    }

    public CharBuffer getCharacters() {
        return current(StreamEvent::getData, null);
    }

    public boolean hasNext() {
        // Current event is in the first position, second will be used for `nextEvent`
        return eventQueue.size() > 1;
    }

    public boolean nextEvent() {
        if (eventQueue.isEmpty()) {
            return false;
        }

        eventPool.add(eventQueue.removeFirst());
        return !eventQueue.isEmpty();
    }

    public EDIStreamValidationError getErrorType() {
        return current(StreamEvent::getErrorType, null);
    }

    public String getReferenceCode() {
        return current(StreamEvent::getReferenceCode, null);
    }

    public StaEDIStreamLocation getLocation() {
        return current(StreamEvent::getLocation, this.location);
    }

    <T> T current(Function<StreamEvent, T> mapper, T defaultValue) {
        final T value;

        if (eventQueue.isEmpty()) {
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
        return current(StreamEvent::getTypeReference, null);
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
            validator.reset();
        }

        enqueueEvent(EDIStreamEvent.END_INTERCHANGE, EDIStreamValidationError.NONE, "", null, location);
    }

    @Override
    public void loopBegin(EDIReference typeReference) {
        final String loopCode = typeReference.getReferencedType().getCode();

        if (LOOP_CODE_TRANSACTION.equals(loopCode)) {
            transaction = true;
            transactionSchemaAllowed = true;
            enqueueEvent(EDIStreamEvent.START_TRANSACTION, EDIStreamValidationError.NONE, null, typeReference, location);
            if (transactionValidator != null) {
                transactionValidator.reset();
            }
        } else if (LOOP_CODE_GROUP.equals(loopCode)) {
            enqueueEvent(EDIStreamEvent.START_GROUP, EDIStreamValidationError.NONE, null, typeReference, location);
        } else {
            enqueueEvent(EDIStreamEvent.START_LOOP, EDIStreamValidationError.NONE, null, typeReference, location);

            if (nestHierarchicalLoops && isHierarchicalLoop(typeReference.getReferencedType())) {
                EDILoopType loop = (EDILoopType) typeReference.getReferencedType();
                startedLevel = eventQueue.getLast();
                levelIdPosition = loop.getLevelIdPosition();
                parentIdPosition = loop.getParentIdPosition();
                levelCheckPending = true;
            }
        }
    }

    @Override
    public void loopEnd(EDIReference typeReference) {
        final String loopCode = typeReference.getReferencedType().getCode();

        // Validator can not be null when a loopEnd event has been signaled.
        validator().validateLoopSyntax(this);

        if (LOOP_CODE_TRANSACTION.equals(loopCode)) {
            transaction = false;
            dialect.transactionEnd();
            enqueueEvent(EDIStreamEvent.END_TRANSACTION, EDIStreamValidationError.NONE, null, typeReference, location);
        } else if (LOOP_CODE_GROUP.equals(loopCode)) {
            dialect.groupEnd();
            enqueueEvent(EDIStreamEvent.END_GROUP, EDIStreamValidationError.NONE, null, typeReference, location);
        } else if (nestHierarchicalLoops && isHierarchicalLoop(typeReference.getReferencedType())) {
            levelCheckPending = true;
        } else {
            enqueueEvent(EDIStreamEvent.END_LOOP, EDIStreamValidationError.NONE, null, typeReference, location);
        }
    }

    @Override
    public boolean segmentBegin(String segmentTag) {
        location.incrementSegmentPosition(segmentTag);
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

        if (controlValidator != null) {
            controlValidator.countSegment(segmentTag);
        }

        enqueueEvent(EDIStreamEvent.START_SEGMENT, EDIStreamValidationError.NONE, segmentTag, typeReference, location);
        currentSegmentBegin = eventQueue.getLast();
        return !levelCheckPending && eventsReady;
    }

    boolean exitTransaction(CharSequence tag) {
        return transaction
                && !transactionSchemaAllowed
                && controlSchema != null
                && controlSchema.containsSegment(tag.toString());
    }

    @Override
    public boolean segmentEnd() {
        Validator validator = validator();
        EDIReference typeReference = null;

        if (validator != null) {
            validator.clearImplementationCandidates(this);
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

    @Override
    public boolean compositeBegin(boolean isNil, boolean derived) {
        if (!derived) {
            location.incrementElement(true);
        }
        location.setComposite(true);

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
    public boolean elementData(CharSequence text, boolean fromStream) {
        location.incrementElement(false);

        boolean derivedComposite;
        EDIReference typeReference;
        final boolean compositeFromStream = location.getComponentPosition() > -1;

        dialect.elementData(text, location);
        Validator validator = validator();
        boolean valid;

        if (levelCheckPending && startedLevel != null) {
            setLevelIdentifiers(text);
        }

        /*
         * The first component of a composite was the only element received
         * for the composite. It was found to be a composite via the schema
         * and the composite begin/end events must be generated.
         **/
        final boolean componentReceivedAsSimple;
        final List<UsageError> errors;

        if (validator != null) {
            derivedComposite = !compositeFromStream && validator.isComposite(dialect, location);
            componentReceivedAsSimple = derivedComposite && fromStream;

            if (componentReceivedAsSimple) {
                this.compositeBegin(text.length() == 0, true);
                location.incrementComponentPosition();
            }

            valid = validator.validateElement(dialect, location, text, null);
            typeReference = validator.getElementReference();
            errors = validator.getElementErrors();
            enqueueElementOccurrenceErrors(text, validator, valid);
        } else {
            errors = Validator.validateCharacters(text);
            valid = errors.isEmpty();
            derivedComposite = false;
            componentReceivedAsSimple = false;
            typeReference = null;
        }

        enqueueElementErrors(text, errors, valid);

        boolean eventsReady = true;

        if (fromStream && (!derivedComposite || text.length() > 0) /* Not an inferred element */) {
            enqueueEvent(EDIStreamEvent.ELEMENT_DATA,
                         EDIStreamValidationError.NONE,
                         text,
                         typeReference,
                         location);

            eventsReady = selectImplementationIfPending(validator, eventsReady);
        }

        if (componentReceivedAsSimple) {
            this.compositeEnd(text.length() == 0);
            location.clearComponentPosition();
        }

        return !levelCheckPending && eventsReady;
    }

    boolean selectImplementationIfPending(Validator validator, boolean eventsReadyDefault) {
        if (validator != null && validator.isPendingDiscrimination()) {
            return validator.selectImplementation(eventQueue, this);
        }

        return eventsReadyDefault;
    }

    void clearLevelCheck() {
        levelCheckPending = false;
        currentSegmentBegin = null;
        startedLevel = null;

        levelIdPosition = null;
        startedLevelId = "";

        parentIdPosition = null;
        startedLevelParentId = "";
    }

    boolean isHierarchicalLoop(EDIType type) {
        EDILoopType loop = (EDILoopType) type;

        return loop.getLevelIdPosition() != null &&
                loop.getParentIdPosition() != null;
    }

    void setLevelIdentifiers(CharSequence text) {
        if (levelIdPosition.matchesLocation(location)) {
            startedLevelId = text.toString();
        }

        if (parentIdPosition.matchesLocation(location)) {
            startedLevelParentId = text.toString();
        }
    }

    void performLevelCheck() {
        if (startedLevel != null) {
            completeLevel(startedLevel, startedLevelParentId);

            StreamEvent openLevel = getPooledEvent();
            openLevel.type = EDIStreamEvent.END_LOOP;
            openLevel.errorType = startedLevel.errorType;
            openLevel.setData(startedLevel.data);
            openLevel.setTypeReference(startedLevel.typeReference);
            openLevel.setLocation(startedLevel.location);

            /*
             * startedLevelId will not be null due to Validator#validateSyntax.
             * Although the client never sees the generated element event, here
             * it will be an empty string.
             */
            openLevels.addLast(new HierarchicalLevel(startedLevelId, openLevel));
        } else {
            completeLevel(currentSegmentBegin, "");
        }

        clearLevelCheck();
    }

    void completeLevel(StreamEvent successor, String parentId) {
        while (!openLevels.isEmpty() && !openLevels.getLast().isParentOf(parentId)) {
            HierarchicalLevel completed = openLevels.removeLast();
            completed.event.location.set(location);
            completed.event.location.clearSegmentLocations();

            eventQueue.add(eventQueue.indexOf(successor), completed.event);
        }
    }

    void enqueueElementOccurrenceErrors(CharSequence text, Validator validator, boolean valid) {
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
                             text,
                             error.getTypeReference(),
                             location);
                cursor.remove();
                //$FALL-THROUGH$
            default:
                continue;
            }
        }
    }

    void enqueueElementErrors(CharSequence text, List<UsageError> errors, boolean valid) {
        if (valid) {
            return;
        }

        for (UsageError error : errors) {
            enqueueEvent(error.getError().getCategory(),
                         error.getError(),
                         text,
                         error.getTypeReference(),
                         location);
        }
    }

    public boolean isBinaryElementLength() {
        return validator() != null && validator().isBinaryElementLength();
    }

    @Override
    public boolean binaryData(InputStream binaryStream) {
        location.incrementElement(false);
        Validator validator = validator();
        EDIReference typeReference = validator != null ? validator.getElementReference() : null;
        enqueueEvent(EDIStreamEvent.ELEMENT_DATA_BINARY, EDIStreamValidationError.NONE, "", typeReference, location);
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

        target.type = event;
        target.errorType = error;
        target.setData(data);
        target.setTypeReference(typeReference);
        target.setLocation(location);

        eventQueue.add(target);
    }

}
