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

public interface Schema extends Iterable<EDIType> {

    public abstract EDIComplexType getMainLoop();

    public abstract EDIType getType(String name);

    public abstract boolean containsSegment(String name);

    /**
     * Attach a schema to this schema to be referenced by the parent type
     * provided and inserted before the provided child reference type.
     *
     * Any type defined by the included schema that are already defined by this
     * schema will be ignored.
     *
     * Neither this schema nor the included schema will be modified by this
     * operation. This method will return a new schema object which represents
     * the combination of the two.
     *
     * @param referenced
     *            the schema to include
     * @param parent
     *            the parent type which will reference the root element of the
     *            included schema
     * @param child
     *            the reference point prior to which the new schema's root type
     *            will be referenced
     * @return the new combined schema
     */
    public abstract Schema reference(Schema referenced,
                                     EDIComplexType parent,
                                     EDIReference child) throws EDISchemaException;
}
