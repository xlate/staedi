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
import java.util.stream.Collectors;

import io.xlate.edi.schema.EDIComplexType;
import io.xlate.edi.schema.EDIReference;
import io.xlate.edi.schema.EDISyntaxRule;
import io.xlate.edi.schema.EDIType;

@SuppressWarnings("java:S2160") // Intentionally inherit 'equals' from superclass
class StructureType extends BasicType implements EDIComplexType {

    private static final String TOSTRING_FORMAT = "id: %s, type: %s, code: %s, references: [%s], syntaxRestrictions: [%s]";
    private String code;
    private List<EDIReference> references;
    private List<EDISyntaxRule> syntaxRules;

    StructureType(String id, EDIType.Type type, String code, List<EDIReference> references, List<EDISyntaxRule> syntaxRules) {
        super(id, type);
        Objects.requireNonNull(code, "EDIComplexType code must not be null");
        Objects.requireNonNull(references, "EDIComplexType references must not be null");
        Objects.requireNonNull(syntaxRules, "EDIComplexType id must not be null");
        this.code = code;
        this.references = Collections.unmodifiableList(new ArrayList<>(references));
        this.syntaxRules = Collections.unmodifiableList(new ArrayList<>(syntaxRules));
    }

    @Override
    public String toString() {
        return String.format(TOSTRING_FORMAT, getId(), getType(), code,
                             references.stream().map(r -> "{" + r + '}').collect(Collectors.joining(",")),
                             syntaxRules.stream().map(r -> "{" + r + '}').collect(Collectors.joining(",")));
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
