package io.xlate.edi.stream;

public class EDIStreamException extends Exception {

	private static final long serialVersionUID = -1232370584780899896L;

	protected Location location;

	public EDIStreamException() {
		super();
	}

	/**
	 * Construct an exception with the associated message.
	 *
	 * @param message
	 *            the message to report
	 */
	public EDIStreamException(String message) {
		super(message);
	}

	/**
	 * Construct an exception with the associated exception
	 *
	 * @param cause
	 *            a nested exception
	 */
	public EDIStreamException(Throwable cause) {
		super(cause);
	}

	/**
	 * Construct an exception with the associated message and exception
	 *
	 * @param cause
	 *            a nested exception
	 * @param message
	 *            the message to report
	 */
	public EDIStreamException(String message, Throwable cause) {
		super(message, cause);
	}

	private static String displayLocation(Location location) {
		return location.getSegmentPosition() + " : " + location.getElementPosition();
	}

	/**
	 * Construct an exception with the associated message, exception and
	 * location.
	 *
	 * @param message
	 *            the message to report
	 * @param location
	 *            the location of the error
	 * @param cause
	 *            a nested error / exception
	 */
	public EDIStreamException(String message, Location location, Throwable cause) {
		super("EDIStreamException at [seg,ele]:[" + displayLocation(location) + "]\n" + "Message: "
				+ message, cause);
		this.location = location;
	}

	/**
	 * Construct an exception with the associated message, exception and
	 * location.
	 *
	 * @param message
	 *            the message to report
	 * @param location
	 *            the location of the error
	 */
	public EDIStreamException(String message, Location location) {
		super("EDIStreamException at [seg,ele]:[" + displayLocation(location) + "]\n" + "Message: "
				+ message);
		this.location = location;
	}

	/**
	 * Gets the location of the exception
	 *
	 * @return the location of the exception, may be null if none is available
	 */
	public Location getLocation() {
		return location;
	}
}
