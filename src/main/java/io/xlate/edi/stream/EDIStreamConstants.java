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

public interface EDIStreamConstants {

    public static class Standards {
        private Standards() {
        }

        public static final String X12 = "X12";
        public static final String EDIFACT = "EDIFACT";
    }

    public static class Delimiters {
        private Delimiters() {
        }

        public static final String SEGMENT = "io.xlate.edi.stream.delim.segment";
        public static final String DATA_ELEMENT = "io.xlate.edi.stream.delim.dataElement";
        public static final String COMPONENT_ELEMENT = "io.xlate.edi.stream.delim.componentElement";
        public static final String REPETITION = "io.xlate.edi.stream.delim.repetition";
        public static final String DECIMAL = "io.xlate.edi.stream.delim.decimal";
        public static final String RELEASE = "io.xlate.edi.stream.delim.release";
    }
}
