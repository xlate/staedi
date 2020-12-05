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
package io.xlate.edi.internal.stream.tokenization;

public enum CharacterClass {

    /*
     * Characters are mapped into these character classes. This allows for a
     * significant reduction in the size of the state transition table.
     */
    SPACE(ClassSequence.sequence++),
    LATIN_A(ClassSequence.sequence++),
    LATIN_B(ClassSequence.sequence++),
    LATIN_D(ClassSequence.sequence++),
    LATIN_E(ClassSequence.sequence++),
    LATIN_I(ClassSequence.sequence++),
    LATIN_N(ClassSequence.sequence++),
    LATIN_S(ClassSequence.sequence++),
    LATIN_T(ClassSequence.sequence++),
    LATIN_U(ClassSequence.sequence++),
    LATIN_X(ClassSequence.sequence++),
    LATIN_Z(ClassSequence.sequence++),
    ALPHANUMERIC(ClassSequence.sequence++),
    SEGMENT_DELIMITER(ClassSequence.sequence++),
    ELEMENT_DELIMITER(ClassSequence.sequence++),
    COMPONENT_DELIMITER(ClassSequence.sequence++),
    ELEMENT_REPEATER(ClassSequence.sequence++),
    RELEASE_CHARACTER(ClassSequence.sequence++),

    WHITESPACE(ClassSequence.sequence++), /* Other white space */
    CONTROL(ClassSequence.sequence++), /* Control Characters */
    OTHER(ClassSequence.sequence++), /* Everything else */
    INVALID(ClassSequence.sequence++),
    SEGMENT_TAG_DELIMITER(ClassSequence.sequence++) /* Used for TRADACOMS only */;

    private static class ClassSequence {
        static int sequence = 0;
    }

    protected final int code;

    private CharacterClass(int code) {
        this.code = code;
    }
}
