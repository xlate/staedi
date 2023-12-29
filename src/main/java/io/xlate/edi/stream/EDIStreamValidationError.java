package io.xlate.edi.stream;

/**
 * Enumeration of validation error types that may be encountered by the
 * EDIStreamReader or EDIStreamWriter.
 *
 * @see EDIStreamReader#getErrorType()
 * @see EDIInputErrorReporter#report(EDIStreamValidationError, EDIStreamReader)
 * @see EDIOutputErrorReporter#report(EDIStreamValidationError, EDIStreamWriter,
 *      Location, CharSequence, io.xlate.edi.schema.EDIReference)
 */
public enum EDIStreamValidationError {
    /**
     * No error
     */
    NONE(null),

    /**
     * An unrecognized (undefined) segment was encountered in the data stream.
     *
     * @deprecated Not used by StAEDI - reserved for future use.
     */
    UNRECOGNIZED_SEGMENT_ID(EDIStreamEvent.SEGMENT_ERROR),

    /**
     * Segment was received in an unexpected position in the data stream. This
     * error may occur when a segment is encountered in a loop for which it is
     * not defined.
     */
    UNEXPECTED_SEGMENT(EDIStreamEvent.SEGMENT_ERROR),

    /**
     * Segment was defined (via a schema) with a minimum number of occurrences,
     * but the number of occurrences encountered in the data stream does not
     * meet the requirement.
     */
    MANDATORY_SEGMENT_MISSING(EDIStreamEvent.SEGMENT_ERROR),

    /**
     * Loop encountered in the data stream has more occurrences than allowed by
     * the configured schema.
     */
    LOOP_OCCURS_OVER_MAXIMUM_TIMES(EDIStreamEvent.SEGMENT_ERROR),

    /**
     * Segment encountered in the data stream has more occurrences than allowed
     * by the configured schema.
     */
    SEGMENT_EXCEEDS_MAXIMUM_USE(EDIStreamEvent.SEGMENT_ERROR),

    /**
     * An unrecognized (undefined) segment was encountered in the data stream.
     */
    SEGMENT_NOT_IN_DEFINED_TRANSACTION_SET(EDIStreamEvent.SEGMENT_ERROR),

    /**
     * Segment was encountered out of order. This will occur if the segment is
     * valid within the current data loop, otherwise the error will be
     * {@link #UNEXPECTED_SEGMENT}.
     */
    SEGMENT_NOT_IN_PROPER_SEQUENCE(EDIStreamEvent.SEGMENT_ERROR),

    /**
     * Segment contains data element errors.
     *
     * @deprecated Not used by StAEDI - reserved for future use.
     */
    SEGMENT_HAS_DATA_ELEMENT_ERRORS(EDIStreamEvent.SEGMENT_ERROR),

    /**
     * Segment is defined as unused (maximum use is zero) in the schema but was
     * encountered in the data stream.
     */
    IMPLEMENTATION_UNUSED_SEGMENT_PRESENT(EDIStreamEvent.SEGMENT_ERROR),

    /**
     * @deprecated Not used by StAEDI - reserved for future use.
     */
    IMPLEMENTATION_DEPENDENT_SEGMENT_MISSING(EDIStreamEvent.SEGMENT_ERROR),

    /**
     * Loop is defined with a minimum number of occurrences in the
     * implementation schema but too few were encountered in the data stream.
     */
    IMPLEMENTATION_LOOP_OCCURS_UNDER_MINIMUM_TIMES(EDIStreamEvent.SEGMENT_ERROR),

    /**
     * Segment is defined with a minimum number of occurrences in the
     * implementation schema but too few were encountered in the data stream.
     */
    IMPLEMENTATION_SEGMENT_BELOW_MINIMUM_USE(EDIStreamEvent.SEGMENT_ERROR),

    /**
     * @deprecated Not used by StAEDI - reserved for future use.
     */
    IMPLEMENTATION_DEPENDENT_UNUSED_SEGMENT_PRESENT(EDIStreamEvent.SEGMENT_ERROR),

    /**
     * Segment is configured as conditionally required (relative to other
     * segments in the loop) in the schema but was not present in the data
     * stream.
     */
    CONDITIONAL_REQUIRED_SEGMENT_MISSING(EDIStreamEvent.SEGMENT_ERROR),

    /**
     * Segment is configured as conditionally excluded (relative to other
     * segments in the loop) in the schema but was present in the data stream.
     */
    SEGMENT_EXCLUSION_CONDITION_VIOLATED(EDIStreamEvent.SEGMENT_ERROR),

    /**
     * Element was defined (via a schema) with a minimum number of occurrences
     * but the number of occurrences encountered in the data stream does not
     * meet the requirement.
     */
    REQUIRED_DATA_ELEMENT_MISSING(EDIStreamEvent.ELEMENT_OCCURRENCE_ERROR),

    /**
     * Element is configured as conditionally required (relative to other
     * elements in the segment) in the schema but was not present in the data
     * stream.
     */
    CONDITIONAL_REQUIRED_DATA_ELEMENT_MISSING(EDIStreamEvent.ELEMENT_OCCURRENCE_ERROR),

    /**
     * Element is configured as conditionally required (relative to other
     * elements in the segment) in the schema but was not present in the data
     * stream.
     */
    TOO_MANY_DATA_ELEMENTS(EDIStreamEvent.ELEMENT_OCCURRENCE_ERROR),

    /**
     * Element is configured as conditionally excluded (relative to other
     * elements in the segment) in the schema but was present in the data
     * stream.
     */
    EXCLUSION_CONDITION_VIOLATED(EDIStreamEvent.ELEMENT_OCCURRENCE_ERROR),

    /**
     * Element repeats more times than allowed by the configured schema.
     */
    TOO_MANY_REPETITIONS(EDIStreamEvent.ELEMENT_OCCURRENCE_ERROR),

    /**
     * Element contains more components than defined by the configured schema.
     */
    TOO_MANY_COMPONENTS(EDIStreamEvent.ELEMENT_OCCURRENCE_ERROR),

    /**
     * @deprecated Not used by StAEDI - reserved for future use.
     */
    IMPLEMENTATION_DEPENDENT_DATA_ELEMENT_MISSING(EDIStreamEvent.ELEMENT_OCCURRENCE_ERROR),

    /**
     * Element is defined as unused (maximum use is zero) in the schema but was
     * encountered in the data stream.
     */
    IMPLEMENTATION_UNUSED_DATA_ELEMENT_PRESENT(EDIStreamEvent.ELEMENT_OCCURRENCE_ERROR),

    /**
     * Element is defined with a minimum number of occurrences in the
     * implementation schema but too few were encountered in the data stream.
     */
    IMPLEMENTATION_TOO_FEW_REPETITIONS(EDIStreamEvent.ELEMENT_OCCURRENCE_ERROR),

    /**
     * @deprecated Not used by StAEDI - reserved for future use.
     */
    IMPLEMENTATION_DEPENDENT_UNUSED_DATA_ELEMENT_PRESENT(EDIStreamEvent.ELEMENT_OCCURRENCE_ERROR),

    /**
     * Element length is less than the minimum length required by the configured
     * schema.
     */
    DATA_ELEMENT_TOO_SHORT(EDIStreamEvent.ELEMENT_DATA_ERROR),

    /**
     * Element length is greater than the maximum length allowed by the
     * configured schema.
     */
    DATA_ELEMENT_TOO_LONG(EDIStreamEvent.ELEMENT_DATA_ERROR),

    /**
     * Element contains invalid character data.
     */
    INVALID_CHARACTER_DATA(EDIStreamEvent.ELEMENT_DATA_ERROR),

    /**
     * Element contains a value that is not present in the set of values
     * configured in the schema.
     */
    INVALID_CODE_VALUE(EDIStreamEvent.ELEMENT_DATA_ERROR),

    /**
     * Element is defined with type
     * {@linkplain io.xlate.edi.schema.EDISimpleType.Base#DATE} but the data
     * encountered does not match formatted as a date.
     */
    INVALID_DATE(EDIStreamEvent.ELEMENT_DATA_ERROR),

    /**
     * Element is defined with type
     * {@linkplain io.xlate.edi.schema.EDISimpleType.Base#TIME} but the data
     * encountered does not match formatted as a time.
     */
    INVALID_TIME(EDIStreamEvent.ELEMENT_DATA_ERROR),

    /**
     * Element contains a value that is not present in the set of values
     * configured in the implementation schema.
     */
    IMPLEMENTATION_INVALID_CODE_VALUE(EDIStreamEvent.ELEMENT_DATA_ERROR),

    /**
     * @deprecated Not used by StAEDI - reserved for future use.
     */
    IMPLEMENTATION_PATTERN_MATCH_FAILURE(EDIStreamEvent.ELEMENT_DATA_ERROR),

    // Control number and counter validation errors

    /**
     * Control number/reference in the trailer segment does not match the value
     * encountered in the header.
     */
    CONTROL_REFERENCE_MISMATCH(EDIStreamEvent.ELEMENT_DATA_ERROR),

    /**
     * Control count (e.g. count of segments, transactions, or functional
     * groups) encountered in the trailer does not match the actual count in the
     * stream.
     */
    CONTROL_COUNT_DOES_NOT_MATCH_ACTUAL_COUNT(EDIStreamEvent.ELEMENT_DATA_ERROR);

    private EDIStreamEvent category;

    private EDIStreamValidationError(EDIStreamEvent category) {
        this.category = category;
    }

    /**
     * Provides the category of the validation error. The category is one of the
     * EDIStreamEvents where {@linkplain EDIStreamEvent#isError()} is true.
     *
     * @return the EDIStreamEvent category of the validation error.
     */
    public EDIStreamEvent getCategory() {
        return category;
    }
}
