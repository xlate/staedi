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

import java.util.ArrayList;
import java.util.List;

import io.xlate.edi.internal.stream.StaEDIStreamLocation;
import io.xlate.edi.internal.stream.tokenization.Dialect;
import io.xlate.edi.internal.stream.tokenization.EventHandler;
import io.xlate.edi.schema.EDIComplexType;
import io.xlate.edi.schema.EDIReference;
import io.xlate.edi.schema.EDISimpleType;
import io.xlate.edi.schema.EDISyntaxRule;
import io.xlate.edi.schema.EDIType;
import io.xlate.edi.schema.Schema;
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

    private final List<String> mandatory = new ArrayList<>();
    private final List<EDIStreamValidationError> elementErrors = new ArrayList<>(5);

    private int depth = 1;
    private boolean complete = false;

    public Validator(Schema schema, Schema containerSchema) {
        this.schema = schema;
        this.containerSchema = containerSchema;
        root = buildTree(schema.getMainLoop());
        correctSegment = segment = root.getFirstChild();
    }

    public boolean isComplete() {
        return complete;
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

    private static EDIReference referenceOf(final EDIComplexType root) {
        return new EDIReference() {
            @Override
            public EDIType getReferencedType() {
                return root;
            }

            @Override
            public int getMinOccurs() {
                return 1;
            }

            @Override
            public int getMaxOccurs() {
                return 1;
            }
        };
    }

    private static UsageNode buildTree(final EDIComplexType root) {
        return buildTree(null, referenceOf(root), -1);
    }

    private static UsageNode buildTree(UsageNode parent, EDIReference link, int index) {

        EDIType referencedNode = link.getReferencedType();

        UsageNode node = new UsageNode(parent, link, index);

        if (!(referencedNode instanceof EDIComplexType)) {
            return node;
        }

        EDIComplexType structure = (EDIComplexType) referencedNode;

        List<? extends EDIReference> children = structure.getReferences();
        List<UsageNode> childUsages = node.getChildren();

        int childIndex = -1;

        for (EDIReference child : children) {
            childUsages.add(buildTree(node, child, ++childIndex));
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

    private void completeLoops(EventHandler handler, int d, UsageNode node) {
        while (this.depth < d--) {
            node = node.getParent();
            handler.loopEnd(node.getCode());
        }
    }

    public void validateSegment(EventHandler handler, CharSequence tag) {
        segmentExpected = true;

        final int startDepth = this.depth;
        final UsageNode startNode = correctSegment;

        UsageNode current = startNode;
        mandatory.clear();
        complete = false;

        scan: while (current != null) {
            switch (current.getNodeType()) {
            case SEGMENT:
                if (this.handleSegment(tag, current, startDepth, startNode, handler)) {
                    break scan;
                }
                break;

            case GROUP:
            case TRANSACTION:
            case LOOP:
                if (this.handleLoop(tag, current, startDepth, startNode, handler)) {
                    break scan;
                }
                break;

            default:
                break;
            }

            if (!current.hasMinimumUsage()) {
                /*
                 * The schema segment has not met it's minimum usage
                 * requirement.
                 */
                switch (current.getNodeType()) {
                case SEGMENT:
                    mandatory.add(current.getId());
                    break;
                case GROUP:
                case TRANSACTION:
                case LOOP:
                    mandatory.add(current.getFirstChild().getId());
                    break;
                default:
                    break;
                }
            }

            UsageNode next = current.getNextSibling();

            if (next != null) {
                current = next;
                continue scan;
            }

            if (this.depth == startDepth && current != startNode) {
                /*
                 * End of the loop; try to see if we can find the segment among
                 * the siblings of the last good segment. If the last good
                 * segment was the final segment of the loop, do not search
                 * here. Rather, go up a level and continue searching from
                 * there.
                 */
                next = current.getSiblingById(tag);

                if (next != null && !next.isFirstChild()) {
                    mandatory.clear();
                    handler.segmentError(next.getId(),
                                         EDIStreamValidationError.SEGMENT_NOT_IN_PROPER_SEQUENCE);

                    next.incrementUsage();

                    if (next.exceedsMaximumUsage()) {
                        handler.segmentError(next.getId(),
                                             EDIStreamValidationError.SEGMENT_EXCEEDS_MAXIMUM_USE);
                    }

                    segment = next;
                    break scan;
                }
            }

            if (this.depth > 1) {
                current = current.getParent();
                this.depth--;
            } else {
                current = this.root.getFirstChild();

                if (!current.getId().contentEquals(tag)) {
                    final String tagString = tag.toString();

                    if (containerSchema != null && containerSchema.containsSegment(tagString)) {
                        // The segment is defined in the containing schema. Handle missing mandatory
                        // segments and complete any open loops.
                        handleMissingMandatory(handler);
                        completeLoops(handler, startDepth, startNode);
                        complete = true;
                    } else {
                        // Unexpected segment... must reset our position!
                        segmentExpected = false;
                        this.depth = startDepth;
                        mandatory.clear();

                        if (schema.containsSegment(tagString)) {
                            handler.segmentError(tag,
                                                 EDIStreamValidationError.UNEXPECTED_SEGMENT);
                        } else {
                            handler.segmentError(tag,
                                                 EDIStreamValidationError.SEGMENT_NOT_IN_DEFINED_TRANSACTION_SET);
                        }
                    }

                    break scan; // Wasn't found; cut our losses and go back.
                }
            }
        }

        handleMissingMandatory(handler);
    }

    boolean handleSegment(CharSequence tag, UsageNode current, int startDepth, UsageNode startNode, EventHandler handler) {
        if (!current.getId().contentEquals(tag)) {
            /*
             * The schema segment does not match the segment tag found
             * in the stream.
             */
            return false;
        }

        if (current.isUsed() && current.isFirstChild()) {
            /*
             * The current segment is the first segment in the loop and
             * the loop has previous occurrences. Scan all segments in
             * the loop to determine if any segments in the previous
             * occurrence did not meet the minimum usage requirements.
             */
            UsageNode parent = current.getParent();

            for (UsageNode sibling : parent.getChildren()) {
                if (!sibling.hasMinimumUsage()) {
                    mandatory.add(sibling.getId());
                }

                sibling.reset();
            }

            if (parent.isNodeType(EDIType.Type.LOOP)) {
                String loopId = parent.getCode();
                handler.loopEnd(loopId);
                handler.loopBegin(loopId);
            }
        }

        completeLoops(handler, startDepth, startNode);
        current.incrementUsage();
        current.resetChildren();

        if (current.exceedsMaximumUsage()) {
            handleMissingMandatory(handler);
            handler.segmentError(
                                 current.getId(),
                                 EDIStreamValidationError.SEGMENT_EXCEEDS_MAXIMUM_USE);
        }

        correctSegment = segment = current;
        return true;
    }

    boolean handleLoop(CharSequence tag, UsageNode current, int startDepth, UsageNode startNode, EventHandler handler) {
        if (!current.getFirstChild().getId().contentEquals(tag)) {
            return false;
        }

        completeLoops(handler, startDepth, startNode);
        handler.loopBegin(current.getCode());
        current.incrementUsage();

        if (current.exceedsMaximumUsage()) {
            handleMissingMandatory(handler);
            handler.segmentError(tag, EDIStreamValidationError.LOOP_OCCURS_OVER_MAXIMUM_TIMES);
        }

        correctSegment = segment = startLoop(current);
        return true;
    }

    private void handleMissingMandatory(EventHandler handler) {
        for (String id : mandatory) {
            handler.segmentError(id, EDIStreamValidationError.MANDATORY_SEGMENT_MISSING);
        }
        mandatory.clear();
    }

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
            elementErrors.add(EDIStreamValidationError.TOO_MANY_DATA_ELEMENTS);
            this.element = null;
            return false;
        } else if (!element.isNodeType(EDIType.Type.COMPOSITE)) {
            this.element.incrementUsage();

            if (this.element.exceedsMaximumUsage()) {
                elementErrors.add(EDIStreamValidationError.TOO_MANY_REPETITIONS);
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
            elementErrors.add(EDIStreamValidationError.TOO_MANY_DATA_ELEMENTS);
            return false;
        }

        this.composite.incrementUsage();

        if (this.composite.exceedsMaximumUsage()) {
            elementErrors.add(EDIStreamValidationError.TOO_MANY_REPETITIONS);
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
                elementErrors.add(EDIStreamValidationError.TOO_MANY_DATA_ELEMENTS);
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
                elementErrors.add(EDIStreamValidationError.TOO_MANY_COMPONENTS);
            } else {
                if (componentIndex == 0) {
                    this.element.resetChildren();
                }

                if (componentIndex < element.getChildren().size()) {
                    if (valueReceived || !derivedComposite) {
                        this.element = this.element.getChild(componentIndex);
                    }
                } else {
                    elementErrors.add(EDIStreamValidationError.TOO_MANY_COMPONENTS);
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
                    elementErrors.add(EDIStreamValidationError.TOO_MANY_REPETITIONS);
                }
            }

            this.element.validate(dialect, value, elementErrors);
        } else {
            if (!element.hasMinimumUsage()) {
                elementErrors.add(EDIStreamValidationError.REQUIRED_DATA_ELEMENT_MISSING);
            }
        }

        return elementErrors.isEmpty();
    }

    public void validateSyntax(EventHandler handler, final StaEDIStreamLocation location, final boolean isComposite) {
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
            validator.validate(rule, structure, handler);
        }
    }
}
