package io.xlate.edi.stream;

/**
 * Runtime exception that may occur when reading or writing EDI any the EDI
 * error event would not otherwise be handled by an application. For example,
 * when writing EDI and no {@linkplain EDIOutputErrorReporter} is in use.
 */
public class EDIValidationException extends RuntimeException {

    private static final long serialVersionUID = 5811097042070037687L;

    /**
     * The stream event associated with the error, one of the events for which
     * {@linkplain EDIStreamEvent#isError()} is true.
     */
    protected final EDIStreamEvent event;

    /**
     * The actual {@linkplain EDIStreamValidationError error} of this exception.
     */
    protected final EDIStreamValidationError error;

    /**
     * {@linkplain Location} of the exception
     */
    protected final transient Location location;

    /**
     * The data from the data stream (input for a reader or output for a writer)
     * associated with the validation error.
     */
    protected final transient CharSequence data;

    /**
     * The next exception when more than one validation error occurred in
     * processing the stream.
     */
    @SuppressWarnings("java:S1165") // Intentionally allow field to be set after instantiation
    private EDIValidationException nextException;

    /**
     * Construct an EDIValidationException with the given data elements.
     *
     * @param event
     *            stream event (required)
     * @param error
     *            validation error (required)
     * @param location
     *            location of the validation error
     * @param data
     *            data associated with the validation error
     */
    public EDIValidationException(EDIStreamEvent event,
            EDIStreamValidationError error,
            Location location,
            CharSequence data) {
        super("Encountered " + event + " [" + error + "]" + (location != null ? " " + location.toString() : ""));
        this.event = event;
        this.error = error;
        this.location = location != null ? location.copy() : null;
        this.data = data;
    }

    /**
     * Get the stream event associated with the error
     *
     * @return event associated with the error
     */
    public EDIStreamEvent getEvent() {
        return event;
    }

    /**
     * Get the type of validation error
     *
     * @return type of validation error
     */
    public EDIStreamValidationError getError() {
        return error;
    }

    /**
     * Get the location of the validation error
     *
     * @return location of the validation error
     */
    public Location getLocation() {
        return location;
    }

    /**
     * Get the data associated with the validation error
     *
     * @return character data associated with the validation error
     */
    public CharSequence getData() {
        return data;
    }

    /**
     * Retrieves the exception chained to this
     * <code>EDIValidationException</code> object by setNextException(EDIValidationException ex).
     *
     * @return the next <code>EDIValidationException</code> object in the chain;
     *         <code>null</code> if there are none
     * @see #setNextException
     */
    public EDIValidationException getNextException() {
        return nextException;
    }

    /**
     * Adds an <code>EDIValidationException</code> object to the end of the chain.
     *
     * @param ex the new exception that will be added to the end of
     *            the <code>EDIValidationException</code> chain
     * @see #getNextException
     */
    public void setNextException(EDIValidationException ex) {
        EDIValidationException current = this;

        while (current.nextException != null) {
            current = current.nextException;
        }

        current.nextException = ex;
    }
}
