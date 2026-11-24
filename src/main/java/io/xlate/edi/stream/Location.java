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
package io.xlate.edi.stream;

import io.xlate.edi.schema.EDIType;

/**
 * Provides information on the location of an event.
 *
 * All the information provided by a Location is optional. For example an
 * application may only report line numbers.
 *
 * @version 1.0
 */
public interface Location {
    /**
     * Return the line number where the current event ends, returns -1 if none
     * is available.
     *
     * @return the current line number
     */
    int getLineNumber();

    /**
     * Return the column number where the current event ends, returns -1 if none
     * is available.
     *
     * @return the current column number
     */
    int getColumnNumber();

    /**
     * Return the byte or character offset into the input source this location
     * is pointing to. If the input source is a file or a byte stream then this
     * is the byte offset into that stream, but if the input source is a
     * character media then the offset is the character offset. Returns -1 if
     * there is no offset available.
     *
     * @return the current offset
     */
    int getCharacterOffset();

    /**
     * Return the segment position within the current stream, returns -1 if none
     * is available.
     *
     * @return the current segment position
     */
    int getSegmentPosition();

    /**
     * Return the segment tag within the current stream, returns null if none is
     * available.
     *
     * @return the current segment tag
     */
    String getSegmentTag();

    /**
     * Return the element position within the current segment, returns -1 if
     * none is available.
     *
     * @return the current element position
     */
    int getElementPosition();

    /**
     * Return the current occurrence number of a repeating data element, returns
     * 1 for non-repeating elements and -1 if otherwise not available.
     *
     * @return the current element occurrence
     */
    int getElementOccurrence();

    /**
     * Return the component data element position within the current composite
     * data element, returns -1 if none is available.
     *
     * @return the current component element position
     */
    int getComponentPosition();

    /**
     * Return the number of items the reader or writer has counted so far in the
     * given control structure. The item is the one the structure's trailer
     * counts: segments for a transaction, transactions for a group, and groups
     * or transactions for an interchange, as the control schema defines.
     * Returns -1 if the structure is not open at the current event or the count
     * is not available.
     *
     * <p>
     * When a {@link EDIStreamValidationError#CONTROL_COUNT_DOES_NOT_MATCH_ACTUAL_COUNT
     * CONTROL_COUNT_DOES_NOT_MATCH_ACTUAL_COUNT} error is reported for a
     * trailer, this is the actual count, and the error's text is the count
     * given in the trailer.
     *
     * @param scope
     *            the control structure: {@link EDIType.Type#INTERCHANGE
     *            INTERCHANGE}, {@link EDIType.Type#GROUP GROUP}, or
     *            {@link EDIType.Type#TRANSACTION TRANSACTION}
     * @return the current count for the structure, or -1 if none is available
     * @throws IllegalArgumentException
     *             if scope is null or is not one of the three values above
     *             (implementations that do not track control counts may
     *             return -1 instead)
     * @since 1.27
     */
    default int getControlCount(EDIType.Type scope) {
        return -1;
    }

    /**
     * Return the number of items counted so far in the innermost control
     * structure (transaction, group, or interchange) that is open at the
     * current event. Returns -1 if none is available.
     *
     * @return the current count for the innermost control structure
     * @see #getControlCount(EDIType.Type)
     * @since 1.27
     */
    default int getControlCount() {
        return -1;
    }

    /**
     * Return the control reference (control number) read from the header of
     * the given control structure, for example ISA13, GS06, or ST02 in X12.
     * Returns null if the structure is not open at the current event or the
     * header element has not been read yet.
     *
     * <p>
     * When a {@link EDIStreamValidationError#CONTROL_REFERENCE_MISMATCH
     * CONTROL_REFERENCE_MISMATCH} error is reported for a trailer, this is the
     * header's value, and the error's text is the value given in the trailer.
     *
     * @param scope
     *            the control structure: {@link EDIType.Type#INTERCHANGE
     *            INTERCHANGE}, {@link EDIType.Type#GROUP GROUP}, or
     *            {@link EDIType.Type#TRANSACTION TRANSACTION}
     * @return the header control reference for the structure, or null if none
     *         is available
     * @throws IllegalArgumentException
     *             if scope is null or is not one of the three values above
     *             (implementations that do not track control references may
     *             return null instead)
     * @since 1.27
     */
    default String getControlReference(EDIType.Type scope) {
        return null;
    }

    /**
     * Return the header control reference of the innermost control structure
     * (transaction, group, or interchange) that is open at the current event.
     * Returns null if none is available.
     *
     * @return the header control reference for the innermost control structure
     * @see #getControlReference(EDIType.Type)
     * @since 1.27
     */
    default String getControlReference() {
        return null;
    }

    /**
     * Create a new copy of this instance
     *
     * @return a new {@link Location } instance with the same values of the instance being copied
     *
     * @since 1.11
     */
    Location copy();
}
