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
package io.xlate.edi.stream.internal;

import io.xlate.edi.stream.EDIStreamConstants.Standards;

public class EDIFACTDialect implements Dialect {

	private static final String UNB = "UNB";

	private static final int EDIFACT_UNA_LENGTH = 9;

	private char componentDelimiter = ':';
	private char elementDelimiter = '+';
	private char decimalMark = '.';
	private char releaseIndicator = '?';
	private char elementRepeater = '*';
	private char segmentDelimiter = '\'';

	private String headerTag;
	private String version;
	StringBuilder header;
	private int index = -1;
	private int unbStart = -1;
	private boolean initialized;
	private boolean rejected;

	@Override
	public void setHeaderTag(String tag) {
		headerTag = tag;
	}

	@Override
	public String getHeaderTag() {
		return headerTag;
	}

	boolean initialize(CharacterSet characters) {
		final int length = header.length();

		if (UNB.equals(headerTag)) {
			if (length < 10) {
				return false;
			}

			version = String.valueOf(header.charAt(9));
		} else {
			if (length < 18) {
				return false;
			}

			for (int i = 11; i < length; i++) {
				if (header.charAt(i - 2) != 'U') {
					continue;
				}
				if (header.charAt(i - 1) != 'N') {
					continue;
				}
				if (header.charAt(i) != 'B') {
					continue;
				}
				if (length < i + 7) {
					return false;
				}
				version = String.valueOf(header.charAt(i + 7));
				break;
			}
		}

		//FIXME: need release
		version = String.format("%s0000", version);

		characters.setClass(componentDelimiter, CharacterClass.COMPONENT_DELIMITER);
		characters.setClass(elementDelimiter, CharacterClass.ELEMENT_DELIMITER);
        if (releaseIndicator != ' ') {
            characters.setClass(releaseIndicator, CharacterClass.RELEASE_CHARACTER);
        }
		if (elementRepeater != ' ') {
	        characters.setClass(elementRepeater, CharacterClass.ELEMENT_REPEATER);
		}
		characters.setClass(segmentDelimiter, CharacterClass.SEGMENT_DELIMITER);

		return (initialized = true);
	}

	@Override
	public boolean isConfirmed() {
		return initialized;
	}

	@Override
	public boolean isRejected() {
		return rejected;
	}

	@Override
	public String getStandard() {
		return Standards.EDIFACT;
	}

	@Override
	public String getVersion() {
		return version;
	}

	@Override
	public boolean appendHeader(CharacterSet characters, char value) {
		if (++index == 0) {
			header = new StringBuilder();
		}

		header.append(value);

		if (UNB.equals(headerTag)) {
			if (index == 0) {
				characters.setClass(componentDelimiter, CharacterClass.COMPONENT_DELIMITER);
				characters.setClass(releaseIndicator, CharacterClass.RELEASE_CHARACTER);
				characters.setClass(elementRepeater, CharacterClass.ELEMENT_REPEATER);
			} else if (index == 3) {
				characters.setClass(elementDelimiter, CharacterClass.ELEMENT_DELIMITER);
			} else if (segmentDelimiter == value) {
				characters.setClass(segmentDelimiter, CharacterClass.SEGMENT_DELIMITER);
				rejected = !initialize(characters);
				return isConfirmed();
			}
		} else {
			switch (index) {
			case 3:
				componentDelimiter = value;
				characters.setClass(componentDelimiter, CharacterClass.COMPONENT_DELIMITER);
				break;
			case 4:
				elementDelimiter = value;
				characters.setClass(elementDelimiter, CharacterClass.ELEMENT_DELIMITER);
				break;
			case 5:
				decimalMark = value;
				break;
			case 6:
				releaseIndicator = value;
				if (value != ' ') {
					characters.setClass(releaseIndicator, CharacterClass.RELEASE_CHARACTER);
				}
				break;
			case 7:
				elementRepeater = value;
				if (value != ' ') {
					characters.setClass(elementRepeater, CharacterClass.ELEMENT_REPEATER);
				}
				break;
			case 8:
				segmentDelimiter = value;
				characters.setClass(segmentDelimiter, CharacterClass.SEGMENT_DELIMITER);
				break;
			}

			if (index > EDIFACT_UNA_LENGTH) {
				if (unbStart > -1 && (index - unbStart) > 9) {
					rejected = !initialize(characters);
					return isConfirmed();
				} else if (header.charAt(index) == 'B') {
					CharSequence un = header.subSequence(index - 2, index);

					if ("UN".contentEquals(un)) {
						unbStart = index - 2;
					} else {
						// Some other segment / element?
						return false;
					}
				} else if (unbStart < 0 && value == elementDelimiter) {
					// Some other segment / element?
					return false;
				}
			}
		}

		return true;
	}

	@Override
	public char getComponentElementSeparator() {
		return componentDelimiter;
	}

	@Override
	public char getDataElementSeparator() {
		return elementDelimiter;
	}

	@Override
	public char getDecimalMark() {
		return decimalMark;
	}

	@Override
	public char getReleaseIndicator() {
		return releaseIndicator;
	}

	@Override
	public char getRepetitionSeparator() {
		return elementRepeater;
	}

	@Override
	public char getSegmentTerminator() {
		return segmentDelimiter;
	}
}
