package io.xlate.edi.stream.internal;

import io.xlate.edi.stream.Location;

public class InternalLocation implements Cloneable, Location {

	private int lineNumber = -1;
	private int columnNumber = -1;
	private int characterOffset = -1;
	private int segmentPosition = -1;
	private int elementPosition = -1;
	private int elementOccurrence = -1;
	private int componentPosition = -1;
	private boolean repeated = false;

	@Override
	public InternalLocation clone() {
		try {
			return (InternalLocation) super.clone();
		} catch (CloneNotSupportedException e) {
			throw new RuntimeException(e);
		}
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

