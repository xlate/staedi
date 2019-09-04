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

import java.util.List;

import io.xlate.edi.schema.EDISyntaxRule;
import io.xlate.edi.stream.EDIStreamEvent;
import io.xlate.edi.stream.EDIStreamValidationError;
import io.xlate.edi.stream.internal.EventHandler;

class ExclusionSyntaxValidator extends SyntaxValidator {

    private static final ExclusionSyntaxValidator singleton = new ExclusionSyntaxValidator();

    private ExclusionSyntaxValidator() {
    }

    static ExclusionSyntaxValidator getInstance() {
        return singleton;
    }

    static void signalExclusionError(EDISyntaxRule syntax, UsageNode structure, EventHandler handler) {
        final List<UsageNode> children = structure.getChildren();
        final int limit = children.size() + 1;
        int tally = 0;

        for (int position : syntax.getPositions()) {
            if (position < limit && children.get(position - 1).isUsed() && ++tally > 1) {
                final int element = getElementPosition(structure, position);
                final int component = getComponentPosition(structure, position);

                handler.elementError(EDIStreamEvent.ELEMENT_OCCURRENCE_ERROR,
                                     EDIStreamValidationError.EXCLUSION_CONDITION_VIOLATED,
                                     element,
                                     component,
                                     -1);
            }
        }
    }

    @Override
    void validate(EDISyntaxRule syntax, UsageNode structure, EventHandler handler) {
        SyntaxStatus status = scanSyntax(syntax, structure.getChildren());

        if (status.elementCount > 1) {
            signalExclusionError(syntax, structure, handler);
        }
    }
}
