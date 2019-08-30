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
package io.xlate.edi.schema;

import java.util.ArrayList;
import java.util.List;

class SyntaxRestriction implements EDISyntaxRule {

    private EDISyntaxRule.Type type;
    private List<Integer> positions;

    SyntaxRestriction(EDISyntaxRule.Type type, List<Integer> positions) {
        super();
        this.type = type;
        this.positions = new ArrayList<>(positions);
    }

    @Override
    public String toString() {
        StringBuilder buffer = new StringBuilder("type: ");
        switch (type) {
        case CONDITIONAL:
            buffer.append("conditional");
            break;
        case EXCLUSION:
            buffer.append("exclusion");
            break;
        case LIST:
            buffer.append("list");
            break;
        case PAIRED:
            buffer.append("paired");
            break;
        case REQUIRED:
            buffer.append("required");
            break;
        }
        buffer.append(", positions: ");
        buffer.append(positions);
        return buffer.toString();
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
