package io.xlate.edi.schema;

import javax.xml.stream.Location;

public class EDISchemaException extends Exception {

	private static final long serialVersionUID = -1232370584780899896L;

	protected Location location;
	protected String message;

	public EDISchemaException() {
		super();
	}

	/**
	 * Construct an exception with the associated message.
	 *
	 * @param message
	 *            the message to report
	 */
	public EDISchemaException(String message) {
		super(message);
		this.message = message;
	}

	/**
	 * Construct an exception with the associated exception
	 *
	 * @param cause
	 *            a nested exception
	 */
	public EDISchemaException(Throwable cause) {
		super(cause);
	}

	/**
	 * Construct an exception with the assocated message and exception
	 *
	 * @param cause
	 *            a nested exception
	 * @param message
	 *            the message to report
	 */
	public EDISchemaException(String message, Throwable cause) {
		super(message, cause);
		this.message = message;
	}

	/**
	 * Construct an exception with the assocated message, exception and
	 * location.
	 *
	 * @param th
	 *            a nested exception
	 * @param msg
	 *            the message to report
	 * @param location
	 *            the location of the error
	 */
	public EDISchemaException(String message, Location location, Throwable cause) {
		super("EDISchemaException at [row,col]:[" + location.getLineNumber()
				+ "," + location.getColumnNumber() + "]\n" + "Message: "
				+ message, cause);
		this.location = location;
		this.message = message;
	}

	/**
	 * Construct an exception with the assocated message, exception and
	 * location.
	 *
	 * @param msg
	 *            the message to report
	 * @param location
	 *            the location of the error
	 */
	public EDISchemaException(String message, Location location) {
		super("EDISchemaException at [row,col]:[" + location.getLineNumber()
				+ "," + location.getColumnNumber() + "]\n" + "Message: "
				+ message);
		this.location = location;
		this.message = message;
	}

	/**
	 * Gets the location of the exception
	 *
	 * @return the location of the exception, may be null if none is available
	 */
	public Location getLocation() {
		return location;
	}

	public String getOriginalMessage() {
		return this.message;
	}
}
