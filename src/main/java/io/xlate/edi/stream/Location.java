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
     * Create a new copy of this instance
     *
     * @return a new {@link Location } instance with the same values of the instance being copied
     *
     * @since 1.11
     */
    Location copy();
}
