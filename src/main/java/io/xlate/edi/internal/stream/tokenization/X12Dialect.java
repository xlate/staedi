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

import io.xlate.edi.stream.EDIStreamConstants.Standards;

public class X12Dialect implements Dialect {

    private static final String ISA = "ISA";
    static final char DFLT_SEGMENT_TERMINATOR = '~';
    static final char DFLT_DATA_ELEMENT_SEPARATOR = '*';
    static final char DFLT_COMPONENT_ELEMENT_SEPARATOR = ':';
    static final char DFLT_REPETITION_SEPARATOR = '^';

    private static final int X12_ISA_LENGTH = 106;
    private static final int X12_ELEMENT_OFFSET = 3;
    private static final int X12_COMPONENT_OFFSET = 104;
    private static final int X12_SEGMENT_OFFSET = 105;
    private static final int X12_REPEAT_OFFSET = 82;

    private static final int[] X12_ISA_TOKENS = { 3, 6, 17, 20, 31, 34, 50, 53, 69, 76, 81, 83, 89, 99, 101, 103 };

    private String[] version;
    char[] header;
    private int index = -1;
    private boolean initialized;
    private boolean rejected;
    private char sd = DFLT_SEGMENT_TERMINATOR;
    private char ed = DFLT_DATA_ELEMENT_SEPARATOR;
    private char cd = DFLT_COMPONENT_ELEMENT_SEPARATOR;
    private char er = DFLT_REPETITION_SEPARATOR;

    X12Dialect() {
    }

    @Override
    public String getStandard() {
        return Standards.X12;
    }

    @Override
    public String[] getVersion() {
        return version;
    }

    boolean initialize(CharacterSet characters) {
        final char ELEMENT = header[X12_ELEMENT_OFFSET];
        int e = 0;

        for (int i = 0, m = X12_ISA_LENGTH; i < m; i++) {
            if (ELEMENT == header[i] && X12_ISA_TOKENS[e++] != i) {
                return false;
            }
        }

        cd = header[X12_COMPONENT_OFFSET];
        sd = header[X12_SEGMENT_OFFSET];
        er = header[X12_REPEAT_OFFSET];

        characters.setClass(cd, CharacterClass.COMPONENT_DELIMITER);
        characters.setClass(sd, CharacterClass.SEGMENT_DELIMITER);

        try {
            version = new String[] { new String(header, 84, 5) };

            if (Integer.parseInt(version[0]) >= 402) {
                characters.setClass(er, CharacterClass.ELEMENT_REPEATER);
            } else {
                er = '\0';
            }
        } catch (@SuppressWarnings("unused") NumberFormatException nfe) {
            /*
             * Ignore exception - the ELEMENT_REPEATER will not be set due to a
             * non-numeric version.
             */
        }

        initialized = true;
        return true;
    }

    @Override
    public void setHeaderTag(String tag) {
        // No operation, can only be ISA
    }

    @Override
    public String getHeaderTag() {
        return ISA;
    }

    @Override
    public boolean isConfirmed() {
        return initialized;
    }

    @Override
    public boolean isRejected() {
        return rejected;
    }

    @Override
    public boolean isServiceAdviceSegment(String tag) {
        return false; // X12 does not use a service advice string
    }

    @Override
    public boolean appendHeader(CharacterSet characters, char value) {
        index++;

        if (index < X12_ISA_LENGTH) {
            switch (index) {
            case 0:
                header = new char[X12_ISA_LENGTH];
                break;
            case X12_ELEMENT_OFFSET:
                ed = value;
                characters.setClass(ed, CharacterClass.ELEMENT_DELIMITER);
                break;
            default:
                break;
            }

            header[index] = value;

            if (index == X12_ISA_LENGTH - 1) {
                rejected = !initialize(characters);
                return isConfirmed();
            }
        } else {
            rejected = true;
            return false;
        }

        return true;
    }

    @Override
    public char getComponentElementSeparator() {
        return cd;
    }

    @Override
    public char getDataElementSeparator() {
        return ed;
    }

    @Override
    public char getDecimalMark() {
        return '.';
    }

    @Override
    public char getReleaseIndicator() {
        return 0;
    }

    @Override
    public char getRepetitionSeparator() {
        return er;
    }

    @Override
    public char getSegmentTerminator() {
        return sd;
    }
}
