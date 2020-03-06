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

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

import io.xlate.edi.schema.EDISimpleType;

class ElementType extends BasicType implements EDISimpleType {

    private static final String TOSTRING_FORMAT = "id: %s, type: %s, base: %s, number: %d, minLength: %d, maxLength: %d, values: %s";
    private Base base;
    private int number;
    private long minLength;
    private long maxLength;
    private Set<String> values;

    ElementType(String id, Base base, int number, long minLength, long maxLength, Set<String> values) {
        super(id, Type.ELEMENT);
        this.base = base;
        this.number = number;
        this.minLength = minLength;
        this.maxLength = maxLength;
        this.values = Collections.unmodifiableSet(new LinkedHashSet<>(values));
    }

    @Override
    public String toString() {
        return String.format(TOSTRING_FORMAT, getId(), getType(), base, number, minLength, maxLength, values);
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
    public long getMinLength() {
        return minLength;
    }

    @Override
    public long getMaxLength() {
        return maxLength;
    }

    @Override
    public Set<String> getValueSet() {
        return values;
    }
}
