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
package io.xlate.edi.stream.validation;

import io.xlate.edi.schema.EDISyntaxRule;
import io.xlate.edi.stream.EDIStreamEvent;
import io.xlate.edi.stream.EDIStreamValidationError;
import io.xlate.edi.stream.Location;
import io.xlate.edi.stream.internal.EventHandler;

import java.util.List;

abstract class SyntaxValidator {

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
        default:
            throw new IllegalArgumentException(
                                               "Unexpected syntax restriction type " + type + ".");
        }
    }

    protected class SyntaxStatus {
        protected int elementCount = 0;
        protected boolean anchorPresent = false;
    }

    abstract void validate(EDISyntaxRule syntax,
                           Location location,
                           List<UsageNode> children,
                           EventHandler handler);

    protected SyntaxStatus scanSyntax(EDISyntaxRule syntax,
                                      List<UsageNode> children) {

        SyntaxStatus status = new SyntaxStatus();
        boolean anchorPosition = true;

        for (int pos : syntax.getPositions()) {
            if (pos < children.size()) {
                UsageNode node = children.get(pos - 1);

                if (node.isUsed()) {
                    status.elementCount++;

                    if (anchorPosition) {
                        status.anchorPresent = true;
                    }
                }

                anchorPosition = false;
            } else {
                break;
            }
        }

        return status;
    }

    protected static void signalConditionError(EDISyntaxRule syntax,
                                               Location location,
                                               List<UsageNode> children,
                                               EventHandler handler) {

        for (int pos : syntax.getPositions()) {
            if (pos < children.size()) {
                UsageNode node = children.get(pos - 1);

                if (!node.isUsed()) {
                    final int element;
                    int component = location.getComponentPosition();

                    if (component > -1) {
                        element = location.getElementPosition();
                        component = pos;
                    } else {
                        element = pos;
                    }

                    handler.elementError(EDIStreamEvent.ELEMENT_OCCURRENCE_ERROR,
                                         EDIStreamValidationError.CONDITIONAL_REQUIRED_DATA_ELEMENT_MISSING,
                                         element,
                                         component,
                                         location.getElementOccurrence());
                }
            } else {
                break;
            }
        }
    }

    protected static void signalExclusionError(EDISyntaxRule syntax,
                                               Location location,
                                               List<UsageNode> children,
                                               EventHandler handler) {

        int tally = 0;

        for (int pos : syntax.getPositions()) {
            if (pos < children.size()) {
                UsageNode node = children.get(pos - 1);

                if (node.isUsed() && ++tally > 1) {
                    final int element;
                    int component = location.getComponentPosition();

                    if (component > -1) {
                        element = location.getElementPosition();
                        component = pos - 1;
                    } else {
                        element = pos - 1;
                    }

                    handler.elementError(EDIStreamEvent.ELEMENT_OCCURRENCE_ERROR,
                                         EDIStreamValidationError.EXCLUSION_CONDITION_VIOLATED,
                                         element,
                                         component,
                                         location.getElementOccurrence());
                }
            } else {
                break;
            }
        }
    }
}
