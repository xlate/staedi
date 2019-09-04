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
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import io.xlate.edi.schema.EDIComplexType;
import io.xlate.edi.schema.EDIReference;
import io.xlate.edi.schema.EDISyntaxRule;
import io.xlate.edi.schema.EDIType;

class Structure extends BasicType implements EDIComplexType {

    private String code;
    private List<EDIReference> references;
    private List<EDISyntaxRule> syntaxRules;

    Structure(String id, EDIType.Type type, String code, List<EDIReference> references, List<EDISyntaxRule> syntaxRules) {
        super(id, type);
        Objects.requireNonNull(code, "EDIComplexType code must not be null");
        Objects.requireNonNull(references, "EDIComplexType references must not be null");
        Objects.requireNonNull(syntaxRules, "EDIComplexType id must not be null");
        this.code = code;
        this.references = Collections.unmodifiableList(new ArrayList<>(references));
        this.syntaxRules = Collections.unmodifiableList(new ArrayList<>(syntaxRules));
    }

    Structure(EDIComplexType other, List<EDIReference> references, List<EDISyntaxRule> syntaxRules) {
        this(other.getId(), other.getType(), other.getCode(), references, syntaxRules);
    }

    @Override
    public String toString() {
        StringBuilder buffer = new StringBuilder("id: ");
        buffer.append(getId());
        buffer.append(", type: ");
        buffer.append(getType());
        buffer.append(", code: ");
        buffer.append(code);
        buffer.append(", references: [");
        for (EDIReference reference : references) {
            buffer.append('{');
            buffer.append(reference);
            buffer.append('}');
        }
        buffer.append("], syntaxRestrictions: [");
        for (EDISyntaxRule rule : syntaxRules) {
            buffer.append('{');
            buffer.append(rule);
            buffer.append('}');
        }
        buffer.append(']');
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
