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

import java.io.InputStream;
import java.nio.ByteBuffer;

public interface EDIStreamWriter {

	/**
	 * Get the value of a feature/property from the underlying implementation
	 * 
	 * @param name
	 *            - The name of the property, may not be null
	 * @return The value of the property
	 * @throws IllegalArgumentException
	 *             if name is null
	 */
	Object getProperty(String name) throws IllegalArgumentException;

	/**
	 * Close this writer and free any resources associated with the writer. This
	 * must not close the underlying output stream.
	 * 
	 * @throws EDIStreamException
	 *             if there are errors freeing associated resources
	 */
	void close() throws EDIStreamException;

	/**
	 * Write any cached data to the underlying output mechanism.
	 * 
	 * @throws EDIStreamException
	 *             if there are errors flushing the cache
	 */
	void flush() throws EDIStreamException;

	EDIStreamWriter startInterchange() throws IllegalStateException, EDIStreamException;
	EDIStreamWriter endInterchange() throws IllegalStateException, EDIStreamException;

	EDIStreamWriter writeStartSegment(String name) throws IllegalStateException, EDIStreamException;
	EDIStreamWriter writeEndSegment() throws IllegalStateException, EDIStreamException;

	EDIStreamWriter writeStartElement() throws IllegalStateException, EDIStreamException;
	EDIStreamWriter writeStartElementBinary() throws IllegalStateException, EDIStreamException;
	EDIStreamWriter endElement() throws IllegalStateException, EDIStreamException;

	EDIStreamWriter writeRepeatElement() throws IllegalStateException, EDIStreamException;

	EDIStreamWriter startComponent() throws IllegalStateException, EDIStreamException;
	EDIStreamWriter endComponent() throws IllegalStateException, EDIStreamException;

	EDIStreamWriter writeEmptyElement() throws IllegalStateException, EDIStreamException;
	EDIStreamWriter writeElement(CharSequence text) throws IllegalStateException, EDIStreamException;
	EDIStreamWriter writeElement(char[] text, int start, int end) throws IllegalStateException, EDIStreamException;

	EDIStreamWriter writeEmptyComponent() throws IllegalStateException, EDIStreamException;
	EDIStreamWriter writeComponent(CharSequence text) throws IllegalStateException, EDIStreamException;
	EDIStreamWriter writeComponent(char[] text, int start, int end) throws IllegalStateException, EDIStreamException;

	EDIStreamWriter writeElementData(CharSequence text) throws IllegalStateException, EDIStreamException;
	EDIStreamWriter writeElementData(char[] text, int start, int end) throws IllegalStateException, EDIStreamException;

	EDIStreamWriter writeBinaryData(InputStream stream) throws IllegalStateException, EDIStreamException;
	EDIStreamWriter writeBinaryData(byte[] text, int start, int end) throws IllegalStateException, EDIStreamException;
	EDIStreamWriter writeBinaryData(ByteBuffer buffer) throws IllegalStateException, EDIStreamException;
}
