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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Supplier;

import io.xlate.edi.schema.EDISimpleType;

//java:S107 : Constructor has 8 arguments
//java:S2160 : Intentionally inherit 'equals' from superclass
@SuppressWarnings({ "java:S107", "java:S2160" })
class ElementType extends BasicType implements EDISimpleType {

    private static final String TOSTRING_FORMAT = "id: %s, type: %s, base: %s, code: %s, minLength: %d, maxLength: %d, values: %s";

    final Base base;
    final String code;
    final int number;
    final long minLength;
    final long maxLength;
    final Set<String> values;
    final List<Version> versions;

    static class Version {
        final String minVersion;
        final String maxVersion;
        final Long minLength;
        final Long maxLength;
        final Set<String> values;

        Version(String minVersion, String maxVersion, Long minLength, Long maxLength, Set<String> values) {
            super();
            this.minVersion = minVersion;
            this.maxVersion = maxVersion;
            this.minLength = minLength;
            this.maxLength = maxLength;
            this.values = values;
        }

        boolean appliesTo(String version) {
            return minVersionIncludes(version) && maxVersionIncludes(version);
        }

        boolean minVersionIncludes(String version) {
            return minVersion.trim().isEmpty() || minVersion.compareTo(version) <= 0;
        }

        boolean maxVersionIncludes(String version) {
            return maxVersion.trim().isEmpty() || maxVersion.compareTo(version) >= 0;
        }

        public long getMinLength(ElementType defaultElement) {
            return minLength != null ? minLength.longValue() : defaultElement.getMinLength();
        }

        public long getMaxLength(ElementType defaultElement) {
            return maxLength != null ? maxLength.longValue() : defaultElement.getMaxLength();
        }

        public Set<String> getValueSet(ElementType defaultElement) {
            return values != null ? values : defaultElement.getValueSet();
        }
    }

    ElementType(String id, Base base, String code, int number, long minLength, long maxLength, Set<String> values, List<Version> versions) {
        super(id, Type.ELEMENT);
        this.base = base;
        this.code = code;
        this.number = number;
        this.minLength = minLength;
        this.maxLength = maxLength;
        this.values = Collections.unmodifiableSet(new LinkedHashSet<>(values));
        this.versions = Collections.unmodifiableList(new ArrayList<>(versions));
    }

    <T> T getVersionAttribute(String version, BiFunction<Version, ElementType, T> versionedSupplier, Supplier<T> defaultSupplier) {
        for (Version ver : versions) {
            if (ver.appliesTo(version)) {
                return versionedSupplier.apply(ver, this);
            }
        }

        return defaultSupplier.get();
    }

    @Override
    public String toString() {
        return String.format(TOSTRING_FORMAT, getId(), getType(), base, code, minLength, maxLength, values);
    }

    @Override
    public Base getBase() {
        return base;
    }

    @Override
    public String getCode() {
        return code;
    }

    @Override
    public boolean hasVersions() {
        return !versions.isEmpty();
    }

    /**
     * @see io.xlate.edi.schema.EDISimpleType#getNumber()
     * @deprecated
     */
    @SuppressWarnings({ "java:S1123", "java:S1133" })
    @Override
    @Deprecated
    public int getNumber() {
        return number;
    }

    @Override
    public long getMinLength() {
        return minLength;
    }

    @Override
    public long getMinLength(String version) {
        return getVersionAttribute(version, Version::getMinLength, this::getMinLength);
    }

    @Override
    public long getMaxLength() {
        return maxLength;
    }

    @Override
    public long getMaxLength(String version) {
        return getVersionAttribute(version, Version::getMaxLength, this::getMaxLength);
    }

    @Override
    public Set<String> getValueSet() {
        return values;
    }

    @Override
    public Set<String> getValueSet(String version) {
        return getVersionAttribute(version, Version::getValueSet, this::getValueSet);
    }
}
