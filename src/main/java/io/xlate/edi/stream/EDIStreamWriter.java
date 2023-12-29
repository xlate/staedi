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

/**
 * The EDIStreamWriter interface specifies how to write EDI. Each method depends
 * on the internal state of the writer and a client application must ensure that
 * the methods are called in the proper sequence. For example, element data may
 * not be written prior to starting an interchange and a segment.
 */
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
     * <p>
     * A built-in control schema may be obtained from
     * {@link io.xlate.edi.schema.SchemaFactory#getControlSchema(String, String[])
     * SchemaFactory#getControlSchema} to pass to this method.
     *
     * @param controlSchema
     *            the schema instance to use for validation of control
     *            structures
     * @throws IllegalStateException
     *             when the writer is not in its initial state
     * @see io.xlate.edi.schema.SchemaFactory#getControlSchema(String, String[])
     *      SchemaFactory#getControlSchema
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
     * Retrieve a read-only map of delimiters in use for the stream being
     * written.
     *
     * @return The value of the property
     * @throws IllegalStateException
     *             when the standard has not yet been determined, prior to the
     *             start of an interchange header segment being fully written
     *
     * @since 1.8
     */
    Map<String, Character> getDelimiters();

    /**
     * Initialize this writer to begin writing an interchange.
     *
     * This method does not write any output to the underlying stream.
     *
     * @return this EDI stream writer
     * @throws EDIStreamException
     *             is not thrown and will be removed in the next major version
     *             of StAEDI.
     * @throws IllegalStateException
     *             when the writer is in any state other than the initial state
     */
    EDIStreamWriter startInterchange() throws EDIStreamException;

    /**
     * Completes an interchange and returns the writer to its initial state. Any
     * data pending output will be {@linkplain #flush() flushed}.
     *
     * @return this EDI stream writer
     * @throws EDIStreamException
     *             if an error occurs
     * @throws IllegalStateException
     *             when the writer is in a state writing a segment, element,
     *             composite
     */
    EDIStreamWriter endInterchange() throws EDIStreamException;

    /**
     * Begin a new segment with the given name and write the tag to the
     * underlying output.
     *
     * @param name
     *            name of the segment (i.e. the segment tag)
     * @return this EDI stream writer
     * @throws EDIStreamException
     *             if an error occurs
     * @throws IllegalStateException
     *             when the writer is not in a state to begin a segment
     */
    EDIStreamWriter writeStartSegment(String name) throws EDIStreamException;

    /**
     * Complete a segment by writing the segment terminator to the underlying
     * output.
     *
     * @return this EDI stream writer
     * @throws EDIStreamException
     *             if an error occurs
     * @throws IllegalStateException
     *             when the writer is not in a state to end a segment
     */
    EDIStreamWriter writeEndSegment() throws EDIStreamException;

    /**
     * Start a new element, composite or simple.
     *
     * @return this EDI stream writer
     * @throws EDIStreamException
     *             if an error occurs
     * @throws IllegalStateException
     *             when a segment has not been started with
     *             {@link #writeStartSegment(String)}
     */
    EDIStreamWriter writeStartElement() throws EDIStreamException;

    /**
     * Start a new element for binary data.
     *
     * @return this EDI stream writer
     * @throws EDIStreamException
     *             if an error occurs
     * @throws IllegalStateException
     *             when the a segment has not been started with
     *             {@link #writeStartSegment(String)}
     */
    EDIStreamWriter writeStartElementBinary() throws EDIStreamException;

    /**
     * Complete an element. A delimiter will not be written immediately.
     *
     * @return this EDI stream writer
     * @throws EDIStreamException
     *             if an error occurs
     * @throws IllegalStateException
     *             when the writer is not in the state of writing an element
     */
    EDIStreamWriter endElement() throws EDIStreamException;

    /**
     * Write an element repeat delimiter/separator to the output stream.
     * Following this method being called, the writer will be in a state to
     * accept element data using {@link #writeElementData(CharSequence)} or
     * {@link #writeElementData(char[], int, int)}.
     *
     * @return this EDI stream writer
     * @throws EDIStreamException
     *             if an error occurs
     * @throws IllegalStateException
     *             when the writer is not in a state for writing element data. A
     *             segment must have already been started.
     */
    EDIStreamWriter writeRepeatElement() throws EDIStreamException;

    /**
     * Start a component of a composite element.
     *
     * @return this EDI stream writer
     * @throws EDIStreamException
     *             if an error occurs
     * @throws IllegalStateException
     *             when an element has not been started with
     *             {@link #writeStartElement()}
     */
    EDIStreamWriter startComponent() throws EDIStreamException;

    /**
     * Complete a component of a composite element.
     *
     * @return this EDI stream writer
     * @throws EDIStreamException
     *             if an error occurs
     * @throws IllegalStateException
     *             when the writer is not in the state of writing an component
     *             element
     */
    EDIStreamWriter endComponent() throws EDIStreamException;

    /**
     * Write an empty simple element.
     * <p>
     * Shorthand for calling {@link #writeStartElement()} immediately followed
     * by {@link #endElement()}.
     *
     * @return this EDI stream writer
     * @throws EDIStreamException
     *             if an error occurs
     * @throws IllegalStateException
     *             when the writer is not in a state for writing simple element
     *             data
     */
    EDIStreamWriter writeEmptyElement() throws EDIStreamException;

    /**
     * Begin an element, write text data from the given CharSequence to the
     * output, and end the element.
     * <p>
     * Shorthand for calling {@link #writeStartElement()},
     * {@link #writeElementData(CharSequence)}, and {@link #endElement()}, in
     * that order.
     *
     * @param text
     *            CharSequence containing element's full text data
     * @return this EDI stream writer
     * @throws EDIStreamException
     *             if an error occurs
     * @throws IllegalStateException
     *             when the writer is not in a state for writing simple element
     *             data
     */
    EDIStreamWriter writeElement(CharSequence text) throws EDIStreamException;

    /**
     * Begin an element, write text data from the given char array to the
     * output, and end the element. Data will be read from the offset given by
     * start (inclusive) to the offset given by end (exclusive).
     * <p>
     * Shorthand for calling {@link #writeStartElement()},
     * {@link #writeElementData(char[], int, int)}, and {@link #endElement()},
     * in that order.
     *
     * @param text
     *            char array containing element's full text data
     * @param start
     *            the start index, inclusive
     * @param end
     *            the end index, exclusive
     * @return this EDI stream writer
     * @throws EDIStreamException
     *             if an error occurs
     * @throws IllegalStateException
     *             when the writer is not in a state for writing simple element
     *             data
     */
    EDIStreamWriter writeElement(char[] text, int start, int end) throws EDIStreamException;

    /**
     * Write an empty component
     * <p>
     * Shorthand for calling {@link #startComponent()} immediately followed by
     * {@link #endComponent()}.
     *
     * @return this EDI stream writer
     * @throws EDIStreamException
     *             if an error occurs
     * @throws IllegalStateException
     *             when the writer is not in a state for writing component
     *             element data
     */
    EDIStreamWriter writeEmptyComponent() throws EDIStreamException;

    /**
     * Begin a component element, write text data from the given CharSequence to
     * the output, and end the element.
     * <p>
     * Shorthand for calling {@link #startComponent()},
     * {@link #writeElementData(CharSequence)}, and {@link #endComponent()}, in
     * that order.
     *
     * @param text
     *            CharSequence containing component's full text data
     * @return this EDI stream writer
     * @throws EDIStreamException
     *             if an error occurs
     * @throws IllegalStateException
     *             when the writer is not in a state for writing component
     *             element data
     */
    EDIStreamWriter writeComponent(CharSequence text) throws EDIStreamException;

    /**
     * Begin a component element, write text data from the given char array to
     * the output, and end the element. Data will be read from the offset given
     * by start (inclusive) to the offset given by end (exclusive).
     * <p>
     * Shorthand for calling {@link #startComponent()},
     * {@link #writeElementData(char[], int, int)}, and {@link #endComponent()},
     * in that order.
     *
     * @param text
     *            char array containing component's full text data
     * @param start
     *            the start index, inclusive
     * @param end
     *            the end index, exclusive
     * @return this EDI stream writer
     * @throws EDIStreamException
     *             if an error occurs
     * @throws IllegalStateException
     *             when the writer is not in a state for writing component
     *             element data
     */
    EDIStreamWriter writeComponent(char[] text, int start, int end) throws EDIStreamException;

    /**
     * Write text data from the given CharSequence to the output.
     *
     * @param text
     *            CharSequence containing element text data
     * @return this EDI stream writer
     * @throws EDIStreamException
     *             if an error occurs
     * @throws IllegalStateException
     *             when the writer is not in a state for writing element data.
     *             See {@linkplain #writeStartElement()}
     */
    EDIStreamWriter writeElementData(CharSequence text) throws EDIStreamException;

    /**
     * Write text data from the given char array to the output. Data will be
     * read from the offset given by start (inclusive) to the offset given by
     * end (exclusive).
     *
     * @param text
     *            char array containing element text data
     * @param start
     *            the start index, inclusive
     * @param end
     *            the end index, exclusive
     * @return this EDI stream writer
     * @throws EDIStreamException
     *             if an error occurs
     * @throws IllegalStateException
     *             when the writer is not in a state for writing element data.
     *             See {@linkplain #writeStartElement()}
     */
    EDIStreamWriter writeElementData(char[] text, int start, int end) throws EDIStreamException;

    /**
     * Write binary data from the given InputStream to the output. The stream
     * will be read fully, until the byte returned by
     * {@linkplain InputStream#read()} is {@code -1}. Any data pending output
     * will first be {@linkplain #flush() flushed}.
     *
     * @param stream
     *            InputStream containing binary data to be consumed by the
     *            reader and written to the underlying output
     * @return this EDI stream writer
     * @throws EDIStreamException
     *             if an error occurs
     * @throws IllegalStateException
     *             when the writer is not in a state for writing binary element
     *             data. See {@linkplain #writeStartElementBinary()}
     */
    EDIStreamWriter writeBinaryData(InputStream stream) throws EDIStreamException;

    /**
     * Write binary data from the given byte array to the output. Data will be
     * read from the offset given by start (inclusive) to the offset given by
     * end (exclusive). Any data pending output will first be
     * {@linkplain #flush() flushed}.
     *
     * @param binary
     *            byte array containing binary data
     * @param start
     *            the start index, inclusive
     * @param end
     *            the end index, exclusive
     * @return this EDI stream writer
     * @throws EDIStreamException
     *             if an error occurs
     * @throws IllegalStateException
     *             when the writer is not in a state for writing binary element
     *             data. See {@linkplain #writeStartElementBinary()}
     */
    EDIStreamWriter writeBinaryData(byte[] binary, int start, int end) throws EDIStreamException;

    /**
     * Write binary data from the given buffer to the output. Any data pending
     * output will first be {@linkplain #flush() flushed}.
     *
     * @param buffer
     *            data buffer containing binary data
     * @return this EDI stream writer
     * @throws EDIStreamException
     *             if an error occurs
     * @throws IllegalStateException
     *             when the writer is not in a state for writing binary element
     *             data. See {@linkplain #writeStartElementBinary()}
     */
    EDIStreamWriter writeBinaryData(ByteBuffer buffer) throws EDIStreamException;
}
