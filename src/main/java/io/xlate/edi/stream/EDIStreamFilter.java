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

/**
 * This interface declares a simple filter interface that one can create to
 * filter EDIStreamReaders
 * 
 * @version 1.0
 */
public interface EDIStreamFilter {

	/**
	 * Tests whether the current state is part of this stream. This method will
	 * return true if this filter accepts this event and false otherwise.
	 *
	 * The method should not change the state of the reader when accepting a
	 * state.
	 *
	 * @param reader
	 *            the event to test
	 * @return true if this filter accepts this event, false otherwise
	 */
	boolean accept(EDIStreamReader reader);

}
