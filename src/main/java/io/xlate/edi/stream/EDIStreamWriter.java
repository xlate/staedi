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
package io.xlate.edi.stream;

import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.Map;

import io.xlate.edi.schema.Schema;

public interface EDIStreamWriter extends AutoCloseable {

    /**
     * Get the value of a feature/property from the underlying implementation
     *
     * @param name
     *            - The name of the property, may not be null
     * @return The value of the property
     * @throws IllegalArgumentException
     *             if name is null
     */
    Object getProperty(String name);

    /**
     * Close this writer and free any resources associated with the writer. This
     * must not close the underlying output stream.
     *
     * @throws EDIStreamException
     *             if there are errors freeing associated resources
     */
    @Override
    void close() throws EDIStreamException;

    /**
     * Write any cached data to the underlying output mechanism.
     *
     * @throws EDIStreamException
     *             if there are errors flushing the cache
     */
    void flush() throws EDIStreamException;

    /**
     * Returns the control schema currently set on the reader. If none has been
     * set, then null will be returned.
     *
     * @return the control schema current set on this reader, may be null
     *
     * @since 1.8
     */
    Schema getControlSchema();

    /**
     * <p>
     * Sets the schema to be used for validation of the control structure for
     * this stream writer. This schema will be used to validate interchange,
     * group, and transaction/message envelopes.
     * <p>
     * Calls to this method are only valid before the interchange is started.
     *
     * @param controlSchema
     *            the schema instance to use for validation of control
     *            structures
     * @throws IllegalStateException
     *             when the writer is not in its initial state
     *
     * @since 1.1
     */
    void setControlSchema(Schema controlSchema);

    /**
     * <p>
     * Sets the schema to be used for validation of the business transaction for
     * this stream writer. This schema will be used to validate only the
     * contents of a transaction/message, <em>not including</em> the begin/end
     * control structures.
     * <p>
     * This method may be called at any time. However, when non-null, the writer
     * will make use of the transaction schema for output validation. It is the
     * responsibility of the caller to set the transaction schema to null at the
     * end of the business transaction.
     *
     * @param transactionSchema
     *            the schema instance to use for validation of business
     *            transaction structures
     *
     * @since 1.1
     */
    void setTransactionSchema(Schema transactionSchema);

    /**
     * Return the current location of the writer. If the Location is unknown the
     * processor should return an implementation of Location that returns -1 for
     * the location values. The location information is only valid until the
     * next item is written to the output.
     *
     * @return current location of the writer
     *
     * @since 1.1
     */
    Location getLocation();

    /**
     * Get the EDI standard name. Calls to this method are only valid when the
     * interchange type has been determined, after the full interchange header
     * segment has been written.
     *
     * @return the name of the EDI standard
     * @throws IllegalStateException
     *             when the standard has not yet been determined, prior to the
     *             start of an interchange header segment being fully written
     *
     * @since 1.7
     */
    String getStandard();

    /**
     * Retrieve a read-only map of delimiters in use for the stream being written.
     *
     * @return The value of the property
     * @throws IllegalStateException
     *             when the standard has not yet been determined, prior to the
     *             start of an interchange header segment being fully written
     *
     * @since 1.8
     */
    Map<String, Character> getDelimiters();

    EDIStreamWriter startInterchange() throws EDIStreamException;

    EDIStreamWriter endInterchange() throws EDIStreamException;

    EDIStreamWriter writeStartSegment(String name) throws EDIStreamException;

    EDIStreamWriter writeEndSegment() throws EDIStreamException;

    EDIStreamWriter writeStartElement() throws EDIStreamException;

    EDIStreamWriter writeStartElementBinary() throws EDIStreamException;

    EDIStreamWriter endElement() throws EDIStreamException;

    EDIStreamWriter writeRepeatElement() throws EDIStreamException;

    EDIStreamWriter startComponent() throws EDIStreamException;

    EDIStreamWriter endComponent() throws EDIStreamException;

    EDIStreamWriter writeEmptyElement() throws EDIStreamException;

    EDIStreamWriter writeElement(CharSequence text) throws EDIStreamException;

    EDIStreamWriter writeElement(char[] text, int start, int end) throws EDIStreamException;

    EDIStreamWriter writeEmptyComponent() throws EDIStreamException;

    EDIStreamWriter writeComponent(CharSequence text) throws EDIStreamException;

    EDIStreamWriter writeComponent(char[] text, int start, int end) throws EDIStreamException;

    EDIStreamWriter writeElementData(CharSequence text) throws EDIStreamException;

    EDIStreamWriter writeElementData(char[] text, int start, int end) throws EDIStreamException;

    EDIStreamWriter writeBinaryData(InputStream stream) throws EDIStreamException;

    EDIStreamWriter writeBinaryData(byte[] text, int start, int end) throws EDIStreamException;

    EDIStreamWriter writeBinaryData(ByteBuffer buffer) throws EDIStreamException;
}
