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
import java.util.LinkedHashSet;
import java.util.Set;

class Element extends BasicType implements EDISimpleType {

    private Base base;
    private int number;
    private int minLength;
    private int maxLength;
    private Set<String> values;

    Element(String id, Base base, int number, int minLength, int maxLength) {
        this(id, base, number, minLength, maxLength, Collections.emptySet());
    }

    Element(String id, Base base, int number, int minLength, int maxLength, Set<String> values) {
        super(id, Type.ELEMENT);
        this.base = base;
        this.number = number;
        this.minLength = minLength;
        this.maxLength = maxLength;
        this.values = Collections.unmodifiableSet(new LinkedHashSet<>(values));
    }

    @Override
    public String toString() {
        StringBuilder buffer = new StringBuilder("id: ");
        buffer.append(getId());
        buffer.append(", type: element");
        buffer.append(", base: ");
        buffer.append(base);
        buffer.append(", number: ");
        buffer.append(number);
        buffer.append(", minLength: ");
        buffer.append(minLength);
        buffer.append(", maxLength: ");
        buffer.append(maxLength);
        buffer.append(", values: ");
        buffer.append(values);
        return buffer.toString();
    }

    @Override
    public Base getBase() {
        return base;
    }

    @Override
    public int getNumber() {
        return number;
    }

    @Override
    public int getMinLength() {
        return minLength;
    }

    @Override
    public int getMaxLength() {
        return maxLength;
    }

    @Override
    public Set<String> getValueSet() {
        return values;
    }
}
