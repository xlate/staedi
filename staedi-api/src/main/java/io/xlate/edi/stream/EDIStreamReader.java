package io.xlate.edi.stream;

import io.xlate.edi.schema.EDISchemaException;
import io.xlate.edi.schema.Schema;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.NoSuchElementException;

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
	Object getProperty(String name) throws IllegalArgumentException;

	/**
	 * Retrieve a read-only map of delimiters in use for the stream being read.
	 *
	 * @return The value of the property
	 * @throws IllegalStateException
	 *             if called outside of an interchange
	 */
	Map<String, Character> getDelimiters() throws IllegalStateException;

	/**
	 * Get next parsing event
	 *
	 * @return the integer code corresponding to the current parse event
	 * @throws NoSuchElementException
	 *             if this is called when hasNext() returns false
	 * @throws EDIStreamException
	 *             if there is an error processing the underlying XML source
	 */
	int next() throws NoSuchElementException, EDIStreamException;

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
	int nextTag() throws NoSuchElementException, EDIStreamException;

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
	 * @throws EDIStreamException
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
	int getEventType();

	/**
	 * Get the EDI standard name. Calls to this method are only valid when the
	 * current event type is START_INTERCHANGE.
	 *
	 * @return the name of the EDI standard
	 * @throws IllegalStateException
	 *             when the current event type is not START_INTERCHANGE
	 */
	String getStandard() throws IllegalStateException;

	/**
	 * Get the interchange version declared on the interchange begin segment.
	 * Calls to this method are only valid when the current event type is
	 * START_INTERCHANGE.
	 *
	 * @return the version
	 * @throws IllegalStateException
	 *             when the current event type is not START_INTERCHANGE
	 */
	String getVersion() throws IllegalStateException;

	/**
	 * Sets the schema to be used for validation of within this stream reader.
	 * Calls to this method are only valid when the current event type is
	 * START_INTERCHANGE.
	 *
	 * @param schema
	 * @throws IllegalStateException
	 *             when the current event type is not START_INTERCHANGE
	 */
	void setSchema(Schema schema) throws IllegalStateException;

	/**
	 * Add an additional schema to be used for validation of within this stream
	 * reader. The root contents of the added schema will be added as the
	 * immediate next siblings of the current segment in the stream.
	 *
	 * Calls to this method are only valid when the current event type is
	 * END_SEGMENT and a schema was previously set on this reader.
	 *
	 * @param schema
	 * @throws IllegalStateException
	 *             when the current event type is not START_INTERCHANGE
	 * @throws EDISchemaException
	 *             when an error occurs adding the schema to the currently set
	 *             schema
	 */
	void addSchema(Schema schema)
			throws IllegalStateException, EDISchemaException;

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
	String getReferenceCode() throws IllegalStateException;

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
	int getErrorType() throws IllegalStateException;

	/**
	 * Returns the current value of the parse event as a string. This returns
	 * the string value of an ELEMENT_DATA event, and the string value of a
	 * segment tag in a START_SEGMENT and END_SEGMENT event. During an
	 * ELEMENT_ERROR event, this contains the invalid element.
	 *
	 * @return the current text or null
	 * @throws IllegalStateException
	 *             if this state is not a valid text state
	 */
	String getText() throws IllegalStateException;

	/**
	 * Returns an array which contains the characters from this event. This
	 * array should be treated as read-only and transient. I.e. the array will
	 * contain the text characters until the EDIStreamReader moves on to the
	 * next event. Attempts to hold onto the character array beyond that time or
	 * modify the contents of the array are breaches of the contract for this
	 * interface.
	 *
	 * @return the current text or an empty array
	 * @throws IllegalStateException
	 *             if this state is not a valid text state
	 */
	char[] getTextCharacters() throws IllegalStateException;

	/**
	 * Gets the the text associated with a ELEMENT_DATA, ELEMENT_ERROR,
	 * START_SEGMENT, or END_SEGMENT event. Text starting at "sourceStart" is
	 * copied into "target" starting at "targetStart". Up to "length" characters
	 * are copied. The number of characters actually copied is returned. The
	 * "sourceStart" argument must be greater or equal to 0 and less than or
	 * equal to the number of characters associated with the event. Usually, one
	 * requests text starting at a "sourceStart" of 0. If the number of
	 * characters actually copied is less than the "length", then there is no
	 * more text. Otherwise, subsequent calls need to be made until all text has
	 * been retrieved.
	 *
	 * For example:
	 *
	 * <pre>
	 * int length = 1024;
	 * char[] myBuffer = new char[length];
	 *
	 * for (int sourceStart = 0;; sourceStart += length) {
	 * 	int nCopied = stream.getTextCharacters(sourceStart, myBuffer, 0, length);
	 * 	if (nCopied &lt; length)
	 * 		break;
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
	 *             if targetStart < 0 or > than the length of target
	 * @throws IndexOutOfBoundsException
	 *             if length < 0 or targetStart + length > length of target
	 * @throws NullPointerException
	 *             if target is null
	 */
	int getTextCharacters(
			int sourceStart,
			char[] target,
			int targetStart,
			int length)
			throws IndexOutOfBoundsException, UnsupportedOperationException,
			NullPointerException;

	/**
	 * Returns the offset into the text character array where the first
	 * character (of this text event) is stored.
	 *
	 * @return offset into the text character array where the first character is
	 *         stored
	 * @throws IllegalStateException
	 *             if this state is not a valid text state
	 */
	int getTextStart() throws IllegalStateException;

	/**
	 * Returns the length of the sequence of characters for this Text event
	 * within the text character array.
	 *
	 * @return length of the sequence of characters for this Text event
	 * @throws IllegalStateException
	 *             if this state is not a valid text state
	 */
	int getTextLength() throws IllegalStateException;

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
	void setBinaryDataLength(long length)
			throws IllegalStateException, EDIStreamException;

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
	InputStream getBinaryData() throws IllegalStateException;
}
