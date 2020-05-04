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

    private boolean repeated = false;

    public StaEDIStreamLocation() {
        super();
    }

    public StaEDIStreamLocation(Location source) {
        super(source);
    }

    public StaEDIStreamLocation copy() {
        StaEDIStreamLocation copy = new StaEDIStreamLocation();
        copy.lineNumber = this.lineNumber;
        copy.columnNumber = this.columnNumber;
        copy.characterOffset = this.characterOffset;
        copy.segmentPosition = this.segmentPosition;
        copy.segmentTag = this.segmentTag;
        copy.elementPosition = this.elementPosition;
        copy.elementOccurrence = this.elementOccurrence;
        copy.componentPosition = this.componentPosition;
        copy.repeated = this.repeated;
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

    public void incrementSegmentPosition(String segmentTag) {
        if (this.segmentPosition < 0) {
            this.segmentPosition = 1;
        } else {
            this.segmentPosition++;
        }

        this.segmentTag = segmentTag;

        clearSegmentLocations();
    }

    public void clearSegmentLocations() {
        this.elementPosition = -1;
        this.elementOccurrence = -1;
        clearComponentPosition();
    }

    public void incrementElementPosition() {
        if (this.elementPosition < 0) {
            this.elementPosition = 1;
        } else {
            this.elementPosition++;
        }

        this.elementOccurrence = 1;
        clearComponentPosition();
    }

    public void incrementElementOccurrence() {
        this.elementOccurrence++;
        clearComponentPosition();
    }

    public void incrementComponentPosition() {
        if (this.componentPosition < 0) {
            this.componentPosition = 1;
        } else {
            this.componentPosition++;
        }
    }

    public void clearComponentPosition() {
        this.componentPosition = -1;
    }

    public void setRepeated(boolean repeated) {
        this.repeated = repeated;
    }

    public boolean isRepeated() {
        return repeated;
    }
}
