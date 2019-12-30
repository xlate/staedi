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
package io.xlate.edi.internal.schema;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import io.xlate.edi.schema.EDISyntaxRule;

class SyntaxRestriction implements EDISyntaxRule {

    private static final String TOSTRING_FORMAT = "type: %s, positions: %s";
    private EDISyntaxRule.Type type;
    private List<Integer> positions;

    SyntaxRestriction(EDISyntaxRule.Type type, List<Integer> positions) {
        Objects.requireNonNull(type, "syntax rule type must not be null");
        Objects.requireNonNull(positions, "syntax rule positions must not be null");

        if (positions.isEmpty()) {
            throw new IllegalArgumentException("syntax rule positions must not be em");
        }

        this.type = type;
        this.positions = new ArrayList<>(positions);
    }

    @Override
    public String toString() {
        return String.format(TOSTRING_FORMAT, type, positions);
    }

    @Override
    public EDISyntaxRule.Type getType() {
        return type;
    }

    @Override
    public List<Integer> getPositions() {
        return positions;
    }
}
