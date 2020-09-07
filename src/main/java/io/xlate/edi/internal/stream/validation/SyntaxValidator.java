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

import static io.xlate.edi.stream.EDIStreamEvent.ELEMENT_OCCURRENCE_ERROR;
import static io.xlate.edi.stream.EDIStreamValidationError.CONDITIONAL_REQUIRED_DATA_ELEMENT_MISSING;
import static io.xlate.edi.stream.EDIStreamValidationError.CONDITIONAL_REQUIRED_SEGMENT_MISSING;
import static io.xlate.edi.stream.EDIStreamValidationError.EXCLUSION_CONDITION_VIOLATED;
import static io.xlate.edi.stream.EDIStreamValidationError.SEGMENT_EXCLUSION_CONDITION_VIOLATED;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import io.xlate.edi.internal.stream.tokenization.ValidationEventHandler;
import io.xlate.edi.schema.EDIComplexType;
import io.xlate.edi.schema.EDIReference;
import io.xlate.edi.schema.EDISyntaxRule;
import io.xlate.edi.schema.EDIType;
import io.xlate.edi.stream.EDIStreamValidationError;

interface SyntaxValidator {

    static SyntaxValidator getInstance(EDISyntaxRule.Type type) {
        SyntaxValidator instance = null;

        switch (type) {
        case CONDITIONAL:
            instance = ConditionSyntaxValidator.INSTANCE;
            break;
        case EXCLUSION:
            instance = ExclusionSyntaxValidator.INSTANCE;
            break;
        case LIST:
            instance = ListSyntaxValidator.INSTANCE;
            break;
        case PAIRED:
            instance = PairedSyntaxValidator.INSTANCE;
            break;
        case REQUIRED:
            instance = RequiredSyntaxValidator.INSTANCE;
            break;
        case SINGLE:
            instance = SingleSyntaxValidator.INSTANCE;
            break;
        case FIRSTONLY:
            instance = FirstOnlySyntaxValidator.INSTANCE;
            break;
        }

        return instance;
    }

    static class SyntaxStatus {
        protected int elementCount = 0;
        protected boolean anchorPresent = false;
    }

    default SyntaxStatus scanSyntax(EDISyntaxRule syntax, List<UsageNode> children) {
        final SyntaxStatus status = new SyntaxStatus();
        final AtomicBoolean anchorPosition = new AtomicBoolean(true);

        syntax.getPositions()
              .stream()
              .filter(position -> position < children.size() + 1)
              .map(position -> children.get(position - 1))
              .forEach(node -> {
                  if (node.isUsed()) {
                      status.elementCount++;

                      if (anchorPosition.get()) {
                          status.anchorPresent = true;
                      }
                  }

                  anchorPosition.set(false);
              });

        return status;
    }

    default void signalConditionError(EDISyntaxRule syntax, UsageNode structure, ValidationEventHandler handler) {
        final List<UsageNode> children = structure.getChildren();
        final int limit = children.size() + 1;

        for (int position : syntax.getPositions()) {
            final boolean used;
            EDIReference typeReference;

            if (position < limit) {
                UsageNode node = children.get(position - 1);
                used = node.isUsed();
                typeReference = node.getLink();
            } else {
                used = false;
                typeReference = null;
            }

            if (!used) {
                if (structure.isNodeType(EDIType.Type.SEGMENT, EDIType.Type.COMPOSITE)) {
                    signalElementError(structure, typeReference, position, CONDITIONAL_REQUIRED_DATA_ELEMENT_MISSING, handler);
                } else if (typeReference != null) {
                    signalSegmentError(typeReference, CONDITIONAL_REQUIRED_SEGMENT_MISSING, handler);
                }
            }
        }
    }

    default void signalExclusionError(EDISyntaxRule syntax, UsageNode structure, ValidationEventHandler handler) {
        final List<UsageNode> children = structure.getChildren();
        final int limit = children.size() + 1;
        int tally = 0;

        for (int position : syntax.getPositions()) {
            if (position < limit
                    && children.get(position - 1).isUsed()
                    && ++tally > 1) {

                EDIReference typeReference = children.get(position - 1).getLink();

                if (structure.isNodeType(EDIType.Type.SEGMENT, EDIType.Type.COMPOSITE)) {
                    signalElementError(structure, typeReference, position, EXCLUSION_CONDITION_VIOLATED, handler);
                } else {
                    signalSegmentError(typeReference, SEGMENT_EXCLUSION_CONDITION_VIOLATED, handler);
                }
            }
        }
    }

    static void signalElementError(UsageNode structure,
                                   EDIReference typeReference,
                                   int position,
                                   EDIStreamValidationError error,
                                   ValidationEventHandler handler) {

        final int element = getElementPosition(structure, position);
        final int component = getComponentPosition(structure, position);

        handler.elementError(ELEMENT_OCCURRENCE_ERROR, error, typeReference, null, element, component, -1);
    }

    static void signalSegmentError(EDIReference typeReference, EDIStreamValidationError error, ValidationEventHandler handler) {
        EDIType type = typeReference.getReferencedType();

        if (!type.isType(EDIType.Type.SEGMENT)) {
            // Error is reported on the first sub-reference of a loop
            typeReference = ((EDIComplexType) type).getReferences().get(0);
            type = typeReference.getReferencedType();
        }

        handler.segmentError(type.getId(), typeReference, error);
    }

    static int getComponentPosition(UsageNode structure, int position) {
        return structure.isNodeType(EDIType.Type.COMPOSITE) ? position : -1;
    }

    static int getElementPosition(UsageNode structure, int position) {
        if (structure.isNodeType(EDIType.Type.COMPOSITE)) {
            return structure.getParent().getIndex() + 1;
        }

        return position;
    }

    default void validate(EDISyntaxRule syntax, UsageNode structure, ValidationEventHandler handler) {
        validate(syntax, structure, handler, scanSyntax(syntax, structure.getChildren()));
    }

    void validate(EDISyntaxRule syntax, UsageNode structure, ValidationEventHandler handler, SyntaxStatus status);
}
