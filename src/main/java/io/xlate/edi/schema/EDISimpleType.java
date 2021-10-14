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

import java.util.Set;

/**
 * An interface representing the schema of an EDI simple type, AKA an element.
 */
public interface EDISimpleType extends EDIType {

    public enum Base {
        STRING,
        NUMERIC,
        DECIMAL,
        DATE,
        TIME,
        BINARY,
        IDENTIFIER;

        public static Base fromString(String value) {
            for (Base entry : values()) {
                if (entry.name().equalsIgnoreCase(value)) {
                    return entry;
                }
            }
            throw new IllegalArgumentException("No enum constant for " + Base.class.getName() + "." + value);
        }
    }

    Base getBase();

    /**
     * Returns the <i>scale</i> of this {@code EDISimpleType} when the
     * <i>base</i> is <i>NUMERIC</i>. The scale is the number of digits to the
     * right of an implied decimal point.
     *
     * @return the scale of this {@code EDISimpleType} if <i>NUMERIC</i>,
     *         otherwise null.
     *
     * @since 1.13
     */
    default Integer getScale() {
        return null;
    }

    /**
     * Returns true if this element has additional version(s) defined beyond the
     * default. Versions may be used to specify different minimum/maximum length
     * restrictions or enumerated values that only apply to specific versions a
     * transaction.
     *
     * @return true if this element has version(s), otherwise false
     *
     * @since 1.8
     */
    default boolean hasVersions() {
        return false;
    }

    /**
     * Retrieve the element reference number for this type.
     *
     * @return the element reference number as declared in the EDI schema, or
     *         <code>-1</code> if not declared
     *
     * @deprecated (since 1.8, for removal) use {@link #getCode()} and
     *             <code>code</code> attribute instead
     */
    @SuppressWarnings({ "java:S1123", "java:S1133" })
    @Deprecated /*(forRemoval = true, since = "1.8")*/
    int getNumber();

    /**
     * Retrieve the minLength attribute of the element.
     *
     * @return the minLength attribute
     */
    long getMinLength();

    /**
     * Retrieve the minLength attribute for a particular version of the element.
     *
     * The default implementation returns the default (un-versioned) value for
     * the element.
     *
     * @param version
     *            the version to select
     * @return the minLength attribute for version
     *
     * @since 1.8
     */
    default long getMinLength(String version) {
        return getMinLength();
    }

    /**
     * Retrieve the maxLength attribute of the element.
     *
     * @return the maxLength attribute
     */
    long getMaxLength();

    /**
     * Retrieve the maxLength attribute for a particular version of the element.
     *
     * The default implementation returns the default (un-versioned) value for
     * the element.
     *
     * @param version
     *            the version to select
     * @return the maxLength attribute for version
     *
     * @since 1.8
     */
    default long getMaxLength(String version) {
        return getMaxLength();
    }

    /**
     * Retrieve the set of enumerated values allowed for this element.
     *
     * @return the set of enumerated values allowed for this element
     */
    Set<String> getValueSet();

    /**
     * Retrieve the set of enumerated values allowed for this element for a
     * particular version of the element.
     *
     * The default implementation returns the default (un-versioned) value for
     * the element.
     *
     * @param version
     *            the version to select
     * @return the set of enumerated values allowed for this element for version
     *
     * @since 1.8
     */
    default Set<String> getValueSet(String version) {
        return getValueSet();
    }

}
