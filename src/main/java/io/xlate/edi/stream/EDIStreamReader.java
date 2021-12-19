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

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.NoSuchElementException;

import io.xlate.edi.schema.EDIReference;
import io.xlate.edi.schema.EDIType;
import io.xlate.edi.schema.Schema;
import io.xlate.edi.schema.implementation.EDITypeImplementation;

public interface EDIStreamReader extends Closeable, EDIStreamConstants {

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
     * Retrieve a read-only map of delimiters in use for the stream being read.
     *
     * @return The value of the property
     * @throws IllegalStateException
     *             if called outside of an interchange
     */
    Map<String, Character> getDelimiters();

    /**
     * Get next parsing event
     *
     * @return the integer code corresponding to the current parse event
     * @throws NoSuchElementException
     *             if this is called when hasNext() returns false
     * @throws EDIStreamException
     *             if there is an error processing the underlying EDI source
     */
    EDIStreamEvent next() throws EDIStreamException;

    /**
     * Skips any ELEMENT_DATA, START_COMPOSITE, and END_COMPOSITE until a
     * START_SEGMENT is reached.
     *
     * @return the event type of the element read - START_SEGMENT
     * @throws NoSuchElementException
     *             if this is called when hasNext() returns false or there are
     *             no additional START_SEGMENT events in the stream
     * @throws EDIStreamException
     *             if the current event is not following START_INTERCHANGE and
     *             preceding END_INTERCHANGE
     */
    EDIStreamEvent nextTag() throws EDIStreamException;

    /**
     * Returns true if there are more parsing events and false if there are no
     * more events. This method will return false if the current state of the
     * EDIStreamReader is END_INTERCHANGE
     *
     * @return true if there are more events, false otherwise
     * @throws EDIStreamException
     *             if there is a fatal error detecting the next state
     */
    boolean hasNext() throws EDIStreamException;

    /**
     * Frees any resources associated with this Reader. This method does not
     * close the underlying input stream.
     *
     * @throws IOException
     *             if there are errors freeing associated resources
     */
    @Override
    void close() throws IOException;

    /**
     * Returns an integer code that indicates the type of the event the cursor
     * is pointing to.
     *
     * @return code that indicates the type of the event the cursor is pointing
     *         to
     */
    EDIStreamEvent getEventType();

    /**
     * Get the EDI standard name. Calls to this method are only valid when the
     * interchange type has been determined, after START_INTERCHANGE.
     *
     * @return the name of the EDI standard
     * @throws IllegalStateException
     *             when the standard has not yet been determined, prior to the
     *             start of an interchange
     */
    String getStandard();

    /**
     * Get the interchange version declared on the interchange begin segment.
     * Calls to this method are only valid when interchange type has been
     * determined, after START_INTERCHANGE.
     *
     * @return the version
     * @throws IllegalStateException
     *             when the version has not yet been determined, prior to the
     *             start of an interchange
     */
    String[] getVersion();

    /**
     * Get the transaction version declared on the transaction header segment or
     * the functional group header segment (X12 only). Calls to this method are
     * only valid when interchange type has been determined, after
     * START_INTERCHANGE, and the transaction version has been determined by
     * reading the dialect-specific data elements containing the version
     * value(s).
     *
     * The elements of the returned array are defined as:
     * <ol>
     * <li>Agency code
     * <li>Version
     * <li>Release
     * <li>Industry code
     * </ol>
     *
     * In practice, the values will be the following elements:
     * <p>
     * For EDIFACT:
     * <ol>
     * <li>Agency code: UNH02-4
     * <li>Version: UNH02-2
     * <li>Release: UNH02-3
     * <li>Industry code: UNH02-5
     * </ol>
     * <p>
     * For X12:
     * <ol>
     * <li>Agency code: GS07
     * <li>Version/Release/Industry code: GS08 (or ST03, when used)
     * </ol>
     *
     * @return the transaction version
     * @throws IllegalStateException
     *             when the version has not yet been determined, prior to the
     *             start of a transaction (or functional group when in use)
     *
     * @since 1.9
     */
    String[] getTransactionVersion();

    /**
     * The transaction version string elements as a single, period-delimited
     * value. This value may be used to obtain version-specific schema
     * information available from the {@link EDIReference} returned from
     * {@link #getSchemaTypeReference()}.
     *
     * @return the transaction version as a single, period-delimited value
     * @throws IllegalStateException
     *             when the version has not yet been determined, prior to the
     *             start of a transaction (or functional group when in use)
     *
     * @since 1.9
     */
    String getTransactionVersionString();

    /**
     * The transaction type string as received in a standard-specific location.
     * For example, the ST01 or UNH02-1 elements.
     *
     * @return the transaction type
     * @throws IllegalStateException
     *             when the type has not yet been determined, prior to the
     *             parsing of the transaction begin segment
     *
     * @since 1.16
     */
    String getTransactionType();

    /**
     * Returns the control schema currently set on the reader. If none has been
     * set, then null will be returned.
     *
     * @return the control schema current set on this reader, may be null
     *
     * @since 1.5
     */
    Schema getControlSchema();

    /**
     * <p>
     * Sets the schema to be used for validation of the control structure for
     * this stream reader. This schema will be used to validate interchange,
     * group, and transaction/message envelopes.
     * <p>
     * Calls to this method are only valid when the current event type is
     * START_INTERCHANGE.
     *
     * @param schema
     *            the schema instance to use for validation of control
     *            structures
     * @throws IllegalStateException
     *             when the current event type is not START_INTERCHANGE
     */
    void setControlSchema(Schema schema);

    /**
     * Returns the schema currently set on the reader to be used for validation
     * of the business transaction. If none has been set, then null will be
     * returned.
     *
     * @return the transaction schema current set on this reader, may be null
     *
     * @since 1.5
     */
    Schema getTransactionSchema();

    /**
     * <p>
     * Sets the schema to be used for validation of the business transaction for
     * this stream reader. This schema will be used to validate only the
     * contents of a transaction/message, <em>not including</em> the begin/end
     * control structures.
     * <p>
     * Calls to this method are only valid after a START_TRANSACTION event and
     * up to and including the END_SEGMENT event representing the beginning of
     * the transaction.
     *
     * @param schema
     *            the schema instance to use for validation of business
     *            transaction structures
     * @throws IllegalStateException
     *             when the reader is not positioned on the start transaction
     *             segment
     */
    void setTransactionSchema(Schema schema);

    /**
     * Return the reference code for the current element if a schema has been
     * set and the current processing state is within an interchange. Otherwise,
     * an IllegalStateException will be thrown.
     *
     * If the reader encounters an unknown type, the reference code will be
     * null.
     *
     * @return the reference code from the schema for the current EDIType
     * @throws IllegalStateException
     *             when the current event type is not within an interchange
     */
    String getReferenceCode();

    /**
     * Returns an integer code that indicates the type of error the cursor is
     * pointing to. Calls to this method are only valid when the current event
     * type is SEGMENT_ERROR or ELEMENT_ERROR.
     *
     * @return code that indicates the type of the error the cursor is pointing
     *         to
     * @throws IllegalStateException
     *             when the current event type is not SEGMENT_ERROR or
     *             ELEMENT_ERROR
     */
    EDIStreamValidationError getErrorType();

    /**
     * Returns the current value of the parse event as a string. This returns
     *
     * <ul>
     * <li>the string value of an element for an
     * {@link EDIStreamEvent#ELEMENT_DATA ELEMENT_DATA} event
     * <li>the string value of a segment tag for a
     * {@link EDIStreamEvent#START_SEGMENT START_SEGMENT} event
     * <li>the string value of the current segment tag for an
     * {@link EDIStreamEvent#END_SEGMENT END_SEGMENT} event
     * <li>the invalid element text from an
     * {@link EDIStreamEvent#ELEMENT_DATA_ERROR ELEMENT_DATA_ERROR} (when
     * available)
     * <li>the invalid element text from an
     * {@link EDIStreamEvent#ELEMENT_OCCURRENCE_ERROR ELEMENT_OCCURRENCE_ERROR}
     * (when available)
     * <li>the string value of a segment tag for a
     * {@link EDIStreamEvent#SEGMENT_ERROR SEGMENT_ERROR} event
     * </ul>
     *
     * @return the current text or an empty {@link java.lang.String String}
     * @throws IllegalStateException
     *             if this state is not a valid text state
     */
    String getText();

    /**
     * Returns an array which contains the characters from this event (as
     * specified by {@link #getText}). This array should be treated as read-only
     * and transient. I.e. the array will contain the text characters until the
     * EDIStreamReader moves on to the next event. Attempts to hold onto the
     * character array beyond that time or modify the contents of the array are
     * breaches of the contract for this interface.
     *
     * @return the current text or an empty array
     * @throws IllegalStateException
     *             if this state is not a valid text state
     */
    char[] getTextCharacters();

    /**
     * Returns the the text associated with an event (as specified by
     * {@link #getText}). Text starting at "sourceStart" is copied into "target"
     * starting at "targetStart". Up to "length" characters are copied. The
     * number of characters actually copied is returned. The "sourceStart"
     * argument must be greater or equal to 0 and less than or equal to the
     * number of characters associated with the event. Usually, one requests
     * text starting at a "sourceStart" of 0. If the number of characters
     * actually copied is less than the "length", then there is no more text.
     * Otherwise, subsequent calls need to be made until all text has been
     * retrieved.
     *
     * For example:
     *
     * <pre>
     * int length = 1024;
     * char[] myBuffer = new char[length];
     *
     * for (int sourceStart = 0;; sourceStart += length) {
     *     int nCopied = stream.getTextCharacters(sourceStart, myBuffer, 0, length);
     *     if (nCopied &lt; length)
     *         break;
     * }
     * </pre>
     *
     * EDIStreamException may be thrown if there are any parsing errors in the
     * underlying source. The "targetStart" argument must be greater than or
     * equal to 0 and less than the length of "target", Length must be greater
     * than 0 and "targetStart + length" must be less than or equal to length of
     * "target".
     *
     * @param sourceStart
     *            - the index of the first character in the source array to copy
     * @param target
     *            - the destination array
     * @param targetStart
     *            - the start offset in the target array
     * @param length
     *            - the number of characters to copy
     * @return the number of characters actually copied
     * @throws IndexOutOfBoundsException
     *             if targetStart &lt; 0 or &gt; than the length of target
     * @throws IndexOutOfBoundsException
     *             if length &lt; 0 or targetStart + length &gt; length of
     *             target
     * @throws NullPointerException
     *             if target is null
     */
    int getTextCharacters(int sourceStart,
                          char[] target,
                          int targetStart,
                          int length);

    /**
     * Returns the offset into the text character array where the first
     * character (of this text event) is stored.
     *
     * @return offset into the text character array where the first character is
     *         stored
     * @throws IllegalStateException
     *             if this state is not a valid text state
     */
    int getTextStart();

    /**
     * Returns the length of the sequence of characters for this Text event
     * within the text character array.
     *
     * @return length of the sequence of characters for this Text event
     * @throws IllegalStateException
     *             if this state is not a valid text state
     */
    int getTextLength();

    /**
     * Return the current location of the processor. If the Location is unknown
     * the processor should return an implementation of Location that returns -1
     * for the location values. The location information is only valid until
     * next() is called.
     *
     * @return current location of the processor
     */
    Location getLocation();

    /**
     * Sets the number of bytes that should be read as binary data and not
     * interpreted as EDI data. This EDIStreamReader will return to normal EDI
     * parsing after reading this number of bytes. The byte immediately
     * following length bytes must be a delimiter valid in the scope of the
     * current interchange or an EDIStreamException will occur.
     *
     * This method must only be called immediately preceding a binary data
     * element. Attempts to call it while the reader is in any other state will
     * result in an IllegalStateException.
     *
     * <p>
     * Note: Applications parsing transactions which contain binary data
     * elements must call this method to avoid the binary data being parsed as
     * EDI content. The length of the binary data is typically found in a
     * companion data element preceding the binary element in the stream.
     * </p>
     *
     * @param length
     *            - the number of bytes to read as binary data and not as
     *            EDI-formatted
     * @throws IllegalStateException
     *             if this state is not a state which may precede a data
     *             element.
     * @throws EDIStreamException
     *             if there are IO errors allocating resources for binary data
     *             processing
     */
    void setBinaryDataLength(long length) throws EDIStreamException;

    /**
     * Returns a ByteBuffer object containing the binary element data read in
     * the previous data element. The limit of the buffer will be set to the
     * length of the data.
     *
     * @return buffer containing binary data
     * @throws IllegalStateException
     *             if the stream reader did not complete the scanning of a
     *             binary data element immediately preceding this call.
     */
    InputStream getBinaryData();

    /**
     * Returns an {@link EDIReference} for the schema type at the current point
     * in the reader's input stream. Information such as minimum and maximum
     * occurrences, as well as the {@link EDIType} may be obtained from the
     * reference. If the reader is utilizing an implementation schema and the
     * current schema type is an implemented type, the returned reference will
     * be an {@link EDITypeImplementation}.
     *
     * @return an {@link EDIReference} for the schema type at the current point
     *         in the reader's input stream
     *
     * @since 1.9
     */
    EDIReference getSchemaTypeReference();

    /**
     * Return true if the current event has text, false otherwise. The following
     * events have text:
     * <ul>
     * <li>START_SEGMENT
     * <li>END_SEGMENT
     * <li>ELEMENT_DATA
     * <li>ELEMENT_DATA_ERROR
     * <li>ELEMENT_OCCURRENCE_ERROR
     * <li>SEGMENT_ERROR
     * </ul>
     *
     * @return true if the current event has text, false otherwise
     * @since 1.20
     */
    boolean hasText();
}
