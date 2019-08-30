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

import java.util.Collections;
import java.util.List;

class Structure extends BasicType implements EDIComplexType {

    private String code;
    private List<EDIReference> references;
    private List<EDISyntaxRule> syntaxRules;

    Structure(String id, EDIType.Type type, String code, List<Reference> references, List<SyntaxRestriction> syntaxRules) {
        super(id, type);
        this.code = code;
        this.references = Collections.unmodifiableList(references);
        this.syntaxRules = Collections.unmodifiableList(syntaxRules);
    }

    Structure(EDIComplexType other, List<EDIReference> references, List<EDISyntaxRule> syntaxRules) {
        super(other);
        this.code = other.getCode();
        this.references = Collections.unmodifiableList(references);
        this.syntaxRules = Collections.unmodifiableList(syntaxRules);
    }

    @Override
    public String toString() {
        StringBuilder buffer = new StringBuilder("id: ");
        buffer.append(super.id);
        buffer.append("\n, type: ");
        switch (super.type) {
        case COMPOSITE:
            buffer.append("composite");
            break;
        case LOOP:
            buffer.append("loop");
            break;
        case SEGMENT:
            buffer.append("segment");
            break;
        default:
            break;
        }
        buffer.append("\n, code: ");
        buffer.append(code);
        buffer.append("\n, references: [");
        for (EDIReference reference : references) {
            buffer.append("\n\t");
            buffer.append(reference);
        }
        buffer.append("\n]\n, syntaxRestrictions: ");
        for (EDISyntaxRule rule : syntaxRules) {
            buffer.append("\n\t");
            buffer.append(rule);
        }
        buffer.append("\n]\n");
        return buffer.toString();
    }

    @Override
    public String getCode() {
        return code;
    }

    @Override
    public List<EDIReference> getReferences() {
        return references;
    }

    @Override
    public List<EDISyntaxRule> getSyntaxRules() {
        return syntaxRules;
    }
}
