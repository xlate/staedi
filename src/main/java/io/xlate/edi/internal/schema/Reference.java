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
import java.util.function.BiFunction;
import java.util.function.Supplier;

import io.xlate.edi.schema.EDIReference;
import io.xlate.edi.schema.EDIType;

class Reference implements EDIReference {

    private static final String TOSTRING_FORMAT = "refId: %s, minOccurs: %d, maxOccurs: %d, type: { %s }";

    private String refId;
    private String refTag;
    private EDIType referencedType;

    final int minOccurs;
    final int maxOccurs;
    final List<Version> versions;

    static class Version extends VersionedProperty {
        final Integer minOccurs;
        final Integer maxOccurs;

        Version(String minVersion, String maxVersion, Integer minOccurs, Integer maxOccurs) {
            super(minVersion, maxVersion);
            this.minOccurs = minOccurs;
            this.maxOccurs = maxOccurs;
        }

        public int getMinOccurs(Reference defaultElement) {
            return minOccurs != null ? minOccurs.intValue() : defaultElement.getMinOccurs();
        }

        public int getMaxOccurs(Reference defaultElement) {
            return maxOccurs != null ? maxOccurs.intValue() : defaultElement.getMaxOccurs();
        }
    }

    Reference(String refId, String refTag, int minOccurs, int maxOccurs, List<Version> versions) {
        this.refId = refId;
        this.refTag = refTag;
        this.minOccurs = minOccurs;
        this.maxOccurs = maxOccurs;
        this.versions = Collections.unmodifiableList(new ArrayList<>(versions));
    }

    Reference(String refId, String refTag, int minOccurs, int maxOccurs) {
        this(refId, refTag, minOccurs, maxOccurs, Collections.emptyList());
    }

    Reference(EDIType referencedType, int minOccurs, int maxOccurs) {
        this.referencedType = referencedType;
        this.minOccurs = minOccurs;
        this.maxOccurs = maxOccurs;
        this.versions = Collections.emptyList();
    }

    <T> T getVersionAttribute(String version, BiFunction<Version, Reference, T> versionedSupplier, Supplier<T> defaultSupplier) {
        for (Version ver : versions) {
            if (ver.appliesTo(version)) {
                return versionedSupplier.apply(ver, this);
            }
        }

        return defaultSupplier.get();
    }

    @Override
    public String toString() {
        return String.format(TOSTRING_FORMAT, refId, minOccurs, maxOccurs, referencedType);
    }

    String getRefId() {
        return refId;
    }

    String getRefTag() {
        return refTag;
    }

    @Override
    public EDIType getReferencedType() {
        return referencedType;
    }

    void setReferencedType(EDIType referencedType) {
        this.referencedType = referencedType;
    }

    @Override
    public int getMinOccurs() {
        return minOccurs;
    }

    @Override
    public int getMaxOccurs() {
        return maxOccurs;
    }

    @Override
    public boolean hasVersions() {
        return !versions.isEmpty();
    }

    @Override
    public int getMinOccurs(String version) {
        return getVersionAttribute(version, Version::getMinOccurs, this::getMinOccurs);
    }

    @Override
    public int getMaxOccurs(String version) {
        return getVersionAttribute(version, Version::getMaxOccurs, this::getMaxOccurs);
    }

}
