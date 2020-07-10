package io.xlate.edi.stream;

/**
 * This interface is used to report non-fatal errors detected in an EDI input.
 *
 * @since 1.4
 * @deprecated implement EDIInputErrorReporter instead. This interface will be
 *             removed in a future version of StAEDI.
 */
@SuppressWarnings({ "java:S1123", "java:S1133" })
@Deprecated/*(forRemoval = true, since = "1.9")*/
public interface EDIReporter extends EDIInputErrorReporter {
}
