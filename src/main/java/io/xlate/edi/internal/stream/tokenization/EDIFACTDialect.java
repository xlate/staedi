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

public class EDIFACTDialect extends Dialect {

    public static final String UNA = "UNA";
    public static final String UNB = "UNB";

    private static final String[] EMPTY = new String[0];
    private static final int EDIFACT_UNA_LENGTH = 9;

    private String headerTag;
    private String[] version;
    StringBuilder header;
    private int index = -1;
    private int unbStart = -1;
    private boolean initialized;
    private boolean rejected;

    private static final int TX_AGENCY = 0;
    private static final int TX_VERSION = 1;
    private static final int TX_RELEASE = 2;
    private static final int TX_ASSIGNED_CODE = 3;
    private String[] transactionVersion = new String[4];
    private String transactionVersionString;

    EDIFACTDialect() {
        componentDelimiter = ':';
        elementDelimiter = '+';
        decimalMark = '.';
        releaseIndicator = '?';
        elementRepeater = '*';
        segmentDelimiter = '\'';

        clearTransactionVersion();
    }

    @Override
    public void setHeaderTag(String tag) {
        headerTag = tag;
    }

    @Override
    public String getHeaderTag() {
        return headerTag;
    }

    boolean initialize(CharacterSet characters) {
        String[] parsedVersion = parseVersion();

        if (parsedVersion.length > 0) {
            this.version = parsedVersion;

            characters.setClass(componentDelimiter, CharacterClass.COMPONENT_DELIMITER);
            characters.setClass(elementDelimiter, CharacterClass.ELEMENT_DELIMITER);

            if (releaseIndicator != ' ') {
                characters.setClass(releaseIndicator, CharacterClass.RELEASE_CHARACTER);
            } else {
                releaseIndicator = '\0';
            }

            if (elementRepeater != ' ') {
                characters.setClass(elementRepeater, CharacterClass.ELEMENT_REPEATER);
            } else {
                elementRepeater = '\0';
            }

            characters.setClass(segmentDelimiter, CharacterClass.SEGMENT_DELIMITER);
            initialized = true;
        } else {
            initialized = false;
        }

        return initialized;
    }

    private String[] parseVersion() {
        int versionStart = findVersionStart();
        int versionEnd = header.indexOf(String.valueOf(elementDelimiter), versionStart);

        if (versionEnd - versionStart > 1) {
            return header.substring(versionStart, versionEnd).split('\\' + String.valueOf(componentDelimiter));
        }

        return EMPTY;
    }

    int findVersionStart() {
        // Skip four characters: UNB<delim>
        return UNB.equals(headerTag) ? 4 : unbStart + 4;
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
        return UNA.equals(tag);
    }

    @Override
    public String getStandard() {
        return Standards.EDIFACT;
    }

    @Override
    public String[] getVersion() {
        return version;
    }

    @Override
    public boolean appendHeader(CharacterSet characters, char value) {
        if (++index == 0) {
            header = new StringBuilder();
        }

        header.append(value);

        if (UNB.equals(headerTag)) {
            return processInterchangeHeader(characters, value);
        }

        return processServiceStringAdvice(characters, value);
    }

    boolean processInterchangeHeader(CharacterSet characters, char value) {
        if (index == 0) {
            characters.setClass(componentDelimiter, CharacterClass.COMPONENT_DELIMITER);
            characters.setClass(releaseIndicator, CharacterClass.RELEASE_CHARACTER);
            characters.setClass(elementRepeater, CharacterClass.ELEMENT_REPEATER);
            return true;
        } else if (index == 3) {
            characters.setClass(elementDelimiter, CharacterClass.ELEMENT_DELIMITER);
            return true;
        } else if (segmentDelimiter == value) {
            characters.setClass(segmentDelimiter, CharacterClass.SEGMENT_DELIMITER);
            rejected = !initialize(characters);
            return isConfirmed();
        }

        return true;
    }

    boolean processServiceStringAdvice(CharacterSet characters, char value) {
        switch (index) {
        case 3:
            componentDelimiter = value;
            setCharacterClass(characters, CharacterClass.COMPONENT_DELIMITER, value, true);
            break;
        case 4:
            elementDelimiter = value;
            setCharacterClass(characters, CharacterClass.ELEMENT_DELIMITER, value, true);
            break;
        case 5:
            decimalMark = value;
            break;
        case 6:
            releaseIndicator = value;
            setCharacterClass(characters, CharacterClass.RELEASE_CHARACTER, value, false);
            break;
        case 7:
            elementRepeater = value;
            setCharacterClass(characters, CharacterClass.ELEMENT_REPEATER, value, false);
            break;
        case 8:
            segmentDelimiter = value;
            setCharacterClass(characters, CharacterClass.SEGMENT_DELIMITER, value, true);
            break;
        default:
            break;
        }

        if (index > EDIFACT_UNA_LENGTH) {
            if (unbStart > -1 && (index - unbStart) > 3) {
                if (value == elementDelimiter) {
                    rejected = !initialize(characters);
                    return isConfirmed();
                }
            } else if (value == 'B') {
                CharSequence un = header.subSequence(index - 2, index);

                if ("UN".contentEquals(un)) {
                    unbStart = index - 2;
                } else {
                    // Some other segment / element?
                    return false;
                }
            } else if (unbStart < 0 && value == elementDelimiter) {
                // Some other segment / element?
                return false;
            }
        }

        return true;
    }

    void setCharacterClass(CharacterSet characters, CharacterClass charClass, char value, boolean allowSpace) {
        if (value != ' ' || allowSpace) {
            characters.setClass(value, charClass);
        }
    }

    void clearTransactionVersion() {
        for (int i = 0; i < transactionVersion.length; i++) {
            transactionVersion[i] = "";
        }
        updateTransactionVersionString(null);
    }

    void updateTransactionVersionString(String[] transactionVersion) {
        transactionVersionString = transactionVersion != null ? String.join(".", transactionVersion) : "";
    }

    @Override
    public void elementData(CharSequence data, Location location) {
        if ("UNH".equals(location.getSegmentTag())) {
            if (location.getElementPosition() == 1) {
                clearTransactionVersion();
            } else if (location.getElementPosition() == 2) {
                switch (location.getComponentPosition()) {
                case 2:
                    transactionVersion[TX_VERSION] = data.toString();
                    break;
                case 3:
                    transactionVersion[TX_RELEASE] = data.toString();
                    break;
                case 4:
                    transactionVersion[TX_AGENCY] = data.toString();
                    updateTransactionVersionString(transactionVersion);
                    break;
                case 5:
                    transactionVersion[TX_ASSIGNED_CODE] = data.toString();
                    updateTransactionVersionString(transactionVersion);
                    break;
                default:
                    break;
                }
            }
        }
    }

    @Override
    public void transactionEnd() {
        clearTransactionVersion();
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
