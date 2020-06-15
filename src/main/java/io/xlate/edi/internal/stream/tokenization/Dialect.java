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

public interface Dialect {

    String getStandard();

    String[] getVersion();

    void setHeaderTag(String tag);

    String getHeaderTag();

    boolean isConfirmed();

    boolean isRejected();

    boolean isServiceAdviceSegment(String tag);

    boolean appendHeader(CharacterSet characters, char value);

    char getSegmentTerminator();

    char getDataElementSeparator();

    char getComponentElementSeparator();

    char getRepetitionSeparator();

    char getReleaseIndicator();

    char getDecimalMark();

    /**
     * Notify the dialect of element data and its location in the stream. Does
     * not support binary elements.
     *
     * @param data
     *            the element data
     * @param location
     *            the location of the element
     */
    void elementData(CharSequence data, Location location);

    /**
     * Notify the dialect that a transaction is complete.
     */
    void transactionEnd();

    /**
     * Returns the identifying elements of the current transaction's version.
     *
     * @return the array of elements identifying the current transaction's version
     */
    String[] getTransactionVersion();

}
