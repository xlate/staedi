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

public interface EDIReference {

    EDIType getReferencedType();

    int getMinOccurs();

    int getMaxOccurs();

    /**
     * Returns true if this element has additional version(s) defined beyond the
     * default. Versions may be used to specify different minimum/maximum
     * occurrence restrictions that only apply to specific versions of a
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
     * Retrieve the minOccurs attribute for a particular version of the element.
     *
     * The default implementation returns the default (un-versioned) value for
     * the element.
     *
     * @param version
     *            the version to select
     * @return the minOccurs attribute for version
     *
     * @since 1.8
     */
    default int getMinOccurs(String version) {
        return getMinOccurs();
    }

    /**
     * Retrieve the maxOccurs attribute for a particular version of the element.
     *
     * The default implementation returns the default (un-versioned) value for
     * the element.
     *
     * @param version
     *            the version to select
     * @return the maxOccurs attribute for version
     *
     * @since 1.8
     */
    default int getMaxOccurs(String version) {
        return getMaxOccurs();
    }

    /**
     * Retrieve the title for this reference, if available.
     *
     * @return the reference's title
     *
     * @since 1.10
     */
    String getTitle();

    /**
     * Retrieve the description for this reference, if available.
     *
     * @return the reference's description
     *
     * @since 1.10
     */
    String getDescription();

}
