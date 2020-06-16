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

import java.util.List;

/**
 * An interface representing the schema of an EDI complex type such as a loop,
 * segment, or composite element.
 */
public interface EDIComplexType extends EDIType {

    /**
     * Retrieve the {@link EDIReference}s (child elements) for a this complex
     * type.
     *
     * @return the references (child elements) without regard to version
     */
    List<EDIReference> getReferences();

    /**
     * Retrieve the {@link EDISyntaxRule}s associated with this EDIComplexType.
     *
     * @return a list of associated {@link EDISyntaxRule}s
     */
    List<EDISyntaxRule> getSyntaxRules();

}
