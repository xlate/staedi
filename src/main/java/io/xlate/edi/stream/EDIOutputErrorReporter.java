package io.xlate.edi.stream;

/**
 * This interface is used to report non-fatal errors detected in an EDI input.
 *
 * @since 1.9
 */
public interface EDIOutputErrorReporter {
    /**
     * Report the desired message in an application specific format. Only
     * warnings and non-fatal errors should be reported through this interface.
     *
     * Fatal errors will be thrown as {@link EDIStreamException}s.
     *
     * @param errorType
     *            the type of error detected
     * @param writer
     *            the EDIStreamWriter that encountered the error
     * @param location
     *            the location of the error, may be different than the location
     *            returned by the writer (e.g. for derived element positions)
     * @param data
     *            the invalid data, may be null (e.g. for missing required
     *            element errors)
     * @param referenceCode
     *            the reference code for the invalid data, if available from the
     *            current schema used for validation
     */
    void report(EDIStreamValidationError errorType, EDIStreamWriter writer, Location location, CharSequence data, CharSequence code);
}
