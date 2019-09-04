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
package io.xlate.edi.stream.internal;

import io.xlate.edi.stream.Location;

public class InternalLocation implements Location {

    private int lineNumber = -1;
    private int columnNumber = -1;
    private int characterOffset = -1;
    private int segmentPosition = -1;
    private int elementPosition = -1;
    private int elementOccurrence = -1;
    private int componentPosition = -1;
    private boolean repeated = false;

    public InternalLocation copy() {
        InternalLocation copy = new InternalLocation();
        copy.lineNumber = this.lineNumber;
        copy.columnNumber = this.columnNumber;
        copy.characterOffset = this.characterOffset;
        copy.segmentPosition = this.segmentPosition;
        copy.elementPosition = this.elementPosition;
        copy.elementOccurrence = this.elementOccurrence;
        copy.componentPosition = this.componentPosition;
        copy.repeated = this.repeated;
        return copy;
    }

    @Override
    public int getLineNumber() {
        return lineNumber;
    }

    @Override
    public int getColumnNumber() {
        return columnNumber;
    }

    @Override
    public int getCharacterOffset() {
        return characterOffset;
    }

    @Override
    public int getSegmentPosition() {
        return segmentPosition;
    }

    @Override
    public int getElementPosition() {
        return elementPosition;
    }

    public void setElementPosition(int elementPosition) {
        this.elementPosition = elementPosition;
    }

    @Override
    public int getElementOccurrence() {
        return elementOccurrence;
    }

    public void setElementOccurrence(int elementOccurrence) {
        this.elementOccurrence = elementOccurrence;
    }

    @Override
    public int getComponentPosition() {
        return componentPosition;
    }

    public void setComponentPosition(int componentPosition) {
        this.componentPosition = componentPosition;
    }

    public void incrementOffset() {
        this.characterOffset++;
    }

    public void incrementSegmentPosition() {
        if (this.segmentPosition < 0) {
            this.segmentPosition = 1;
        } else {
            this.segmentPosition++;
        }

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
