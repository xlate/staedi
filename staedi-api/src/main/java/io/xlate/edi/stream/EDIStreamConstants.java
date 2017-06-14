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
package io.xlate.edi.stream;

public interface EDIStreamConstants {

	public static interface Standards {
		public static final String X12 = "X12";
		public static final String EDIFACT = "EDIFACT";
	}

	public static interface Delimiters {
		public static final String SEGMENT = "io.xlate.edi.stream.delim.segment";
		public static final String DATA_ELEMENT = "io.xlate.edi.stream.delim.dataElement";
		public static final String COMPONENT_ELEMENT = "io.xlate.edi.stream.delim.componentElement";
		public static final String REPETITION = "io.xlate.edi.stream.delim.repetition";
		public static final String RELEASE = "io.xlate.edi.stream.delim.release";
	}

	public static interface Events {
		public static final int ELEMENT_DATA = 1;
		public static final int ELEMENT_DATA_BINARY = 2;

		public static final int START_COMPOSITE = 3;
		public static final int END_COMPOSITE = 4;

		public static final int START_SEGMENT = 5;
		public static final int END_SEGMENT = 6;

		public static final int START_INTERCHANGE = 7;
		public static final int END_INTERCHANGE = 8;

		public static final int START_LOOP = 9;
		public static final int END_LOOP = 10;

		public static final int SEGMENT_ERROR = 11;
		public static final int ELEMENT_DATA_ERROR = 12;
		public static final int ELEMENT_OCCURRENCE_ERROR = 13;
	}

	public static interface SegmentErrors {
		public static final int UNRECOGNIZED_SEGMENT_ID = 100;
		public static final int UNEXPECTED_SEGMENT = 101;
		public static final int MANDATORY_SEGMENT_MISSING = 102;
		public static final int LOOP_OCCURS_OVER_MAXIMUM_TIMES = 103;
		public static final int SEGMENT_EXCEEDS_MAXIMUM_USE = 104;
		public static final int SEGMENT_NOT_IN_DEFINED_TRANSACTION_SET = 105;
		public static final int SEGMENT_NOT_IN_PROPER_SEQUENCE = 106;
		public static final int SEGMENT_HAS_DATA_ELEMENT_ERRORS = 107;
	}

	public static interface ElementOccurrenceErrors {
		public static final int REQUIRED_DATA_ELEMENT_MISSING = 150;
		public static final int CONDITIONAL_REQUIRED_DATA_ELEMENT_MISSING = 151;
		public static final int TOO_MANY_DATA_ELEMENTS = 152;
		public static final int EXCLUSION_CONDITION_VIOLATED = 153;
		public static final int TOO_MANY_REPETITIONS = 154;
		public static final int TOO_MANY_COMPONENTS = 155;
	}

	public static interface ElementDataErrors {
		public static final int DATA_ELEMENT_TOO_SHORT = 180;
		public static final int DATA_ELEMENT_TOO_LONG = 181;
		public static final int INVALID_CHARACTER_DATA = 182;
		public static final int INVALID_CODE_VALUE = 183;
		public static final int INVALID_DATE = 184;
		public static final int INVALID_TIME = 185;
	}
}
