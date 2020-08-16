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

public interface EDIType {

    public enum Type {
        INTERCHANGE,
        GROUP,
        TRANSACTION,
        LOOP,
        SEGMENT,
        COMPOSITE,
        ELEMENT;
    }

    String getId();

    String getCode();

    Type getType();

    default boolean isType(Type type) {
        return getType() == type;
    }

    /**
     * Retrieve the title for this type, if available.
     *
     * @return the type's title
     *
     * @since 1.10
     */
    String getTitle();

    /**
     * Retrieve the description for this type, if available.
     *
     * @return the type's description
     *
     * @since 1.10
     */
    String getDescription();

}
