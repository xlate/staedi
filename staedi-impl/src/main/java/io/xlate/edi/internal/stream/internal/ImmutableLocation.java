/*******************************************************************************
 * Copyright 2017 xlate.io LLC, http://www.xlate.io
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License.  You may obtain a copy
 * of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations under
 * the License.
 ******************************************************************************/
package io.xlate.edi.internal.stream.internal;

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
