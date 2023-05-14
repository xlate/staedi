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
package io.xlate.edi.internal.stream;

import io.xlate.edi.stream.Location;

public class StaEDIStreamLocation extends LocationView implements Location {

    private boolean afterSegment = false;
    private boolean composite = false;
    private boolean repeating = false;
    private int repeatCount = -1;

    public StaEDIStreamLocation() {
        super();
    }

    public StaEDIStreamLocation(Location source) {
        super(source);
    }

    @Override
    public String toString() {
        return super.toString(afterSegment);
    }

    @Override
    public StaEDIStreamLocation copy() {
        StaEDIStreamLocation copy = new StaEDIStreamLocation(this);
        copy.afterSegment = this.afterSegment;
        copy.composite = this.composite;
        copy.repeating = this.repeating;
        copy.repeatCount = this.repeatCount;
        return copy;
    }

    public void set(Location source) {
        lineNumber = source.getLineNumber();
        columnNumber = source.getColumnNumber();
        characterOffset = source.getCharacterOffset();
        segmentPosition = source.getSegmentPosition();
        segmentTag = source.getSegmentTag();
        elementPosition = source.getElementPosition();
        componentPosition = source.getComponentPosition();
        elementOccurrence = source.getElementOccurrence();
    }

    public void setElementPosition(int elementPosition) {
        this.elementPosition = elementPosition;
    }

    public void setElementOccurrence(int elementOccurrence) {
        this.elementOccurrence = elementOccurrence;
    }

    public void setComponentPosition(int componentPosition) {
        this.componentPosition = componentPosition;
    }

    public void incrementOffset(int value) {
        this.characterOffset++;
        if (value == '\n') {
            this.lineNumber++;
            this.columnNumber = 0;
        }
        this.columnNumber++;
    }

    static int initOrIncrement(int position) {
        if (position < 0) {
            return 1;
        }
        return position + 1;
    }

    public void incrementSegmentPosition(String segmentTag) {
        this.segmentPosition = initOrIncrement(segmentPosition);
        this.segmentTag = segmentTag;
        clearSegmentLocations(false);
    }

    public void clearSegmentLocations(boolean afterSegment) {
        this.afterSegment = afterSegment;
        this.elementPosition = -1;
        this.elementOccurrence = -1;
        this.repeating = false;
        this.repeatCount = -1;
        clearComponentPosition();
    }

    public void incrementElementPosition() {
        this.elementPosition = initOrIncrement(elementPosition);
        this.elementOccurrence = 1;
        clearComponentPosition();
    }

    public void incrementElementOccurrence() {
        this.elementPosition = Math.max(elementPosition, 1);
        this.elementOccurrence = initOrIncrement(elementOccurrence);
        clearComponentPosition();
    }

    public void incrementComponentPosition() {
        this.componentPosition = initOrIncrement(componentPosition);
    }

    public void clearComponentPosition() {
        this.composite = false;
        this.componentPosition = -1;
    }

    public void setComposite(boolean composite) {
        this.composite = composite;
    }

    public void setRepeating(boolean repeating) {
        if (repeating) {
            // Encountered a repeat delimiter
            if (this.repeating) {
                // Previous delimiter was repeat, increment
                repeatCount++;
            } else {
                // First repeat delimiter for this element
                repeatCount = 0;
            }
        } else if (this.repeating) {
            // Previous delimiter was repeat, this one is not. The element just completed is a repeat
            repeatCount++;
        } else {
            // Repeat does not apply
            repeatCount = -1;
        }

        this.repeating = repeating;
    }

    public void incrementElement(boolean compositeBegin) {
        if (composite) {
            incrementComponentPosition();
        } else if (elementPosition < 0 || repeatCount == -1) {
            // First element of the segment or not a repeating element
            incrementElementPosition();
        } else if (repeating) {
            if (compositeBegin) {
                // Previous element delimiter was a repeater and the first component was encountered
                incrementElementOccurrence();
            } else if (repeatCount == 0) {
                // First element of the repeating series is in a new element position
                incrementElementPosition();
            } else {
                incrementElementOccurrence();
            }
        } else if (compositeBegin) {
            incrementElementPosition();
        } else {
            incrementElementOccurrence();
        }
    }
}
