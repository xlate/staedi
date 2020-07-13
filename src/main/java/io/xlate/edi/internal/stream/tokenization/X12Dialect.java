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
import io.xlate.edi.stream.Location;

public class X12Dialect implements Dialect {

    private static final String ISA = "ISA";
    private static final String ISX = "ISX";
    private static final String GS = "GS";
    private static final String ST = "ST";

    private static final int RELEASE_ISX_SEGMENT = 704; // 007040 (Version 7, release 4)
    private static final int RELEASE_ELEMENT_I65 = 402; // 004020 (Version 4, release 2)

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

    private CharacterSet characters;
    private char segmentDelimiter = DFLT_SEGMENT_TERMINATOR;
    private char elementDelimiter = DFLT_DATA_ELEMENT_SEPARATOR;
    private char decimalMark = '.';
    private char releaseIndicator = 0;
    private char componentDelimiter = DFLT_COMPONENT_ELEMENT_SEPARATOR;
    private char elementRepeater = DFLT_REPETITION_SEPARATOR;

    private static final int TX_AGENCY = 0;
    private static final int TX_VERSION = 1;

    private String[] transactionVersion = new String[2];
    private String transactionVersionString;
    private String agencyCode;
    private String groupVersion;

    X12Dialect() {
        clearTransactionVersion();
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

        componentDelimiter = header[X12_COMPONENT_OFFSET];
        segmentDelimiter = header[X12_SEGMENT_OFFSET];
        elementRepeater = header[X12_REPEAT_OFFSET];

        characters.setClass(componentDelimiter, CharacterClass.COMPONENT_DELIMITER);
        characters.setClass(segmentDelimiter, CharacterClass.SEGMENT_DELIMITER);

        version = new String[] { new String(header, 84, 5) };

        if (numericVersion() >= RELEASE_ELEMENT_I65) {
            characters.setClass(elementRepeater, CharacterClass.ELEMENT_REPEATER);
        } else {
            /*
             * Exception parsing the version or older version - the ELEMENT_REPEATER
             * will not be set.
             */
            elementRepeater = '\0';
        }

        this.characters = characters;
        initialized = true;
        return true;
    }

    int numericVersion() {
        try {
            return Integer.parseInt(version[0]);
        } catch (NumberFormatException nfe) {
            return 0;
        }
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
                elementDelimiter = value;
                characters.setClass(elementDelimiter, CharacterClass.ELEMENT_DELIMITER);
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
        return componentDelimiter;
    }

    @Override
    public char getDataElementSeparator() {
        return elementDelimiter;
    }

    @Override
    public char getDecimalMark() {
        return decimalMark;
    }

    @Override
    public char getReleaseIndicator() {
        return releaseIndicator;
    }

    @Override
    public char getRepetitionSeparator() {
        return elementRepeater;
    }

    @Override
    public char getSegmentTerminator() {
        return segmentDelimiter;
    }

    void clearTransactionVersion() {
        agencyCode = "";
        groupVersion = "";
        transactionVersion[TX_AGENCY] = agencyCode;
        transactionVersion[TX_VERSION] = groupVersion;
        updateTransactionVersionString(null);
    }

    void updateTransactionVersionString(String[] transactionVersion) {
        transactionVersionString = transactionVersion != null ? String.join(".", transactionVersion) : "";
    }

    @Override
    public void elementData(CharSequence data, Location location) {
        if (ISX.equals(location.getSegmentTag()) && numericVersion() >= RELEASE_ISX_SEGMENT) {
            if (location.getElementPosition() == 1 && data.length() == 1) {
                releaseIndicator = data.charAt(0);
                characters.setClass(releaseIndicator, CharacterClass.RELEASE_CHARACTER);
            }
        } else if (GS.equals(location.getSegmentTag())) {
            switch (location.getElementPosition()) {
            case 1:
                clearTransactionVersion();
                break;
            case 7:
                agencyCode = data.toString();
                break;
            case 8:
                groupVersion = data.toString();
                transactionVersion[TX_AGENCY] = agencyCode;
                transactionVersion[TX_VERSION] = groupVersion;
                updateTransactionVersionString(transactionVersion);
                break;
            default:
                break;
            }
        } else if (ST.equals(location.getSegmentTag()) && location.getElementPosition() == 3 && data.length() > 0) {
            transactionVersion[TX_VERSION] = data.toString();
            updateTransactionVersionString(transactionVersion);
        }
    }

    @Override
    public void transactionEnd() {
        transactionVersion[TX_VERSION] = groupVersion;
        updateTransactionVersionString(transactionVersion);
    }

    @Override
    public void groupEnd() {
        clearTransactionVersion();
    }

    @Override
    public String[] getTransactionVersion() {
        return transactionVersionString.isEmpty() ? null : transactionVersion;
    }

    @Override
    public String getTransactionVersionString() {
        return transactionVersionString;
    }
}
