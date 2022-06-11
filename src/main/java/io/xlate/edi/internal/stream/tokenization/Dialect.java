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

import java.util.Optional;

import io.xlate.edi.stream.Location;

public abstract class Dialect {

    protected final String[] transactionVersion;
    protected final int dialectStateCode;

    protected char segmentDelimiter;
    protected char segmentTagTerminator = '\0';
    protected char elementDelimiter;
    protected char decimalMark;
    protected char releaseIndicator;
    protected char componentDelimiter;
    protected char elementRepeater;

    protected boolean initialized;
    protected String rejectionMessage;

    protected String transactionType;
    protected String transactionVersionString;

    protected Dialect(int dialectStateCode, String[] initialTransactionVersion) {
        this.dialectStateCode = dialectStateCode;
        this.transactionVersion = initialTransactionVersion;
    }

    public static String getStandard(Dialect dialect) {
        return dialect != null ? dialect.getStandard() : "UNKNOWN";
    }

    public int getDialectStateCode() {
        return dialectStateCode;
    }

    public State getTagSearchState() {
        return State.TAG_SEARCH;
    }

    public char getComponentElementSeparator() {
        return componentDelimiter;
    }

    public char getDataElementSeparator() {
        return elementDelimiter;
    }

    public char getDecimalMark() {
        return decimalMark;
    }

    public char getReleaseIndicator() {
        return releaseIndicator;
    }

    public char getRepetitionSeparator() {
        return elementRepeater;
    }

    public char getSegmentTerminator() {
        return segmentDelimiter;
    }

    public char getSegmentTagTerminator() {
        return segmentTagTerminator;
    }

    public boolean isDecimalMark(char value) {
        return value == getDecimalMark();
    }

    public boolean isConfirmed() {
        return initialized;
    }

    public boolean isRejected() {
        return rejectionMessage != null;
    }

    public String getRejectionMessage() {
        return rejectionMessage;
    }

    /**
     * Check if the given segment tag is this dialect's service advice segment.
     * E.g. <code>UNA</code> resolves to <code>true</code> for EDIFACT.
     *
     * @param segmentTag the character tag of the segment
     * @return true when the segmentTag is the dialect's service segment,
     *         otherwise false
     */
    public boolean isServiceAdviceSegment(CharSequence segmentTag) {
        return false; // Service segment not used by default
    }

    public abstract String getStandard();

    public abstract String[] getVersion();

    public abstract String getHeaderTag();

    public abstract boolean appendHeader(CharacterSet characters, char value);

    /**
     * Check if the current state of the dialect is at a valid end of segment position.
     * If false, the reason will be given to the provided reasonHandler.
     */
    public abstract Optional<String> assertValidHeaderEnd();

    /**
     * Notify the dialect of element data and its location in the stream. Does
     * not support binary elements.
     *
     * @param data
     *            the element data
     * @param location
     *            the location of the element
     */
    public abstract void elementData(CharSequence data, Location location);

    protected abstract void clearTransactionVersion();

    /**
     * Notify the dialect that a transaction is complete.
     */
    public void transactionEnd() {
        transactionType = null;
        clearTransactionVersion();
    }

    /**
     * Notify the dialect that a group is complete.
     */
    public void groupEnd() {
        clearTransactionVersion();
    }

    /**
     * Returns the transaction type code, or null if not within a transaction
     *
     * @return the transaction type code, or null if not within a transaction
     *
     * @since 1.16
     */
    public String getTransactionType() {
        return transactionType;
    }

    /**
     * Returns the identifying elements of the current transaction's version.
     *
     * @return the array of elements identifying the current transaction's
     *         version
     */
    public String[] getTransactionVersion() {
        return transactionVersionString.isEmpty() ? null : transactionVersion;
    }

    /**
     * Returns the identifying elements of the current transaction's version as
     * a single String joined with period `.` characters.
     *
     * @return the String representation of the elements identifying the current
     *         transaction's version
     */
    public String getTransactionVersionString() {
        return transactionVersionString;
    }

}
