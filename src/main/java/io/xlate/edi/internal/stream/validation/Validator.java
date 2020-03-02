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

import static io.xlate.edi.stream.EDIStreamValidationError.IMPLEMENTATION_LOOP_OCCURS_UNDER_MINIMUM_TIMES;
import static io.xlate.edi.stream.EDIStreamValidationError.IMPLEMENTATION_SEGMENT_BELOW_MINIMUM_USE;
import static io.xlate.edi.stream.EDIStreamValidationError.IMPLEMENTATION_UNUSED_SEGMENT_PRESENT;
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.function.BiConsumer;

import io.xlate.edi.internal.stream.StaEDIStreamLocation;
import io.xlate.edi.internal.stream.tokenization.Dialect;
import io.xlate.edi.internal.stream.tokenization.ElementDataHandler;
import io.xlate.edi.internal.stream.tokenization.StreamEvent;
import io.xlate.edi.internal.stream.tokenization.ValidationEventHandler;
import io.xlate.edi.schema.EDIComplexType;
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

    private Schema containerSchema;
    private Schema schema;
    private final UsageNode root;

    private boolean segmentExpected;
    private UsageNode segment;
    private UsageNode correctSegment;
    private UsageNode element;
    private UsageNode composite;

    static class UsageError {
        String code;
        EDIStreamValidationError error;
        int depth;

        UsageError(String code, EDIStreamValidationError error, int depth) {
            super();
            this.code = code;
            this.error = error;
            this.depth = depth;
        }

        void handle(BiConsumer<String, EDIStreamValidationError> handler) {
            handler.accept(code, error);
        }
    }

    private final List<UsageError> useErrors = new ArrayList<>();

    private final List<EDIStreamValidationError> elementErrors = new ArrayList<>(5);

    private int depth = 1;
    private boolean complete = false;

    private final UsageNode implRoot;
    private UsageNode implSegment;
    private UsageNode implCorrectSegment;
    private List<UsageNode> implSegmentCandidates = new ArrayList<>();

    public Validator(Schema schema, Schema containerSchema) {
        this.schema = schema;
        this.containerSchema = containerSchema;

        root = buildTree(schema.getStandard());
        correctSegment = segment = root.getFirstChild();

        if (schema.getImplementation() != null) {
            implRoot = buildTree(null, 0, schema.getImplementation(), -1);
            implCorrectSegment = implSegment = implRoot.getFirstChild();
        } else {
            implRoot = null;
            implCorrectSegment = implSegment = null;
        }
    }

    public boolean isComplete() {
        return complete;
    }

    public boolean isImplementation() {
        return implRoot != null;
    }

    public boolean isPendingDiscrimination() {
        return !implSegmentCandidates.isEmpty();
    }

    public String getCompositeReferenceCode() {
        return composite != null ? composite.getCode() : null;
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

    public String getElementReferenceNumber() {
        int number = (element != null) ? element.getNumber() : -1;
        return (number > -1) ? String.valueOf(number) : null;
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
        };
    }

    private static UsageNode buildTree(final EDIComplexType root) {
        return buildTree(null, 0, referenceOf(root, 1, 1), -1);
    }

    private static UsageNode buildTree(UsageNode parent, int parentDepth, EDIReference link, int index) {
        int depth = parentDepth + 1;
        EDIType referencedNode = link.getReferencedType();

        UsageNode node = new UsageNode(parent, depth, link, index);

        if (!(referencedNode instanceof EDIComplexType)) {
            return node;
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

    private static UsageNode buildTree(UsageNode parent, int parentDepth, EDITypeImplementation impl, int index) {
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
            childUsages.add(buildTree(node, depth, child, ++childIndex));
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

    private void completeLoops(ValidationEventHandler handler, int d) {
        UsageNode node;
        boolean implLoop;

        if (implCorrectSegment != null) {
            /*-
             * Use the implementation node so that the reference code passed to the
             * handler reflects the implementation.
             */
            node = implCorrectSegment;
            implLoop = true;
        } else {
            node = correctSegment;
            implLoop = false;
        }

        while (this.depth < d--) {
            handleMissingMandatory(handler, d);
            node = completeLoop(handler, node);
        }

        if (implLoop) {
            implSegment = node;
        }
    }

    UsageNode completeLoop(ValidationEventHandler handler, UsageNode node) {
        UsageNode parent = node.getParent();
        handler.loopEnd(parent.getCode());
        return parent;
    }

    public void validateSegment(ValidationEventHandler handler, CharSequence tag) {
        segmentExpected = true;

        final int startDepth = this.depth;

        UsageNode current = correctSegment;
        UsageNode currentImpl = implCorrectSegment;
        useErrors.clear();
        complete = false;
        boolean handled = false;

        while (!handled && current != null) {
            handled = handleNode(tag, current, currentImpl, startDepth, handler);

            if (!handled) {
                /*
                 * The segment doesn't match the current node, ensure
                 * requirements for the current node are met.
                 */
                checkMinimumUsage(current);

                UsageNode next = current.getNextSibling();
                UsageNode nextImpl = currentImpl;

                while (nextImpl != null && nextImpl.getReferencedType().equals(current.getReferencedType())) {
                    // Advance past multiple implementations of the 'current' standard node
                    checkMinimumUsage(nextImpl);
                    nextImpl = nextImpl.getNextSibling();
                }

                if (next != null) {
                    // Advance to the next segment in the loop
                    current = next;
                    if (nextImpl != null) {
                        currentImpl = nextImpl; // May be unchanged
                    }
                } else {
                    // End of the loop - check if the segment appears earlier in the loop
                    handled = checkPeerSegments(tag, current, startDepth, handler);

                    if (!handled) {
                        if (this.depth > 1) {
                            current = current.getParent();
                            currentImpl = currentImpl != null ? currentImpl.getParent() : null;
                            this.depth--;
                        } else {
                            current = this.root.getFirstChild();
                            currentImpl = this.implRoot != null ? this.implRoot.getFirstChild() : null;
                            handled = checkUnexpectedSegment(tag, current, startDepth, handler);
                        }
                    }
                }
            }
        }

        handleMissingMandatory(handler);
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

        if (current.exceedsMaximumUsage()) {
            handleMissingMandatory(handler);
            handler.segmentError(current.getId(), SEGMENT_EXCEEDS_MAXIMUM_USE);
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
                handler.segmentError(current.getId(), IMPLEMENTATION_UNUSED_SEGMENT_PRESENT);
                // Save the currentImpl so that the search is resumed from the correct location
                implCorrectSegment = implSegment = currentImpl;
            } else if (implSegmentCandidates.size() == 1) {
                currentImpl.incrementUsage();
                currentImpl.resetChildren();

                if (currentImpl.exceedsMaximumUsage()) {
                    handler.segmentError(currentImpl.getId(), SEGMENT_EXCEEDS_MAXIMUM_USE);
                }

                implCorrectSegment = implSegment = currentImpl;
                implSegmentCandidates.clear();
            }
        }

        return true;
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
        if (!node.hasMinimumUsage()) {
            /*
             * The schema segment has not met it's minimum usage
             * requirement.
             */
            final UsageNode segmentNode = toSegment(node);
            final String tag = segmentNode.getId();

            if (!segmentNode.isImplementation()) {
                useErrors.add(new UsageError(tag, MANDATORY_SEGMENT_MISSING, node.getDepth()));
            } else if (node.getNodeType() == Type.SEGMENT) {
                useErrors.add(new UsageError(tag, IMPLEMENTATION_SEGMENT_BELOW_MINIMUM_USE, node.getDepth()));
            } else {
                useErrors.add(new UsageError(tag, IMPLEMENTATION_LOOP_OCCURS_UNDER_MINIMUM_TIMES, node.getDepth()));
            }
        }
    }

    boolean handleLoop(CharSequence tag, UsageNode current, UsageNode currentImpl, int startDepth, ValidationEventHandler handler) {
        if (!current.getFirstChild().getId().contentEquals(tag)) {
            return false;
        }

        completeLoops(handler, startDepth);
        handler.loopBegin(current.getCode());
        correctSegment = segment = startLoop(current);

        if (current.exceedsMaximumUsage()) {
            handleMissingMandatory(handler);
            handler.segmentError(tag, LOOP_OCCURS_OVER_MAXIMUM_TIMES);
        }

        if (currentImpl != null) {
            UsageNode impl = currentImpl;

            while (impl != null && impl.getReferencedType().equals(current.getReferencedType())) {
                this.implSegmentCandidates.add(impl);
                impl = impl.getNextSibling();
            }
        }

        return true;
    }

    boolean checkPeerSegments(CharSequence tag, UsageNode current, int startDepth, ValidationEventHandler handler) {
        boolean handled = false;

        if (this.depth == startDepth && current != correctSegment) {
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
                handler.segmentError(next.getId(), SEGMENT_NOT_IN_PROPER_SEQUENCE);

                next.incrementUsage();

                if (next.exceedsMaximumUsage()) {
                    handler.segmentError(next.getId(), SEGMENT_EXCEEDS_MAXIMUM_USE);
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

                complete = true;
            } else {
                // Unexpected segment... must reset our position!
                segmentExpected = false;
                this.depth = startDepth;
                useErrors.clear();

                if (schema.containsSegment(tagString)) {
                    handler.segmentError(tag, UNEXPECTED_SEGMENT);
                } else {
                    handler.segmentError(tag, SEGMENT_NOT_IN_DEFINED_TRANSACTION_SET);
                }
            }

            handled = true; // Wasn't found or it's in the control schema; cut our losses and go back.
        }

        return handled;
    }

    private void handleMissingMandatory(ValidationEventHandler handler) {
        for (UsageError error : useErrors) {
            error.handle(handler::segmentError);
        }

        useErrors.clear();
    }

    private void handleMissingMandatory(ValidationEventHandler handler, int depth) {
        Iterator<UsageError> cursor = useErrors.iterator();

        while (cursor.hasNext()) {
            UsageError e = cursor.next();
            if (e.depth >= depth) {
                e.handle(handler::segmentError);
                cursor.remove();
            }
        }
    }

    public boolean selectImplementation(StreamEvent[] events, int index, int count, ValidationEventHandler handler) {
        StreamEvent currentEvent = events[index + count - 1];

        if (currentEvent.getType() != EDIStreamEvent.ELEMENT_DATA) {
            return false;
        }

        // TODO: Add test cases with discriminators in composites and in element no. >1

        for (UsageNode candidate : implSegmentCandidates) {
            PolymorphicImplementation implType;
            UsageNode implSeg = toSegment(candidate);
            implType = (PolymorphicImplementation) candidate.getLink();

            if (isMatch(implType, currentEvent)) {
                handleImplementationSelected(candidate, implSeg, handler);

                if (implSegment.isFirstChild()) {
                    //start of loop
                    setLoopReferenceCode(events, index, count - 1, implType);
                }

                return true;
            }
        }

        return false;
    }

    void handleImplementationSelected(UsageNode candidate, UsageNode implSeg, ValidationEventHandler handler) {
        checkMinimumImplUsage(implSegment, candidate, handler);
        implSegmentCandidates.clear();
        implCorrectSegment = implSegment = implSeg;

        if (candidate.isNodeType(Type.LOOP)) {
            candidate.incrementUsage();
            candidate.resetChildren();
            implSeg.incrementUsage();

            if (candidate.exceedsMaximumUsage()) {
                handler.segmentError(implSeg.getId(), LOOP_OCCURS_OVER_MAXIMUM_TIMES);
            }
        } else {
            candidate.incrementUsage();

            if (candidate.exceedsMaximumUsage()) {
                handler.segmentError(implSeg.getId(), SEGMENT_EXCEEDS_MAXIMUM_USE);
            }
        }
    }

    void checkMinimumImplUsage(UsageNode sibling, UsageNode selected, ValidationEventHandler handler) {
        while (sibling != null && sibling != selected) {
            checkMinimumUsage(sibling);
            sibling = sibling.getNextSibling();
        }
        handleMissingMandatory(handler);
    }

    static boolean isMatch(PolymorphicImplementation implType, StreamEvent currentEvent) {
        Discriminator discr = implType.getDiscriminator();

        if (discr.getValueSet().contains(currentEvent.getData().toString())) {
            int eleLoc = discr.getElementPosition();
            int comLoc = discr.getComponentPosition() == 0 ? -1 : discr.getComponentPosition();
            Location location = currentEvent.getLocation();

            if (eleLoc == location.getElementPosition() && comLoc == location.getComponentPosition()) {
                return true;
            }
        }

        return false;
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
    static void setLoopReferenceCode(StreamEvent[] events, int index, int count, PolymorphicImplementation implType) {
        for (int i = index; i < count; i++) {
            CharSequence stdRefCode = events[i].getReferenceCode();
            // This is the reference code of the impl's standard type
            CharSequence implRefCode = ((EDIComplexType) implType.getReferencedType()).getCode();

            if (events[i].getType() == EDIStreamEvent.START_LOOP && compare(stdRefCode, implRefCode) == 0) {
                events[i].setReferenceCode(implType.getId());
            }
        }
    }

    // Replace with CharSequence#compare(CharSequence, CharSequence) when migrating to Java 11+
    @SuppressWarnings("unchecked")
    static int compare(CharSequence cs1, CharSequence cs2) {
        if (Objects.requireNonNull(cs1) == Objects.requireNonNull(cs2)) {
            return 0;
        }

        if (cs1.getClass() == cs2.getClass() && cs1 instanceof Comparable) {
            return ((Comparable<Object>) cs1).compareTo(cs2);
        }

        for (int i = 0, len = Math.min(cs1.length(), cs2.length()); i < len; i++) {
            char a = cs1.charAt(i);
            char b = cs2.charAt(i);
            if (a != b) {
                return a - b;
            }
        }

        return cs1.length() - cs2.length();
    }

    /**************************************************************************/

    public List<EDIStreamValidationError> getElementErrors() {
        return elementErrors;
    }

    public boolean validCompositeOccurrences(Location position) {
        if (!segmentExpected) {
            return true;
        }

        int elementPosition = position.getElementPosition() - 1;
        int componentIndex = position.getComponentPosition() - 1;
        elementErrors.clear();
        this.composite = null;
        this.element = segment.getChild(elementPosition);

        if (element == null) {
            elementErrors.add(TOO_MANY_DATA_ELEMENTS);
            this.element = null;
            return false;
        } else if (!element.isNodeType(EDIType.Type.COMPOSITE)) {
            this.element.incrementUsage();

            if (this.element.exceedsMaximumUsage()) {
                elementErrors.add(TOO_MANY_REPETITIONS);
                return false;
            }

            // There are too many components - validate in element validation
            return true;
        } else if (componentIndex > -1) {
            throw new IllegalStateException("Invalid position w/in composite");
        }

        this.composite = this.element;
        this.element = null;

        if (elementPosition >= segment.getChildren().size()) {
            elementErrors.add(TOO_MANY_DATA_ELEMENTS);
            return false;
        }

        this.composite.incrementUsage();

        if (this.composite.exceedsMaximumUsage()) {
            elementErrors.add(TOO_MANY_REPETITIONS);
            return false;
        }

        return true;
    }

    public boolean isComposite() {
        return composite != null;
    }

    public boolean validateElement(Dialect dialect, StaEDIStreamLocation position, CharSequence value) {
        if (!segmentExpected) {
            return true;
        }

        boolean valueReceived = value != null && value.length() > 0;
        elementErrors.clear();
        this.composite = null;
        this.element = null;

        int elementPosition = position.getElementPosition() - 1;
        int componentIndex = position.getComponentPosition() - 1;

        if (elementPosition >= segment.getChildren().size()) {
            if (componentIndex < 0) {
                /*
                 * Only notify if this is not a composite - handled in
                 * validCompositeOccurrences
                 */
                elementErrors.add(TOO_MANY_DATA_ELEMENTS);
                return false;
            }

            /*
             * Undefined element - unable to report errors.
             */
            return true;
        }

        this.element = segment.getChild(elementPosition);
        boolean isComposite = element.isNodeType(EDIType.Type.COMPOSITE);
        boolean derivedComposite = false;

        if (isComposite) {
            this.composite = this.element;

            if (componentIndex < 0) {
                derivedComposite = true;
                componentIndex = 0;
            }
        }

        if (componentIndex > -1) {
            if (!isComposite) {
                /*
                 * This element has components but is not defined as a composite
                 * structure.
                 */
                elementErrors.add(TOO_MANY_COMPONENTS);
            } else {
                if (componentIndex == 0) {
                    this.element.resetChildren();
                }

                if (componentIndex < element.getChildren().size()) {
                    if (valueReceived || !derivedComposite) {
                        this.element = this.element.getChild(componentIndex);
                    }
                } else {
                    elementErrors.add(TOO_MANY_COMPONENTS);
                }
            }
        }

        if (!elementErrors.isEmpty()) {
            return false;
        }

        if (valueReceived) {
            if (!isComposite) {
                this.element.incrementUsage();

                if (this.element.exceedsMaximumUsage()) {
                    elementErrors.add(TOO_MANY_REPETITIONS);
                }
            }

            this.element.validate(dialect, value, elementErrors);
        } else {
            if (!element.hasMinimumUsage()) {
                elementErrors.add(REQUIRED_DATA_ELEMENT_MISSING);
            }
        }

        return elementErrors.isEmpty();
    }

    public void validateSyntax(ElementDataHandler handler, ValidationEventHandler validationHandler, final StaEDIStreamLocation location, final boolean isComposite) {
        if (isComposite && composite == null) {
            // End composite but element is not composite in schema
            return;
        }

        final UsageNode structure = isComposite ? composite : segment;

        final int index;

        if (isComposite) {
            int componentPosition = location.getComponentPosition();

            if (componentPosition < 1) {
                index = 1;
            } else {
                index = componentPosition;
            }
        } else {
            index = location.getElementPosition();
        }

        final List<UsageNode> children = structure.getChildren();

        for (int i = index, max = children.size(); i < max; i++) {
            if (isComposite) {
                location.incrementComponentPosition();
            } else {
                location.incrementElementPosition();
            }

            handler.elementData(null, 0, 0);
        }

        for (EDISyntaxRule rule : structure.getSyntaxRules()) {
            final EDISyntaxRule.Type ruleType = rule.getType();
            SyntaxValidator validator = SyntaxValidator.getInstance(ruleType);
            validator.validate(rule, structure, validationHandler);
        }
    }
}
