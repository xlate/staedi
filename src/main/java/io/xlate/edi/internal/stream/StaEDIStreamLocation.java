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

import io.xlate.edi.schema.EDIType;
import io.xlate.edi.stream.Location;

public class StaEDIStreamLocation extends LocationView implements Location {

    private boolean afterSegment = false;
    private boolean composite = false;
    private boolean repeating = false;
    private int repeatCount = -1;

    /*
     * Control counts and header references of the open control structures,
     * indexed by the ordinal of EDIType.Type.INTERCHANGE (0), GROUP (1) and
     * TRANSACTION (2). The counts are copied by value. The references array is
     * never changed after it is set, so locations share it.
     */
    public static final int CONTROL_SCOPES = 3;
    static final String[] NO_REFERENCES = new String[CONTROL_SCOPES];

    private int interchangeCount = -1;
    private int groupCount = -1;
    private int transactionCount = -1;
    private String[] controlReferences = NO_REFERENCES;

    public StaEDIStreamLocation() {
        super();
    }

    public StaEDIStreamLocation(Location source) {
        super(source);
        if (source instanceof StaEDIStreamLocation) {
            copyControls((StaEDIStreamLocation) source);
        }
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

    public void set(StaEDIStreamLocation source) {
        lineNumber = source.getLineNumber();
        columnNumber = source.getColumnNumber();
        characterOffset = source.getCharacterOffset();
        segmentPosition = source.getSegmentPosition();
        segmentTag = source.getSegmentTag();
        elementPosition = source.getElementPosition();
        componentPosition = source.getComponentPosition();
        elementOccurrence = source.getElementOccurrence();
        afterSegment = source.afterSegment;
        copyControls(source);
    }

    private void copyControls(StaEDIStreamLocation source) {
        interchangeCount = source.interchangeCount;
        groupCount = source.groupCount;
        transactionCount = source.transactionCount;
        controlReferences = source.controlReferences;
    }

    static int controlIndex(EDIType.Type scope) {
        if (scope != null) {
            switch (scope) {
            case INTERCHANGE:
            case GROUP:
            case TRANSACTION:
                return scope.ordinal();
            default:
                break;
            }
        }
        throw new IllegalArgumentException("Control scope must be INTERCHANGE, GROUP, or TRANSACTION: " + scope);
    }

    /**
     * Index of the innermost open control structure, or -1 when none is open.
     */
    private int innermostControl() {
        for (int i = CONTROL_SCOPES - 1; i >= 0; i--) {
            if (controlCount(i) > -1 || controlReferences[i] != null) {
                return i;
            }
        }
        return -1;
    }

    private int controlCount(int index) {
        switch (index) {
        case 0:
            return interchangeCount;
        case 1:
            return groupCount;
        default:
            return transactionCount;
        }
    }

    /**
     * Set the control values. The references array must not be changed after
     * this call.
     */
    public void setControls(int[] counts, String[] references) {
        interchangeCount = counts[0];
        groupCount = counts[1];
        transactionCount = counts[2];
        controlReferences = references;
    }

    public void setControlCount(int index, int count) {
        switch (index) {
        case 0:
            interchangeCount = count;
            break;
        case 1:
            groupCount = count;
            break;
        default:
            transactionCount = count;
            break;
        }
    }

    @Override
    public int getControlCount(EDIType.Type scope) {
        return controlCount(controlIndex(scope));
    }

    @Override
    public int getControlCount() {
        int i = innermostControl();
        return i < 0 ? -1 : controlCount(i);
    }

    @Override
    public String getControlReference(EDIType.Type scope) {
        return controlReferences[controlIndex(scope)];
    }

    @Override
    public String getControlReference() {
        int i = innermostControl();
        return i < 0 ? null : controlReferences[i];
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
