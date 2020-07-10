package io.xlate.edi.stream;

/**
 * This interface is used to report non-fatal errors detected in an EDI input.
 *
 * @since 1.9
 */
public interface EDIInputErrorReporter {
    /**
     * Report the desired message in an application specific format. Only
     * warnings and non-fatal errors should be reported through this interface.
     *
     * Fatal errors will be thrown as EDIStreamException.
     *
     * @param errorType
     *            the type of error detected
     * @param reader
     *            the EDIStreamReader that encountered the error
     * @throws EDIStreamException
     *             when errors occur calling the reader
     */
    void report(EDIStreamValidationError errorType, EDIStreamReader reader) throws EDIStreamException;
}
