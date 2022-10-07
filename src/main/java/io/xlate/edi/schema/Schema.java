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

import io.xlate.edi.schema.implementation.LoopImplementation;

public interface Schema extends Iterable<EDIType> {

    /**
     * Retrieve the {@link EDIComplexType} that is the entry point of the
     * standard schema.
     *
     * @return the standard schema root type
     *
     * @deprecated use {@link #getStandard()} instead
     */
    @SuppressWarnings({ "java:S1123", "java:S1133" })
    @Deprecated /*(forRemoval = true, since = "1.2")*/
    default EDIComplexType getMainLoop() {
        return getStandard();
    }

    /**
     * Retrieve the {@link EDIComplexType} that is the entry point of the
     * standard schema.
     *
     * @return the standard schema root type
     *
     */
    EDIComplexType getStandard();

    /**
     * Retrieve the {@link LoopImplementation} that is the entry point of the
     * implementation schema.
     *
     * @return the implementation schema root type
     *
     */
    LoopImplementation getImplementation();

    /**
     * Retrieve an {@link EDIType} by name.
     *
     * @param name name of the type (e.g. segment tag or an element's unique identifier)
     * @return the type having the name requested, null if not present in this schema.
     */
    EDIType getType(String name);

    /**
     * Determine if the named segment is present in this schema.
     *
     * @param name segment tag
     * @return true if a segment with the given name is present, otherwise false
     */
    boolean containsSegment(String name);

}
