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

public interface EDISyntaxRule {

    public enum Type {
        /**
         * X12: N/A
         * EDIFACT: (D1) One and only one
         */
        SINGLE,
        /**
         * X12: Type P
         * EDIFACT: (D2) All or none
         */
        PAIRED,
        /**
         * X12: Type R
         * EDIFACT: (D3) One or more
         */
        REQUIRED,
        /**
         * X12: Type E
         * EDIFACT: (D4) One or none
         */
        EXCLUSION,
        /**
         * X12: Type C
         * EDIFACT: (D5) If first, then all
         */
        CONDITIONAL,
        /**
         * X12: Type L
         * EDIFACT: (D6) If first, then at least one more
         */
        LIST,
        /**
         * X12: N/A
         * EDIFACT: (D7) If first, then none of the others
         */
        FIRSTONLY;

        public static Type fromString(String value) {
            for (Type entry : values()) {
                if (entry.name().equalsIgnoreCase(value)) {
                    return entry;
                }
            }
            throw new IllegalArgumentException("No enum constant for " + Type.class.getName() + "." + value);
        }
    }

    Type getType();

    List<Integer> getPositions();
}
