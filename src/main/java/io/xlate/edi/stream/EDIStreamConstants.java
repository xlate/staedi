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
package io.xlate.edi.stream;

/**
 * Collection of constant values identifying the several EDI standards and the
 * delimiters used in processing EDI data streams.
 */
public interface EDIStreamConstants {

    /**
     * Defines the constant values possibly returned by
     * {@link EDIStreamReader#getStandard()} and
     * {@link EDIStreamWriter#getStandard()}.
     */
    public static class Standards {
        private Standards() {
        }

        /**
         * Constant name for the EDIFACT EDI Dialect
         */
        public static final String EDIFACT = "EDIFACT";

        /**
         * Constant name for the TRADACOMS EDI Dialect
         * @since 1.15
         */

        public static final String TRADACOMS = "TRADACOMS";

        /**
         * Constant name for the X12 EDI Dialect
         */
        public static final String X12 = "X12";
    }

    /**
     * Defines the constant values of EDI delimiters present in the maps
     * returned by
     * {@link EDIStreamReader#getDelimiters()}/{@link EDIStreamWriter#getDelimiters()}
     * and accepted as properties for output via
     * {@link EDIOutputFactory#setProperty(String, Object)}.
     */
    public static class Delimiters {
        private Delimiters() {
        }

        /**
         * Key for the delimiter used to terminate/end a segment.
         */
        public static final String SEGMENT = "io.xlate.edi.stream.delim.segment";

        /**
         * Key for the delimiter used to terminate/end a simple or composite
         * data element.
         */
        public static final String DATA_ELEMENT = "io.xlate.edi.stream.delim.dataElement";

        /**
         * Key for the delimiter used to terminate/end a component of a
         * composite element.
         */
        public static final String COMPONENT_ELEMENT = "io.xlate.edi.stream.delim.componentElement";

        /**
         * Key for the delimiter used to terminate/end a repeating data element.
         */
        public static final String REPETITION = "io.xlate.edi.stream.delim.repetition";

        /**
         * Key for the character used as the decimal point for non-integer
         * numeric element types.
         */
        public static final String DECIMAL = "io.xlate.edi.stream.delim.decimal";

        /**
         * Key for the character used as a release character, allowing the next
         * character in the EDI stream to be treated as data element text rather
         * than a delimiter.
         */
        public static final String RELEASE = "io.xlate.edi.stream.delim.release";
    }
}
