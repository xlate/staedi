package io.xlate.edi.stream.internal;

import io.xlate.edi.stream.Location;

class ImmutableLocation implements Location {

	private final int lineNumber;
	private final int columnNumber;
	private final int characterOffset;
	private final int segmentPosition;
	private final int elementPosition;
	private final int componentPosition;
	private final int elementRepetition;

	public ImmutableLocation(Location source) {
		lineNumber = source.getLineNumber();
		columnNumber = source.getColumnNumber();
		characterOffset = source.getCharacterOffset();
		segmentPosition = source.getSegmentPosition();
		elementPosition = source.getElementPosition();
		componentPosition = source.getComponentPosition();
		elementRepetition = source.getElementOccurrence();
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

	@Override
	public int getComponentPosition() {
		return componentPosition;
	}

	@Override
	public int getElementOccurrence() {
		return elementRepetition;
	}
}
