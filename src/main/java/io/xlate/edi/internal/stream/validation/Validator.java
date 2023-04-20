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
package io.xlate.edi.internal.stream.validation;

import static io.xlate.edi.stream.EDIStreamValidationError.IMPLEMENTATION_INVALID_CODE_VALUE;
import static io.xlate.edi.stream.EDIStreamValidationError.IMPLEMENTATION_LOOP_OCCURS_UNDER_MINIMUM_TIMES;
import static io.xlate.edi.stream.EDIStreamValidationError.IMPLEMENTATION_SEGMENT_BELOW_MINIMUM_USE;
import static io.xlate.edi.stream.EDIStreamValidationError.IMPLEMENTATION_TOO_FEW_REPETITIONS;
import static io.xlate.edi.stream.EDIStreamValidationError.IMPLEMENTATION_UNUSED_DATA_ELEMENT_PRESENT;
import static io.xlate.edi.stream.EDIStreamValidationError.IMPLEMENTATION_UNUSED_SEGMENT_PRESENT;
import static io.xlate.edi.stream.EDIStreamValidationError.INVALID_CODE_VALUE;
import static io.xlate.edi.stream.EDIStreamValidationError.LOOP_OCCURS_OVER_MAXIMUM_TIMES;
import static io.xlate.edi.stream.EDIStreamValidationError.MANDATORY_SEGMENT_MISSING;
import static io.xlate.edi.stream.EDIStreamValidationError.REQUIRED_DATA_ELEMENT_MISSING;
import static io.xlate.edi.stream.EDIStreamValidationError.SEGMENT_EXCEEDS_MAXIMUM_USE;
import static io.xlate.edi.stream.EDIStreamValidationError.SEGMENT_NOT_IN_DEFINED_TRANSACTION_SET;
import static io.xlate.edi.stream.EDIStreamValidationError.SEGMENT_NOT_IN_PROPER_SEQUENCE;
import static io.xlate.edi.stream.EDIStreamValidationError.TOO_MANY_COMPONENTS;
import static io.xlate.edi.stream.EDIStreamValidationError.TOO_MANY_DATA_ELEMENTS;
import static io.xlate.edi.stream.EDIStreamValidationError.TOO_MANY_REPETITIONS;
import static io.xlate.edi.stream.EDIStreamValidationError.UNEXPECTED_SEGMENT;

import java.nio.CharBuffer;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Queue;
import java.util.logging.Logger;

import io.xlate.edi.internal.schema.StaEDISchema;
import io.xlate.edi.internal.stream.StaEDIStreamLocation;
import io.xlate.edi.internal.stream.tokenization.Dialect;
import io.xlate.edi.internal.stream.tokenization.ElementDataHandler;
import io.xlate.edi.internal.stream.tokenization.StreamEvent;
import io.xlate.edi.internal.stream.tokenization.ValidationEventHandler;
import io.xlate.edi.schema.EDIComplexType;
import io.xlate.edi.schema.EDIControlType;
import io.xlate.edi.schema.EDIReference;
import io.xlate.edi.schema.EDISimpleType;
import io.xlate.edi.schema.EDISyntaxRule;
import io.xlate.edi.schema.EDIType;
import io.xlate.edi.schema.EDIType.Type;
import io.xlate.edi.schema.Schema;
import io.xlate.edi.schema.implementation.CompositeImplementation;
import io.xlate.edi.schema.implementation.Discriminator;
import io.xlate.edi.schema.implementation.EDITypeImplementation;
import io.xlate.edi.schema.implementation.LoopImplementation;
import io.xlate.edi.schema.implementation.PolymorphicImplementation;
import io.xlate.edi.schema.implementation.SegmentImplementation;
import io.xlate.edi.stream.EDIStreamEvent;
import io.xlate.edi.stream.EDIStreamValidationError;
import io.xlate.edi.stream.Location;

public class Validator {

    static final Logger LOGGER = Logger.getLogger(Validator.class.getName());
    // Versions are not yet supported for segments
    static final String SEGMENT_VERSION = "";

    private Schema containerSchema;
    private Schema schema;
    private final boolean validateCodeValues;
    private final boolean formatElements;
    private boolean initial = true;

    final UsageNode root;
    final UsageNode implRoot;

    private boolean segmentExpected;
    private UsageNode segment;
    private UsageNode correctSegment;
    private UsageNode composite;
    private UsageNode element;
    private Queue<RevalidationNode> revalidationQueue = new LinkedList<>();

    private boolean implSegmentSelected;
    private UsageNode implNode;
    private UsageNode implComposite;
    private UsageNode implElement;
    private List<UsageNode> implSegmentCandidates = new ArrayList<>();

    private final Deque<UsageNode> loopStack = new ArrayDeque<>();

    private final List<UsageError> useErrors = new ArrayList<>();
    private final List<UsageError> elementErrors = new ArrayList<>(5);

    private int depth = 1;
    private final UsageCursor cursor = new UsageCursor();

    static class UsageCursor {
        UsageNode standard;
        UsageNode impl;

        boolean hasNextSibling() {
            return standard.getNextSibling() != null;
        }

        void next(UsageNode nextImpl) {
            standard = standard.getNextSibling();
            if (nextImpl != null) {
                impl = nextImpl;
            }
        }

        void nagivateUp(int limit) {
            standard = standard.getParent();

            if (impl != null && impl.getDepth() > limit) {
                impl = impl.getParent().getFirstSiblingSameType();
            }
        }

        void reset(UsageNode root, UsageNode implRoot) {
            standard = UsageNode.getFirstChild(root);
            impl = UsageNode.getFirstChild(implRoot);
        }

        UsageCursor copy() {
            UsageCursor copy = new UsageCursor();
            copy.standard = this.standard;
            copy.impl = this.impl;
            return copy;
        }
    }

    static class RevalidationNode {
        final UsageNode standard;
        final UsageNode impl;
        final CharBuffer data;
        final StaEDIStreamLocation location;

        public RevalidationNode(UsageNode standard, UsageNode impl, CharSequence data, StaEDIStreamLocation location) {
            this.standard = standard;
            this.impl = impl;
            this.data = CharBuffer.allocate(data.length());
            this.data.append(data);
            this.data.flip();
            this.location = location.copy();
        }

        public RevalidationNode(UsageNode standard, UsageNode impl, StaEDIStreamLocation location) {
            this.standard = standard;
            this.impl = impl;
            this.data = null;
            this.location = location.copy();
        }

    }

    public static Validator forSchema(Schema schema, Schema containerSchema, boolean validateCodeValues, boolean formatElements) {
        final Validator instance;

        if (schema != null) {
            instance = new Validator(schema, containerSchema, validateCodeValues, formatElements);
        } else {
            instance = null;
        }

        return instance;
    }

    public Validator(Schema schema, Schema containerSchema, boolean validateCodeValues) {
        this(schema, containerSchema, validateCodeValues, false);
    }

    public Validator(Schema schema, Schema containerSchema, boolean validateCodeValues, boolean formatElements) {
        this.schema = schema;
        this.validateCodeValues = validateCodeValues;
        this.formatElements = formatElements;
        this.containerSchema = containerSchema;

        LOGGER.finer(() -> "Creating usage tree");
        root = buildTree(schema.getStandard());
        LOGGER.finer(() -> "Done creating usage tree");
        correctSegment = segment = root.getFirstChild();

        if (schema.getImplementation() != null) {
            implRoot = buildImplTree(null, 0, schema.getImplementation(), -1);
            implNode = implRoot.getFirstChild();
        } else {
            implRoot = null;
            implNode = null;
        }
    }

    public void reset() {
        if (initial) {
            return;
        }

        root.reset();
        correctSegment = segment = root.getFirstChild();

        if (implRoot != null) {
            implRoot.reset();
            implNode = implRoot.getFirstChild();
        }

        cursor.reset(root, implRoot);
        depth = 1;

        segmentExpected = false;
        implSegmentSelected = false;
        clearElements();

        implSegmentCandidates.clear();
        useErrors.clear();
        elementErrors.clear();
        initial = true;
    }

    void clearElements() {
        composite = null;
        element = null;

        implComposite = null;
        implElement = null;
    }

    public boolean isPendingDiscrimination() {
        return !implSegmentCandidates.isEmpty();
    }

    public EDIReference getSegmentReference() {
        final EDIReference reference;

        if (implSegmentSelected) {
            reference = implNode.getLink();
        } else if (segmentExpected) {
            reference = segment.getLink();
        } else  {
            reference = null;
        }

        return reference;
    }

    public EDIReference getCompositeReference() {
        final EDIReference reference;

        if (implSegmentSelected && implComposite != null) {
            reference = implComposite.getLink();
        } else if (composite != null) {
            reference = composite.getLink();
        } else {
            reference = null;
        }

        return reference;
    }

    public boolean isBinaryElementLength() {
        if (element != null) {
            UsageNode next = element.getNextSibling();

            if (next != null && next.isNodeType(EDIType.Type.ELEMENT)) {
                EDISimpleType nextType = (EDISimpleType) next.getReferencedType();
                return nextType.getBase() == EDISimpleType.Base.BINARY;
            }
        }

        return false;
    }

    public EDIReference getElementReference() {
        final EDIReference reference;

        if (implSegmentSelected && implElement != null) {
            reference = implElement.getLink();
        } else if (element != null) {
            reference = element.getLink();
        } else {
            reference = null;
        }

        return reference;
    }

    private static EDIReference referenceOf(EDIComplexType type, int minOccurs, int maxOccurs) {
        return new EDIReference() {
            @Override
            public EDIType getReferencedType() {
                return type;
            }

            @Override
            public int getMinOccurs() {
                return minOccurs;
            }

            @Override
            public int getMaxOccurs() {
                return maxOccurs;
            }

            @Override
            public String getTitle() {
                return type.getTitle();
            }

            @Override
            public String getDescription() {
                return type.getDescription();
            }
        };
    }

    private static UsageNode buildTree(final EDIComplexType root) {
        return buildTree(null, 0, referenceOf(root, 1, 1), -1);
    }

    private static UsageNode buildTree(UsageNode parent, int parentDepth, EDIReference link, int index) {
        int depth = parentDepth + 1;
        EDIType referencedNode = link.getReferencedType();

        if (referencedNode instanceof EDISimpleType) {
            return new UsageNode(parent, depth, link, index);
        }

        final UsageNode node;

        if (referencedNode instanceof EDIControlType) {
            node = new ControlUsageNode(parent, depth, link, index);
        } else {
            node = new UsageNode(parent, depth, link, index);
        }

        EDIComplexType structure = (EDIComplexType) referencedNode;

        List<? extends EDIReference> children = structure.getReferences();
        List<UsageNode> childUsages = node.getChildren();

        int childIndex = -1;

        for (EDIReference child : children) {
            childUsages.add(buildTree(node, depth, child, ++childIndex));
        }

        return node;
    }

    private static UsageNode buildImplTree(UsageNode parent, int parentDepth, EDITypeImplementation impl, int index) {
        int depth = parentDepth + 1;
        final UsageNode node = new UsageNode(parent, depth, impl, index);
        final List<EDITypeImplementation> children;

        switch (impl.getType()) {
        case COMPOSITE:
            children = CompositeImplementation.class.cast(impl).getSequence();
            break;
        case ELEMENT:
            children = Collections.emptyList();
            break;
        case TRANSACTION:
        case LOOP:
            children = LoopImplementation.class.cast(impl).getSequence();
            break;
        case SEGMENT:
            children = SegmentImplementation.class.cast(impl).getSequence();
            break;
        default:
            throw new IllegalArgumentException("Illegal type of EDITypeImplementation: " + impl.getType());
        }

        List<UsageNode> childUsages = node.getChildren();

        int childIndex = -1;

        for (EDITypeImplementation child : children) {
            ++childIndex;

            UsageNode childNode = null;

            if (child != null) {
                childNode = buildImplTree(node, depth, child, childIndex);
            }

            childUsages.add(childNode);
        }

        return node;
    }

    private UsageNode startLoop(UsageNode loop) {
        loop.incrementUsage();
        loop.resetChildren();

        UsageNode startSegment = loop.getFirstChild();

        startSegment.reset();
        startSegment.incrementUsage();

        depth++;

        return startSegment;
    }

    private void completeLoops(ValidationEventHandler handler, int workingDepth) {
        while (this.depth < workingDepth) {
            handleMissingMandatory(handler, workingDepth);
            UsageNode loop = loopStack.pop();
            handler.loopEnd(loop.getLink());
            workingDepth--;

            if (loop.isImplementation()) {
                implNode = loop;
            }
        }
    }

    public void validateSegment(ValidationEventHandler handler, CharSequence tag) {
        initial = false;
        segmentExpected = true;
        implSegmentSelected = false;
        clearElements();

        final int startDepth = this.depth;
        UsageCursor startLoop = null;

        cursor.standard = correctSegment;
        cursor.impl = implNode;

        // Version specific validation must be complete by the end of a segment
        revalidationQueue.clear();
        useErrors.clear();
        boolean handled = false;

        while (!handled) {
            handled = handleNode(tag, cursor.standard, cursor.impl, startDepth, handler);

            if (!handled) {
                /*
                 * The segment doesn't match the current node, ensure
                 * requirements for the current node are met.
                 */
                checkMinimumUsage(cursor.standard);

                UsageNode nextImpl = getNextImplementationNode(cursor.impl, cursor.standard.getReferencedType());

                if (cursor.hasNextSibling()) {
                    // Advance to the next segment in the loop
                    cursor.next(nextImpl); // Impl node may be unchanged
                } else {
                    if (startLoop == null) {
                        /*
                         * Remember the position of the last known loop's segment in case
                         * the segment being validated is an earlier sibling that is out of
                         * proper sequence.
                         */
                        startLoop = cursor.copy();
                    }
                    handled = handleLoopEnd(cursor, startLoop, tag, startDepth, handler);
                }
            }
        }

        handleMissingMandatory(handler);
    }

    public void countSegment(CharSequence tag) {
        if (loopStack.isEmpty()) {
            countSegment(root, tag);
        } else {
            for (UsageNode loop : loopStack) {
                countSegment(loop, tag);
            }
        }
    }

    void countSegment(UsageNode node, CharSequence tag) {
        int count;

        if ((count = count(node, EDIControlType.Type.SEGMENTS)) > 0) {
            LOGGER.finer(() -> "Counted tag " + tag + " @ " + count + " towards " + node);
        }
    }

    void countControl() {
        if (containerSchema == null) {
            if (loopStack.isEmpty()) {
                count(root, EDIControlType.Type.CONTROLS);
            } else {
                count(loopStack.peekLast(), EDIControlType.Type.CONTROLS);
            }
        }
    }

    int count(UsageNode node, EDIControlType.Type type) {
        if (node instanceof ControlUsageNode) {
            return ((ControlUsageNode) node).incrementCount(type);
        }
        return 0;
    }

    boolean handleNode(CharSequence tag, UsageNode current, UsageNode currentImpl, int startDepth, ValidationEventHandler handler) {
        final boolean handled;

        switch (current.getNodeType()) {
        case SEGMENT:
            handled = handleSegment(tag, current, currentImpl, startDepth, handler);
            break;
        case GROUP:
        case TRANSACTION:
        case LOOP:
            handled = handleLoop(tag, current, currentImpl, startDepth, handler);
            break;
        default:
            handled = false;
            break;
        }

        return handled;
    }

    boolean handleSegment(CharSequence tag, UsageNode current, UsageNode currentImpl, int startDepth, ValidationEventHandler handler) {
        if (!current.getId().contentEquals(tag)) {
            /*
             * The schema segment does not match the segment tag found
             * in the stream.
             */
            return false;
        }

        if (current.isUsed() && current.isFirstChild() &&
                current.getParent().isNodeType(EDIType.Type.LOOP)) {
            /*
             * The current segment is the first segment in the loop and
             * the loop has previous occurrences. This will occur when
             * the previous loop occurrence contained only the loop start
             * segment.
             */
            return false;
        }

        completeLoops(handler, startDepth);
        current.incrementUsage();
        current.resetChildren();

        if (current.exceedsMaximumUsage(SEGMENT_VERSION)) {
            handleMissingMandatory(handler);
            handler.segmentError(current.getId(), current.getLink(), SEGMENT_EXCEEDS_MAXIMUM_USE);
        }

        correctSegment = segment = current;

        if (currentImpl != null) {
            UsageNode impl = currentImpl;

            while (impl != null && impl.getReferencedType().equals(current.getReferencedType())) {
                implSegmentCandidates.add(impl);
                impl = impl.getNextSibling();
            }

            if (implSegmentCandidates.isEmpty()) {
                handleMissingMandatory(handler);
                handler.segmentError(current.getId(), current.getLink(), IMPLEMENTATION_UNUSED_SEGMENT_PRESENT);
                // Save the currentImpl so that the search is resumed from the correct location
                implNode = currentImpl;
            } else if (isSingleSegmentWithoutDescriminator(implSegmentCandidates)) {
                currentImpl.incrementUsage();
                currentImpl.resetChildren();

                if (currentImpl.exceedsMaximumUsage(SEGMENT_VERSION)) {
                    handler.segmentError(currentImpl.getId(), current.getLink(), SEGMENT_EXCEEDS_MAXIMUM_USE);
                }

                implNode = currentImpl;
                implSegmentCandidates.clear();
                implSegmentSelected = true;
            }
        }

        return true;
    }

    boolean isSingleSegmentWithoutDescriminator(List<UsageNode> candidates) {
        if (candidates.size() != 1) {
            return false;
        }

        PolymorphicImplementation candidate = (PolymorphicImplementation) candidates.get(0).getLink();
        return candidate.getDiscriminator() == null;
    }

    static UsageNode toSegment(UsageNode node) {
        UsageNode segmentNode;

        switch (node.getNodeType()) {
        case SEGMENT:
            segmentNode = node;
            break;
        case GROUP:
        case TRANSACTION:
        case LOOP:
            segmentNode = node.getFirstChild();
            break;
        default:
            throw new IllegalStateException("Unexpected node type: " + node.getNodeType());
        }

        return segmentNode;
    }

    void checkMinimumUsage(UsageNode node) {
        if (!node.hasMinimumUsage(SEGMENT_VERSION)) {
            /*
             * The schema segment has not met it's minimum usage
             * requirement.
             */
            final UsageNode segmentNode = toSegment(node);

            if (!segmentNode.isImplementation()) {
                useErrors.add(new UsageError(segmentNode.getLink(), MANDATORY_SEGMENT_MISSING, node.getDepth()));
            } else if (node.getNodeType() == Type.SEGMENT) {
                useErrors.add(new UsageError(segmentNode.getLink(), IMPLEMENTATION_SEGMENT_BELOW_MINIMUM_USE, node.getDepth()));
            } else {
                useErrors.add(new UsageError(segmentNode.getLink(), IMPLEMENTATION_LOOP_OCCURS_UNDER_MINIMUM_TIMES, node.getDepth()));
            }
        }
    }

    UsageNode getNextImplementationNode(UsageNode implNode, EDIType type) {
        while (implNode != null && implNode.getReferencedType().equals(type)) {
            // Advance past multiple implementations of the 'current' standard node
            checkMinimumUsage(implNode);
            implNode = implNode.getNextSibling();
        }

        // `implNode` will be an implementation of the type following `type`
        return implNode;
    }

    boolean handleLoop(CharSequence tag, UsageNode current, UsageNode currentImpl, int startDepth, ValidationEventHandler handler) {
        if (!current.getFirstChild().getId().contentEquals(tag)) {
            return false;
        }

        completeLoops(handler, startDepth);
        boolean implUnusedSegment = false;

        if (currentImpl != null) {
            UsageNode impl = currentImpl;

            while (impl != null && impl.getReferencedType().equals(current.getReferencedType())) {
                this.implSegmentCandidates.add(impl);
                impl = impl.getNextSibling();
            }

            if (implSegmentCandidates.isEmpty()) {
                implUnusedSegment = true;
                // Save the currentImpl so that the search is resumed from the correct location
                implNode = currentImpl;
            } else if (implSegmentCandidates.size() == 1) {
                handleImplementationSelected(currentImpl, currentImpl.getFirstChild(), handler);
            }
        }

        if (currentImpl != null && implSegmentSelected) {
            loopStack.push(currentImpl);
            handler.loopBegin(currentImpl.getLink());
        } else {
            if (current instanceof ControlUsageNode) {
                countControl();
            }
            loopStack.push(current);
            handler.loopBegin(current.getLink());
        }

        correctSegment = segment = startLoop(current);

        if (current.exceedsMaximumUsage(SEGMENT_VERSION)) {
            handleMissingMandatory(handler);
            handler.segmentError(tag, current.getLink(), LOOP_OCCURS_OVER_MAXIMUM_TIMES);
        }

        if (implUnusedSegment) {
            handleMissingMandatory(handler);
            handler.segmentError(segment.getId(), segment.getLink(), IMPLEMENTATION_UNUSED_SEGMENT_PRESENT);
        }

        return true;
    }

    boolean handleLoopEnd(UsageCursor cursor, UsageCursor startLoop, CharSequence tag, int startDepth, ValidationEventHandler handler) {
        boolean handled;

        if (depth > 1) {
            // Determine if the segment is in a loop higher in the tree
            cursor.nagivateUp(this.depth);
            this.depth--;
            handled = false;
        } else {
            // End of the loop - check if the segment appears earlier in the loop
            handled = checkPeerSegments(tag, startLoop.standard, handler);

            if (handled) {
                // Found the segment among the last known segment's peers so reset the depth
                this.depth = startDepth;
            } else {
                // Determine if the segment is in the transaction whatsoever
                cursor.reset(this.root, this.implRoot);
                handled = checkUnexpectedSegment(tag, cursor.standard, startDepth, handler);
            }
        }

        return handled;
    }

    boolean checkPeerSegments(CharSequence tag, UsageNode current, ValidationEventHandler handler) {
        boolean handled = false;

        if (current != correctSegment) {
            /*
             * End of the loop; try to see if we can find the segment among
             * the siblings of the last good segment. If the last good
             * segment was the final segment of the loop, do not search
             * here. Rather, go up a level and continue searching from
             * there.
             */
            UsageNode next = current.getSiblingById(tag);

            if (next != null && !next.isFirstChild()) {
                useErrors.clear();
                handler.segmentError(next.getId(), next.getLink(), SEGMENT_NOT_IN_PROPER_SEQUENCE);

                next.incrementUsage();
                next.resetChildren();

                if (next.exceedsMaximumUsage(SEGMENT_VERSION)) {
                    handler.segmentError(next.getId(), next.getLink(), SEGMENT_EXCEEDS_MAXIMUM_USE);
                }

                segment = next;
                handled = true;
            }
        }

        return handled;
    }

    boolean checkUnexpectedSegment(CharSequence tag, UsageNode current, int startDepth, ValidationEventHandler handler) {
        boolean handled = false;

        if (!current.getId().contentEquals(tag)) {
            final String tagString = tag.toString();

            if (containerSchema != null && containerSchema.containsSegment(tagString)) {
                // The segment is defined in the containing schema.
                // Complete any open loops (handling missing mandatory at each level).
                completeLoops(handler, startDepth);

                // Handle any remaining missing mandatory segments
                handleMissingMandatory(handler);
            } else {
                // Unexpected segment... must reset our position!
                segmentExpected = false;
                this.depth = startDepth;
                useErrors.clear();

                if (schema.containsSegment(tagString)) {
                    handler.segmentError(tag, null, UNEXPECTED_SEGMENT);
                } else {
                    handler.segmentError(tag, null, SEGMENT_NOT_IN_DEFINED_TRANSACTION_SET);
                }
            }

            handled = true; // Wasn't found or it's in the control schema; cut our losses and go back.
        }

        return handled;
    }

    private void handleMissingMandatory(ValidationEventHandler handler) {
        for (UsageError error : useErrors) {
            error.handleSegmentError(handler);
        }

        useErrors.clear();
    }

    private void handleMissingMandatory(ValidationEventHandler handler, int depth) {
        Iterator<UsageError> errors = useErrors.iterator();

        while (errors.hasNext()) {
            UsageError e = errors.next();
            if (e.isDepthGreaterThan(depth)) {
                e.handleSegmentError(handler);
                errors.remove();
            }
        }
    }

    public void clearImplementationCandidates(ValidationEventHandler handler) {
        if (!implSegmentCandidates.isEmpty()) {
            handler.segmentError(segment.getId(), segment.getLink(), IMPLEMENTATION_UNUSED_SEGMENT_PRESENT);
            implSegmentCandidates.clear();
        }
    }

    public boolean selectImplementation(Deque<StreamEvent> eventQueue, ValidationEventHandler handler) {
        StreamEvent currentEvent = eventQueue.getLast();

        if (currentEvent.getType() != EDIStreamEvent.ELEMENT_DATA) {
            return false;
        }

        for (UsageNode candidate : implSegmentCandidates) {
            PolymorphicImplementation implType;
            UsageNode implSeg = toSegment(candidate);
            implType = (PolymorphicImplementation) candidate.getLink();

            if (isMatch(implType, currentEvent)) {
                handleImplementationSelected(candidate, implSeg, handler);

                if (implNode.isFirstChild()) {
                    //start of loop, update the loop, segment, and element references that were already reported
                    updateEventReferences(eventQueue, implType, implSeg.getLink());

                    // Replace the standard loop with the implementation on the stack
                    loopStack.pop();
                    loopStack.push(implNode.getParent());
                } else {
                    //update segment and element references that were already reported
                    updateEventReferences(eventQueue, null, implSeg.getLink());
                }

                return true;
            }
        }

        return false;
    }

    void handleImplementationSelected(UsageNode candidate, UsageNode implSeg, ValidationEventHandler handler) {
        implSegmentCandidates.clear();
        implNode = implSeg;
        implSegmentSelected = true;

        /*
         * NOTE: Validation of prior implementation elements will occur only occur
         * for prior simple elements and composites (not prior components in the same
         * composite when the discriminator is a component element and the position
         * within the composite is > 1).
         */
        if (this.isComposite()) {
            this.implComposite = implSeg.getChild(this.composite.getIndex());
            this.implElement = this.implComposite.getChild(this.element.getIndex());
            checkPreviousSiblings(implSeg, handler);
        } else if (this.element != null) {
            // Set implementation when standard element is already set (e.g. via discriminator)
            this.implComposite = null;
            this.implElement = implSeg.getChild(this.element.getIndex());
            checkPreviousSiblings(implSeg, handler);
        }

        if (candidate.isNodeType(Type.LOOP)) {
            candidate.incrementUsage();
            candidate.resetChildren();
            implSeg.incrementUsage();

            if (candidate.exceedsMaximumUsage(SEGMENT_VERSION)) {
                handler.segmentError(implSeg.getId(), implSeg.getLink(), LOOP_OCCURS_OVER_MAXIMUM_TIMES);
            }
        } else {
            candidate.incrementUsage();

            if (candidate.exceedsMaximumUsage(SEGMENT_VERSION)) {
                handler.segmentError(implSeg.getId(), implSeg.getLink(), SEGMENT_EXCEEDS_MAXIMUM_USE);
            }
        }
    }

    /**
     * Validate any implementation elements previously skipped while searching
     * for the loop discriminator element. Validation of enumerated values specified
     * by the implementation schema are currently not supported for elements occurring
     * prior to the discriminator element.
     *
     * @param implSeg selected implementation segment
     * @param handler validation handler
     */
    void checkPreviousSiblings(UsageNode implSeg, ValidationEventHandler handler) {
        for (RevalidationNode entry : this.revalidationQueue) {
            UsageNode std = entry.standard;
            UsageNode impl = implSeg.getChild(std.getIndex());

            validateImplRepetitions(null, impl);

            if (std.isUsed()) {
                validateImplUnusedElementBlank(std, impl, true);
            } else {
                validateDataElementRequirement(null, std, impl, entry.location);
            }

            handleRevalidatedElementErrors(entry, elementErrors, handler);
        }

        revalidationQueue.clear();
    }

    static boolean isMatch(PolymorphicImplementation implType, StreamEvent currentEvent) {
        Discriminator discr = implType.getDiscriminator();

        // If no discriminator, matches by default
        if (discr ==  null) {
            return true;
        }

        return discr.matchesLocation(currentEvent.getLocation())
                && discr.getValueSet().contains(currentEvent.getData().toString());
    }

    /**
     * Overlay the most recently started loop's standard reference code with the reference
     * code of the implType.
     *
     * @param events
     * @param index
     * @param count
     * @param implType
     */
    static void updateEventReferences(Deque<StreamEvent> eventQueue, EDIReference implType, EDIReference implSeg) {
        for (StreamEvent event : eventQueue) {
            switch (event.getType()) {
            case START_LOOP:
                // Assuming implType is not null if we find a START_LOOP event
                Objects.requireNonNull(implType, "Unexpected loop event during implementation segment selection");
                updateReferenceWhenMatched(event, implType);
                break;
            case START_SEGMENT:
                updateReferenceWhenMatched(event, implSeg);
                break;
            case START_COMPOSITE:
            case END_COMPOSITE:
            case ELEMENT_DATA:
                updateReference(event, (SegmentImplementation) implSeg);
                break;
            default:
                break;
            }
        }
    }

    static void updateReferenceWhenMatched(StreamEvent event, EDIReference override) {
        // The reference type code from override is the reference code of the impl's standard type
        if (Objects.equals(event.getReferenceCode(), override.getReferencedType().getCode())) {
            event.setTypeReference(override);
        }
    }

    static void updateReference(StreamEvent event, SegmentImplementation override) {
        final List<EDITypeImplementation> implElements = override.getSequence();
        final Location location = event.getLocation();
        final int elementIndex = location.getElementPosition() - 1;
        final int componentIndex = location.getComponentPosition() - 1;
        EDITypeImplementation element = implElements.get(elementIndex);

        if (componentIndex > -1) {
            CompositeImplementation composite = (CompositeImplementation) implElements.get(elementIndex);
            element = composite.getSequence().get(componentIndex);
        }

        if (element != null) {
            // Set the impl element if present, otherwise leave the standard in place
            event.setTypeReference(element);
        }
    }

    /* ********************************************************************** */

    public List<UsageError> getElementErrors() {
        return elementErrors;
    }

    UsageNode getImplElement(String version, int index) {
        if (implSegmentSelected) {
            return this.implNode.getChild(version, index);
        }

        return null;
    }

    boolean isImplElementSelected() {
        return implSegmentSelected && this.implElement != null;
    }

    boolean isImplUnusedElementPresent(UsageNode implElementUsed, boolean valueReceived) {
        return valueReceived && implSegmentSelected && implElementUsed == null;
    }

    public boolean validCompositeOccurrences(Dialect dialect, Location position) {
        if (!segmentExpected) {
            return true;
        }

        final int elementPosition = position.getElementPosition() - 1;
        final int componentIndex = position.getComponentPosition() - 1;
        final String version = dialect.getTransactionVersionString();

        elementErrors.clear();

        this.composite = null;
        this.element = segment.getChild(version, elementPosition);

        validateImplRepetitions(version, elementPosition, -1);

        this.implComposite = null;
        this.implElement = getImplElement(version, elementPosition);

        if (element == null) {
            elementErrors.add(new UsageError(TOO_MANY_DATA_ELEMENTS));
            return false;
        } else if (!element.isNodeType(EDIType.Type.COMPOSITE)) {
            this.element.incrementUsage();

            if (this.element.exceedsMaximumUsage(version)) {
                elementErrors.add(new UsageError(this.element, TOO_MANY_REPETITIONS));
                return false;
            }

            // Element has components but is not defined as a composite (validate in element validation)
            return true;
        } else if (componentIndex > -1) {
            throw new IllegalStateException("Invalid position w/in composite");
        }

        this.composite = this.element;
        this.element = null;
        this.composite.incrementUsage();
        // resetChildren?

        if (this.composite.exceedsMaximumUsage(version)) {
            elementErrors.add(new UsageError(this.composite, TOO_MANY_REPETITIONS));
            return false;
        }

        if (!validateImplUnusedElementBlank(this.composite, this.implElement, true)) {
            return false;
        }

        this.implComposite = this.implElement;
        this.implElement = null;

        if (implSegmentSelected) {
            this.implComposite.incrementUsage();
            // resetChildren?
        }

        return elementErrors.isEmpty();
    }

    boolean isComposite() {
        return isComposite(this.composite);
    }

    static boolean isComposite(UsageNode composite) {
        return composite != null && !StaEDISchema.ANY_COMPOSITE_ID.equals(composite.getId());
    }

    public boolean isComposite(Dialect dialect, StaEDIStreamLocation position) {
        if (!segmentExpected) {
            return false;
        }

        final String version = dialect.getTransactionVersionString();
        final int elementPosition = position.getElementPosition() - 1;

        if (elementPosition < segment.getChildren(version).size()) {
            UsageNode candidate = segment.getChild(version, elementPosition);
            return candidate.isNodeType(EDIType.Type.COMPOSITE) && isComposite(candidate);
        }

        return false;
    }

    public boolean validateElement(Dialect dialect, StaEDIStreamLocation position, CharSequence value, StringBuilder formattedValue) {
        if (!segmentExpected) {
            return true;
        }

        boolean valueReceived = value != null && value.length() > 0;
        clearElements();
        elementErrors.clear();

        int elementPosition = position.getElementPosition() - 1;
        int componentIndex = position.getComponentPosition() - 1;
        final String version = dialect.getTransactionVersionString();

        validateImplRepetitions(version, elementPosition, componentIndex);

        if (elementPosition >= segment.getChildren(version).size()) {
            if (componentIndex < 0) {
                /*
                 * Only notify if this is not a composite - handled in
                 * validCompositeOccurrences
                 */
                elementErrors.add(new UsageError(TOO_MANY_DATA_ELEMENTS));
                return false;
            }

            /*
             * Undefined element - unable to report errors.
             */
            return true;
        }

        this.element = segment.getChild(version, elementPosition);
        this.implElement = getImplElement(version, elementPosition);

        if (element.isNodeType(EDIType.Type.COMPOSITE)) {
            this.composite = this.element;
            this.implComposite = this.implElement;

            if (componentIndex < 0) {
                componentIndex = 0;
            }
        }

        if (componentIndex > -1) {
            validateComponentElement(dialect, componentIndex, valueReceived);
        } else {
            // Validated in validCompositeOccurrences for received composites
            validateImplUnusedElementBlank(this.element, this.implElement, valueReceived);
        }

        if (!elementErrors.isEmpty()) {
            return false;
        }

        if (valueReceived) {
            validateElementValue(dialect, position, value, formattedValue);
        } else {
            validateDataElementRequirement(version, this.element, this.implElement, position);
        }

        return elementErrors.isEmpty();
    }

    void validateComponentElement(Dialect dialect, int componentIndex, boolean valueReceived) {
        if (!element.isNodeType(EDIType.Type.COMPOSITE)) {
            /*
             * This element has components but is not defined as a composite
             * structure.
             */
            elementErrors.add(new UsageError(this.element, TOO_MANY_COMPONENTS));
        } else {
            if (componentIndex == 0) {
                UsageNode.resetChildren(this.element, this.implElement);
            }

            String version = dialect.getTransactionVersionString();

            if (componentIndex < element.getChildren(version).size()) {
                if (valueReceived || componentIndex != 0 /* Derived component */) {
                    this.element = this.element.getChild(version, componentIndex);

                    if (isImplElementSelected()) {
                        this.implElement = this.implElement.getChild(version, componentIndex);
                        validateImplUnusedElementBlank(this.element, this.implElement, valueReceived);
                    }
                }
            } else {
                elementErrors.add(new UsageError(this.element, TOO_MANY_COMPONENTS));
            }
        }
    }

    void validateElementValue(Dialect dialect, StaEDIStreamLocation position, CharSequence value, StringBuilder formattedValue) {
        final String version = dialect.getTransactionVersionString();

        if (!element.isNodeType(EDIType.Type.COMPOSITE)) {
            this.element.incrementUsage();

            if (this.implElement != null) {
                this.implElement.incrementUsage();
            }

            if (this.element.exceedsMaximumUsage(version)) {
                elementErrors.add(new UsageError(this.element, TOO_MANY_REPETITIONS));
            }
        }

        if ((version.isEmpty() && this.element.hasVersions()) || isPendingDiscrimination()) {
            // This element value can not be validated until the version is determined
            revalidationQueue.add(new RevalidationNode(this.element, this.implElement, value, position));
            return;
        }

        validateElementValue(dialect, position, this.element, this.implElement, value, formattedValue);
    }

    public void validateVersionConstraints(Dialect dialect, ValidationEventHandler validationHandler, StringBuilder formattedValue) {
        for (RevalidationNode entry : revalidationQueue) {
            if (entry.data != null) {
                validateElementValue(dialect, entry.location, entry.standard, entry.impl, entry.data, formattedValue);
            } else {
                validateDataElementRequirement(dialect.getTransactionVersionString(), entry.standard, entry.impl, entry.location);
            }
            handleRevalidatedElementErrors(entry, elementErrors, validationHandler);
        }

        revalidationQueue.clear();
    }

    void handleRevalidatedElementErrors(RevalidationNode entry, List<UsageError> errors, ValidationEventHandler validationHandler) {
        for (UsageError error : errors) {
            validationHandler.elementError(error.getError().getCategory(),
                                           error.getError(),
                                           error.getTypeReference(),
                                           entry.data,
                                           entry.location.getElementPosition(),
                                           entry.location.getComponentPosition(),
                                           entry.location.getElementOccurrence());
        }

        errors.clear();
    }

    void validateElementValue(Dialect dialect, StaEDIStreamLocation position, UsageNode element, UsageNode implElement, CharSequence value, StringBuilder formattedValue) {
        List<EDIStreamValidationError> errors = new ArrayList<>();
        if (this.formatElements) {
            formattedValue.setLength(0);
            element.format(dialect, value, formattedValue);
            value = formattedValue;
        } else {
            element.validate(dialect, value, errors);
            validateControlValue(segment.getParent(), position, value, errors);
        }

        for (EDIStreamValidationError error : errors) {
            if (this.validateCodeValues || error != INVALID_CODE_VALUE) {
                elementErrors.add(new UsageError(element, error));
            }
        }

        if (errors.isEmpty() && implSegmentSelected && implElement != null) {
            implElement.validate(dialect, value, errors);

            for (EDIStreamValidationError error : errors) {
                if (error == INVALID_CODE_VALUE) {
                    error = IMPLEMENTATION_INVALID_CODE_VALUE;
                }
                elementErrors.add(new UsageError(element, error));
            }
        }
    }

    public static List<UsageError> validateCharacters(CharSequence value) {
        if (AlphaNumericValidator.validCharacters(value, value.length())) {
            return Collections.emptyList();
        }
        return Collections.singletonList(new UsageError(EDIStreamValidationError.INVALID_CHARACTER_DATA));
    }

    void validateControlValue(UsageNode loop, StaEDIStreamLocation position, CharSequence value, List<EDIStreamValidationError> errors) {
        if (loop instanceof ControlUsageNode) {
            ((ControlUsageNode) loop).validateReference(position, value, errors);
            ((ControlUsageNode) loop).validateCount(position, value, errors);
        }
    }

    public void validateSyntax(Dialect dialect, ElementDataHandler handler, ValidationEventHandler validationHandler, final StaEDIStreamLocation location, final boolean isComposite) {
        if (isComposite && composite == null) {
            // End composite but element is not composite in schema
            return;
        }

        final String version = dialect.getTransactionVersionString();
        final UsageNode structure = isComposite ? composite : segment;
        final int index = getCurrentIndex(location, isComposite);
        final int elementPosition = location.getElementPosition() - 1;
        final int componentIndex = location.getComponentPosition() - 1;
        final List<UsageNode> children = structure.getChildren(version);

        // Ensure the start index is at least zero. Index may be -1 for empty segments
        for (int i = Math.max(index, 0), max = children.size(); i < max; i++) {
            handler.elementData("", false);
        }

        if (!isComposite && implSegmentSelected && index == children.size()) {
            UsageNode previousImpl = implNode.getChild(elementPosition);

            if (tooFewRepetitions(version, previousImpl)) {
                validationHandler.elementError(IMPLEMENTATION_TOO_FEW_REPETITIONS.getCategory(),
                                               IMPLEMENTATION_TOO_FEW_REPETITIONS,
                                               previousImpl.getLink(),
                                               null,
                                               elementPosition + 1,
                                               componentIndex + 1,
                                               -1);
            }
        }

        for (EDISyntaxRule rule : structure.getSyntaxRules()) {
            final EDISyntaxRule.Type ruleType = rule.getType();
            SyntaxValidator validator = SyntaxValidator.getInstance(ruleType);
            validator.validate(rule, structure, validationHandler);
        }
    }

    public void validateLoopSyntax(ValidationEventHandler validationHandler) {
        final UsageNode loop = segment.getParent();

        for (EDISyntaxRule rule : loop.getSyntaxRules()) {
            final EDISyntaxRule.Type ruleType = rule.getType();
            SyntaxValidator validator = SyntaxValidator.getInstance(ruleType);
            validator.validate(rule, loop, validationHandler);
        }
    }

    /**
     * Validate that the implementation element prior to the element identified
     * by elemenetPosition and componentPosition met its required number of
     * repetitions. Validation will only occur for simple elements or at the
     * start of a composite element.
     *
     * @param version version to use for validation
     * @param elementPosition position of the current element
     * @param componentPosition position of the current component
     */
    void validateImplRepetitions(String version, int elementPosition, int componentPosition) {
        if (elementPosition > 0 && componentPosition < 0) {
            UsageNode previousImpl = getImplElement(version, elementPosition - 1);
            validateImplRepetitions(version, previousImpl);
        }
    }

    void validateImplRepetitions(String version, UsageNode implElement) {
        if (tooFewRepetitions(version, implElement)) {
            elementErrors.add(new UsageError(implElement, IMPLEMENTATION_TOO_FEW_REPETITIONS));
        }
    }

    boolean validateImplUnusedElementBlank(UsageNode node, UsageNode implNode, boolean valueReceived) {
        if (isImplUnusedElementPresent(implNode, valueReceived)) {
            // Validated in validCompositeOccurrences for received composites
            elementErrors.add(new UsageError(node, IMPLEMENTATION_UNUSED_DATA_ELEMENT_PRESENT));
            return false;
        }
        return true;
    }

    void validateDataElementRequirement(String version, UsageNode element, UsageNode implElement, StaEDIStreamLocation position) {
        if (!UsageNode.hasMinimumUsage(version, element) || !UsageNode.hasMinimumUsage(version, implElement)) {
            elementErrors.add(new UsageError(element, REQUIRED_DATA_ELEMENT_MISSING));
        } else if (isPendingDiscrimination()) {
            // This element requirement can not be validated until the correct implementation is determined
            revalidationQueue.add(new RevalidationNode(this.element, this.implElement, position));
        }
    }

    boolean tooFewRepetitions(String version, UsageNode node) {
        if (!UsageNode.hasMinimumUsage(version, node)) {
            /*
             * Compare to `1` for repetitions. Elements not meeting requirement
             * of `> 0` are instead signaled as missing requirement elements.
             */
            return node.getLink().getMinOccurs(version) > 1;
        }

        return false;
    }

    int getCurrentIndex(Location location, boolean isComposite) {
        final int index;

        if (isComposite) {
            index = Math.max(location.getComponentPosition(), 1);
        } else {
            index = location.getElementPosition();
        }

        return index;
    }
}
