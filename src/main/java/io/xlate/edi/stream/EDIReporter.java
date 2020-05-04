package io.xlate.edi.stream;

/**
 * This interface is used to report non-fatal errors detected in an EDI input.
 *
 * @since 1.4
 */
public interface EDIReporter {
    /**
     * Report the desired message in an application specific format. Only
     * warnings and non-fatal errors should be reported through this interface.
     *
     * Fatal errors will be thrown as EDIStreamException.
     *
     * @param errorType
     *            the type of error detected
     * @param location
     *            the location of the error, if available
     * @throws EDIStreamException
     */
    public void report(EDIStreamValidationError errorType, Location location) throws EDIStreamException;
}
