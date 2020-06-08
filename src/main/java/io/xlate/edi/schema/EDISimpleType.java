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

public interface EDISimpleType extends EDIType {

    public enum Base {
        STRING,
        NUMERIC,
        DECIMAL,
        DATE,
        TIME,
        BINARY,
        IDENTIFIER;
    }

    Base getBase();

    /**
     * Retrieve the element reference number for this type.
     *
     * @return the element reference number as declared in the EDI schema
     *
     * @deprecated (since 1.8, for removal) use {@link #getCode()} and
     *             <code>code</code> attribute instead
     */
    @SuppressWarnings({ "java:S1123", "java:S1133" })
    @Deprecated /*(forRemoval = true, since = "1.8")*/
    int getNumber();

    long getMinLength();

    long getMaxLength();

    Set<String> getValueSet();

}
