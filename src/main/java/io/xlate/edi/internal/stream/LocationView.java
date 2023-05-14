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

public class LocationView implements Location {

    protected int lineNumber;
    protected int columnNumber;
    protected int characterOffset;
    protected int segmentPosition;
    protected String segmentTag;
    protected int elementPosition;
    protected int componentPosition;
    protected int elementOccurrence;

    public LocationView(Location source) {
        lineNumber = source.getLineNumber();
        columnNumber = source.getColumnNumber();
        characterOffset = source.getCharacterOffset();
        segmentPosition = source.getSegmentPosition();
        segmentTag = source.getSegmentTag();
        elementPosition = source.getElementPosition();
        elementOccurrence = source.getElementOccurrence();
        componentPosition = source.getComponentPosition();
    }

    protected LocationView() {
        lineNumber = 1;
        columnNumber = 0;
        characterOffset = 0;
        segmentPosition = -1;
        elementPosition = -1;
        componentPosition = -1;
        elementOccurrence = -1;
    }

    public String toString() {
        return toString(false);
    }

    protected String toString(boolean afterSegment) {
        StringBuilder display = new StringBuilder();

        if (getSegmentPosition() < 0) {
            display.append("at offset ");
            display.append(getCharacterOffset());
        } else {
            display.append(afterSegment ? "after " : "in ");
            display.append("segment ");
            display.append(String.valueOf(getSegmentTag()));
            display.append(" at position ");
            display.append(String.valueOf(getSegmentPosition()));

            if (getElementPosition() > -1) {
                display.append(", element ");
                display.append(String.valueOf(getElementPosition()));

                if (getElementOccurrence() > 1) {
                    display.append(" (occurrence ");
                    display.append(String.valueOf(getElementOccurrence()));
                    display.append(')');
                }
            }

            if (getComponentPosition() > -1) {
                display.append(", component ");
                display.append(String.valueOf(getComponentPosition()));
            }
        }

        return display.toString();
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
    public String getSegmentTag() {
        return segmentTag;
    }

    @Override
    public int getElementPosition() {
        return elementPosition;
    }

    @Override
    public int getComponentPosition() {
        return componentPosition;
    }

    @Override
    public int getElementOccurrence() {
        return elementOccurrence;
    }

    @Override
    public Location copy() {
        return new LocationView(this);
    }
}
