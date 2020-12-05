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

import io.xlate.edi.stream.Location;

public abstract class Dialect {

    protected char segmentDelimiter;
    protected char segmentTagTerminator = '\0';
    protected char elementDelimiter;
    protected char decimalMark;
    protected char releaseIndicator;
    protected char componentDelimiter;
    protected char elementRepeater;

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

    public abstract String getStandard();

    public abstract String[] getVersion();

    public abstract String getHeaderTag();

    public abstract boolean isConfirmed();

    public abstract boolean isRejected();

    public abstract boolean isServiceAdviceSegment(CharSequence tag);

    public abstract boolean appendHeader(CharacterSet characters, char value);

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

    /**
     * Notify the dialect that a transaction is complete.
     */
    public abstract void transactionEnd();

    /**
     * Notify the dialect that a group is complete.
     */
    public abstract void groupEnd();

    /**
     * Returns the identifying elements of the current transaction's version.
     *
     * @return the array of elements identifying the current transaction's
     *         version
     */
    public abstract String[] getTransactionVersion();

    /**
     * Returns the identifying elements of the current transaction's version as
     * a single String joined with period `.` characters.
     *
     * @return the String representation of the elements identifying the current
     *         transaction's version
     */
    public abstract String getTransactionVersionString();
}
