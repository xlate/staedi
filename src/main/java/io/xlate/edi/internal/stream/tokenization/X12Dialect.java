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

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import io.xlate.edi.stream.EDIStreamConstants.Standards;
import io.xlate.edi.stream.Location;

public class X12Dialect extends Dialect {

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

    private static final Integer[] X12_ISA_TOKENS = { 3, 6, 17, 20, 31, 34, 50, 53, 69, 76, 81, 83, 89, 99, 101, 103 };
    private static final Set<Integer> elementDelimiterOffsets = new HashSet<>(Arrays.asList(X12_ISA_TOKENS));

    private String[] version;
    char[] header;
    private int index = -1;

    private CharacterSet characters;
    private static final int TX_AGENCY = 0;
    private static final int TX_VERSION = 1;

    private String agencyCode;
    private String groupVersion;

    X12Dialect() {
        super(State.DialectCode.X12, new String[2]);
        segmentDelimiter = DFLT_SEGMENT_TERMINATOR;
        elementDelimiter = DFLT_DATA_ELEMENT_SEPARATOR;
        decimalMark = '.';
        releaseIndicator = 0;
        componentDelimiter = DFLT_COMPONENT_ELEMENT_SEPARATOR;
        elementRepeater = DFLT_REPETITION_SEPARATOR;

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
        for (int i = 0, m = X12_ISA_LENGTH; i < m; i++) {
            if (elementDelimiterOffsets.contains(i)) {
                if (elementDelimiter != header[i]) {
                    rejectionMessage = String.format("Element delimiter '%s' required in position %d of X12 header but not found", elementDelimiter, i + 1);
                    return false;
                }
            } else {
                if (elementDelimiter == header[i]) {
                    rejectionMessage = String.format("Unexpected element delimiter value '%s' in X12 header position %d", elementDelimiter, i + 1);
                    return false;
                }
            }
        }

        componentDelimiter = header[X12_COMPONENT_OFFSET];
        segmentDelimiter = header[X12_SEGMENT_OFFSET];
        elementRepeater = header[X12_REPEAT_OFFSET];

        characters.setClass(componentDelimiter, CharacterClass.COMPONENT_DELIMITER);
        characters.setClass(segmentDelimiter, CharacterClass.SEGMENT_DELIMITER);

        version = new String[] { new String(header, 84, 5) };

        if (numericVersion() >= RELEASE_ELEMENT_I65 && characters.isValidDelimiter(elementRepeater)) {
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
    public String getHeaderTag() {
        return ISA;
    }

    @Override
    public boolean appendHeader(CharacterSet characters, char value) {
        index++;

        switch (index) {
        case 0:
            header = new char[X12_ISA_LENGTH];
            break;
        case X12_ELEMENT_OFFSET:
            elementDelimiter = value;
            characters.setClass(elementDelimiter, CharacterClass.ELEMENT_DELIMITER);
            break;
        case X12_REPEAT_OFFSET:
        case X12_COMPONENT_OFFSET:
        case X12_SEGMENT_OFFSET:
            break;
        default:
            if (characters.isIgnored(value)) {
                // Discard control character if not used as a delimiter
                index--;
                return true;
            }
            break;
        }

        header[index] = value;
        boolean proceed = true;

        if (index == X12_SEGMENT_OFFSET) {
            initialize(characters);
            proceed = isConfirmed();
        }

        return proceed;
    }

    @Override
    public void clearTransactionVersion() {
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
        } else if (ST.equals(location.getSegmentTag())) {
            switch (location.getElementPosition()) {
            case 1:
                transactionType = data.toString();
                break;
            case 3:
                if (data.length() > 0) {
                    transactionVersion[TX_VERSION] = data.toString();
                    updateTransactionVersionString(transactionVersion);
                }
                break;
            default:
                break;
            }
        }
    }

    @Override
    public void transactionEnd() {
        transactionType = null;
        transactionVersion[TX_VERSION] = groupVersion;
        updateTransactionVersionString(transactionVersion);
    }

}
