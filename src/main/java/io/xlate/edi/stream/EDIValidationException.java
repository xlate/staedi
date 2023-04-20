package io.xlate.edi.stream;

public class EDIValidationException extends RuntimeException {

    private static final long serialVersionUID = 5811097042070037687L;

    protected final EDIStreamEvent event;
    protected final EDIStreamValidationError error;
    protected final transient Location location;
    protected final transient CharSequence data;

    @SuppressWarnings("java:S1165") // Intentionally allow field to be set after instantiation
    private EDIValidationException nextException;

    public EDIValidationException(EDIStreamEvent event,
            EDIStreamValidationError error,
            Location location,
            CharSequence data) {
        super("Encountered " + event + " [" + error + "] for data={" + data + "} " + (location != null ? " " + location.toString() : ""));
        this.event = event;
        this.error = error;
        this.location = location != null ? location.copy() : null;
        this.data = data;
    }

    public EDIStreamEvent getEvent() {
        return event;
    }

    public EDIStreamValidationError getError() {
        return error;
    }

    public Location getLocation() {
        return location;
    }

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
