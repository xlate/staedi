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

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import io.xlate.edi.internal.stream.tokenization.ValidationEventHandler;
import io.xlate.edi.schema.EDISyntaxRule;
import io.xlate.edi.schema.EDIType;
import io.xlate.edi.stream.EDIStreamEvent;
import io.xlate.edi.stream.EDIStreamValidationError;

interface SyntaxValidator {

    static SyntaxValidator getInstance(EDISyntaxRule.Type type) {
        switch (type) {
        case CONDITIONAL:
            return ConditionSyntaxValidator.getInstance();
        case EXCLUSION:
            return ExclusionSyntaxValidator.getInstance();
        case LIST:
            return ListSyntaxValidator.getInstance();
        case PAIRED:
            return PairedSyntaxValidator.getInstance();
        case REQUIRED:
            return RequiredSyntaxValidator.getInstance();
        case SINGLE:
            return SingleSyntaxValidator.getInstance();
        default:
            throw new IllegalArgumentException("Unexpected syntax restriction type " + type + ".");
        }
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
            final String referenceCode;

            if (position < limit) {
                UsageNode node = children.get(position - 1);
                used = node.isUsed();
                referenceCode = node.getCode();
            } else {
                used = false;
                referenceCode = null;
            }

            if (!used) {
                final int element = getElementPosition(structure, position);
                final int component = getComponentPosition(structure, position);

                handler.elementError(EDIStreamEvent.ELEMENT_OCCURRENCE_ERROR,
                                     EDIStreamValidationError.CONDITIONAL_REQUIRED_DATA_ELEMENT_MISSING,
                                     referenceCode,
                                     null,
                                     element,
                                     component,
                                     -1);
            }
        }
    }

    default void signalExclusionError(EDISyntaxRule syntax, UsageNode structure, ValidationEventHandler handler) {
        final List<UsageNode> children = structure.getChildren();
        final int limit = children.size() + 1;
        int tally = 0;

        for (int position : syntax.getPositions()) {
            if (position < limit && children.get(position - 1).isUsed() && ++tally > 1) {
                final int element = getElementPosition(structure, position);
                final int component = getComponentPosition(structure, position);

                handler.elementError(EDIStreamEvent.ELEMENT_OCCURRENCE_ERROR,
                                     EDIStreamValidationError.EXCLUSION_CONDITION_VIOLATED,
                                     structure.getCode(),
                                     null,
                                     element,
                                     component,
                                     -1);
            }
        }
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

    void validate(EDISyntaxRule syntax, UsageNode structure, ValidationEventHandler handler);
}
