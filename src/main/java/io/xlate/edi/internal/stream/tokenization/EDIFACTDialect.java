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

    static final char DFLT_SEGMENT_TERMINATOR = '\'';
    static final char DFLT_DATA_ELEMENT_SEPARATOR = '+';
    static final char DFLT_COMPONENT_ELEMENT_SEPARATOR = ':';
    static final char DFLT_REPETITION_SEPARATOR = '*';
    static final char DFLT_RELEASE_CHARACTER = '?';
    static final char DFLT_DECIMAL_MARK = '.';
    static final char ALT_DECIMAL_MARK = ',';

    private static final int EDIFACT_UNA_LENGTH = 9;

    private final String headerTag;
    private String[] version;
    StringBuilder header;
    private int index = -1;
    private int unbStart = -1;
    private boolean ignoreDecimalAdvice;

    private static final int TX_AGENCY = 0;
    private static final int TX_VERSION = 1;
    private static final int TX_RELEASE = 2;
    private static final int TX_ASSIGNED_CODE = 3;

    EDIFACTDialect(String headerTag) {
        super(new String[4]);
        componentDelimiter = DFLT_COMPONENT_ELEMENT_SEPARATOR;
        elementDelimiter = DFLT_DATA_ELEMENT_SEPARATOR;
        decimalMark = DFLT_DECIMAL_MARK;
        releaseIndicator = DFLT_RELEASE_CHARACTER;
        elementRepeater = DFLT_REPETITION_SEPARATOR;
        segmentDelimiter = DFLT_SEGMENT_TERMINATOR;
        this.headerTag = headerTag;

        clearTransactionVersion();
    }

    @Override
    public String getHeaderTag() {
        return headerTag;
    }

    @Override
    public boolean isDecimalMark(char value) {
        if (!this.ignoreDecimalAdvice) {
            return super.isDecimalMark(value);
        }

        return value == DFLT_DECIMAL_MARK || value == ALT_DECIMAL_MARK;
    }

    boolean initialize(CharacterSet characters) {
        String[] parsedVersion = parseVersion();

        if (parsedVersion.length > 1) {
            this.version = parsedVersion;
            final String syntaxVersion = this.version[1];
            this.ignoreDecimalAdvice = syntaxVersion.compareTo("4") >= 0;

            characters.setClass(componentDelimiter, CharacterClass.COMPONENT_DELIMITER);
            characters.setClass(elementDelimiter, CharacterClass.ELEMENT_DELIMITER);

            if (syntaxVersion.compareTo("4") >= 0 || releaseIndicator != ' ') {
                // Must not be blank for version 4 and above, may be blank before version 4 if not used
                characters.setClass(releaseIndicator, CharacterClass.RELEASE_CHARACTER);
            } else {
                releaseIndicator = '\0';
            }

            if (syntaxVersion.compareTo("4") >= 0) {
                // Must not be blank for version 4 and above
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
        final int versionStart = findVersionStart();
        String versionComposite = findVersionString(versionStart, elementDelimiter);

        if (versionComposite == null) {
            // Handle the case where the segment was terminated prematurely (zero or one element)
            versionComposite = findVersionString(versionStart, segmentDelimiter);
        }

        return versionComposite.split('\\' + String.valueOf(componentDelimiter));
    }

    int findVersionStart() {
        // Skip four characters: UNB<delim>
        return UNB.equals(headerTag) ? 4 : unbStart + 4;
    }

    String findVersionString(int versionStart, char delimiter) {
        final int versionEnd = header.indexOf(String.valueOf(delimiter), versionStart);

        if (versionEnd - versionStart > -1) {
            return header.substring(versionStart, versionEnd);
        }

        return null;
    }

    @Override
    public boolean isServiceAdviceSegment(CharSequence tag) {
        return UNA.contentEquals(tag);
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
        boolean proceed = true;

        if (++index == 0) {
            header = new StringBuilder();
        }

        if (UNB.equals(headerTag)) {
            if (characters.isIgnored(value)) {
                index--;
            } else {
                header.append(value);
                proceed = processInterchangeHeader(characters, value);
            }
        } else {
            header.append(value);
            proceed = processServiceStringAdvice(characters, value);
        }

        return proceed;
    }

    boolean processInterchangeHeader(CharacterSet characters, char value) {
        if (index == 0) {
            characters.setClass(componentDelimiter, CharacterClass.COMPONENT_DELIMITER);
        } else if (index == 3) {
            /*
             * Do not set the element delimiter until after the segment tag has been passed
             * to prevent triggering an "element data" event prematurely.
             */
            characters.setClass(elementDelimiter, CharacterClass.ELEMENT_DELIMITER);
        } else if (segmentDelimiter == value) {
            rejected = !initialize(characters);
            return isConfirmed();
        }

        return true;
    }

    boolean processServiceStringAdvice(CharacterSet characters, char value) {
        boolean proceed = true;

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
            // Do not set the character class until initialize() is executed
            break;
        case 8:
            segmentDelimiter = value;
            setCharacterClass(characters, CharacterClass.SEGMENT_DELIMITER, value, true);
            break;
        default:
            break;
        }

        if (index > EDIFACT_UNA_LENGTH) {
            if (characters.isIgnored(value)) {
                header.deleteCharAt(index--);
            } else if (isIndexBeyondUNBFirstElement()) {
                if (value == elementDelimiter || value == segmentDelimiter) {
                    rejected = !initialize(characters);
                    proceed = isConfirmed();
                }
            } else if (value == 'B') {
                CharSequence un = header.subSequence(index - 2, index);

                if ("UN".contentEquals(un)) {
                    unbStart = index - 2;
                } else {
                    // Some other segment / element?
                    proceed = false;
                }
            } else if (isUnexpectedSegmentDetected(value)) {
                // Some other segment / element?
                proceed = false;
            }
        }

        return proceed;
    }

    boolean isIndexBeyondUNBFirstElement() {
        return unbStart > -1 && (index - unbStart) > 3;
    }

    boolean isUnexpectedSegmentDetected(int value) {
        return unbStart < 0 && value == elementDelimiter;
    }

    void setCharacterClass(CharacterSet characters, CharacterClass charClass, char value, boolean allowSpace) {
        if (value != ' ' || allowSpace) {
            characters.setClass(value, charClass);
        }
    }

    @Override
    public void clearTransactionVersion() {
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
        switch (location.getSegmentTag()) {
        case "UNG":
            groupHeaderElementData(data, location);
            break;
        case "UNH":
            messageHeaderElementData(data, location);
            break;
        default:
            break;
        }
    }

    void groupHeaderElementData(CharSequence data, Location location) {
        if (location.getElementPosition() == 1) {
            clearTransactionVersion();
        } else if (location.getElementPosition() == 6) {
            transactionVersion[TX_AGENCY] = data.toString();
        } else if (location.getElementPosition() == 7) {
            switch (location.getComponentPosition()) {
            case 1:
                transactionVersion[TX_VERSION] = data.toString();
                updateTransactionVersionString(transactionVersion);
                break;
            case 2:
                transactionVersion[TX_RELEASE] = data.toString();
                updateTransactionVersionString(transactionVersion);
                break;
            case 3:
                transactionVersion[TX_ASSIGNED_CODE] = data.toString();
                updateTransactionVersionString(transactionVersion);
                break;
            default:
                break;
            }
        }
    }

    void messageHeaderElementData(CharSequence data, Location location) {
        if (location.getElementPosition() == 1) {
            clearTransactionVersion();
        } else if (location.getElementPosition() == 2) {
            switch (location.getComponentPosition()) {
            case 1:
                transactionType = data.toString();
                break;
            case 2:
                transactionVersion[TX_VERSION] = data.toString();
                updateTransactionVersionString(transactionVersion);
                break;
            case 3:
                transactionVersion[TX_RELEASE] = data.toString();
                updateTransactionVersionString(transactionVersion);
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
