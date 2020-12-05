/*******************************************************************************
 * Copyright 2020 xlate.io LLC, http://www.xlate.io
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

public class TradacomsDialect extends Dialect {

    public static final String STX = "STX";
    public static final String MHD = "MHD";

    private static final String[] EMPTY = new String[0];

    static final char DFLT_SEGMENT_TERMINATOR = '\'';
    static final char DFLT_DATA_ELEMENT_SEPARATOR = '+';
    static final char DFLT_COMPONENT_ELEMENT_SEPARATOR = ':';
    static final char DFLT_RELEASE_CHARACTER = '?';

    private String[] version;
    StringBuilder header;
    private int index = -1;

    private static final int TX_AGENCY = 0;
    private static final int TX_VERSION = 1;

    TradacomsDialect() {
        super(new String[2]);
        componentDelimiter = DFLT_COMPONENT_ELEMENT_SEPARATOR;
        elementDelimiter = DFLT_DATA_ELEMENT_SEPARATOR;
        decimalMark = 0;
        releaseIndicator = DFLT_RELEASE_CHARACTER;
        elementRepeater = 0;
        segmentDelimiter = DFLT_SEGMENT_TERMINATOR;
        segmentTagTerminator = '=';

        clearTransactionVersion();
    }

    @Override
    public String getHeaderTag() {
        return STX;
    }

    boolean initialize(CharacterSet characters) {
        String[] parsedVersion = parseVersion();

        if (parsedVersion.length > 1) {
            this.version = parsedVersion;
            initialized = true;
            characters.setClass(segmentDelimiter, CharacterClass.SEGMENT_DELIMITER);
        } else {
            initialized = false;
        }

        return initialized;
    }

    private String[] parseVersion() {
        int versionStart = 4; // 4 = length of "STX="
        int versionEnd = header.indexOf(String.valueOf(elementDelimiter), versionStart);

        if (versionEnd - versionStart > 1) {
            return header.substring(versionStart, versionEnd).split('\\' + String.valueOf(componentDelimiter));
        }

        return EMPTY;
    }

    @Override
    public String getStandard() {
        return Standards.TRADACOMS;
    }

    @Override
    public String[] getVersion() {
        return version;
    }

    @Override
    public boolean appendHeader(CharacterSet characters, char value) {
        boolean proceed = true;

        switch (++index) {
        case 0:
            header = new StringBuilder();
            break;
        case 3:
            /*
             * TRADACOMS delimiters are fixed. Do not set the element delimiter
             * until after the segment tag has been passed to prevent triggering
             * an "element data" event prematurely.
             */
            characters.setClass(segmentTagTerminator, CharacterClass.SEGMENT_TAG_DELIMITER);
            characters.setClass(componentDelimiter, CharacterClass.COMPONENT_DELIMITER);
            characters.setClass(elementDelimiter, CharacterClass.ELEMENT_DELIMITER);
            characters.setClass(releaseIndicator, CharacterClass.RELEASE_CHARACTER);
            break;
        default:
            break;
        }

        if (characters.isIgnored(value)) {
            index--;
        } else {
            header.append(value);
            proceed = processInterchangeHeader(characters, value);
        }

        return proceed;
    }

    boolean processInterchangeHeader(CharacterSet characters, char value) {
        if (segmentDelimiter == value) {
            rejected = !initialize(characters);
            return isConfirmed();
        }

        return true;
    }

    @Override
    protected void clearTransactionVersion() {
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
        if (MHD.contentEquals(location.getSegmentTag())) {
            messageHeaderElementData(data, location);
        }
    }

    void messageHeaderElementData(CharSequence data, Location location) {
        if (location.getElementPosition() == 1) {
            clearTransactionVersion();
        } else if (location.getElementPosition() == 2) {
            switch (location.getComponentPosition()) {
            case 1:
                transactionVersion[TX_AGENCY] = data.toString();
                updateTransactionVersionString(transactionVersion);
                break;
            case 2:
                transactionVersion[TX_VERSION] = data.toString();
                updateTransactionVersionString(transactionVersion);
                break;
            default:
                break;
            }
        }
    }

}
