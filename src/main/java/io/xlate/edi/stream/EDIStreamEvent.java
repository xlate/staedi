package io.xlate.edi.stream;

/**
 * Enumeration of stream event types that may be encountered by the
 * EDIStreamReader.
 *
 * @see EDIStreamReader#next()
 * @see EDIStreamReader#nextTag()
 * @see EDIStreamReader#getEventType()
 */
public enum EDIStreamEvent {

    /**
     * Event for a simple element or component element (within a composite).
     */
    ELEMENT_DATA,

    /**
     * Event for binary element data
     *
     * @see EDIStreamReader#getBinaryData()
     */
    ELEMENT_DATA_BINARY,

    /**
     * Event for the start of a composite element.
     */
    START_COMPOSITE,

    /**
     * Event for the end of a composite element.
     */
    END_COMPOSITE,

    /**
     * Event for the start of a segment.
     */
    START_SEGMENT,

    /**
     * Event for the end of a segment.
     */
    END_SEGMENT,

    /**
     * Event for the start of an interchange.
     */
    START_INTERCHANGE,

    /**
     * Event for the end of an interchange.
     */
    END_INTERCHANGE,

    /**
     * Event for the start of a functional group.
     */
    START_GROUP,

    /**
     * Event for the end of a functional group.
     */
    END_GROUP,

    /**
     * Event for the start of a transaction.
     */
    START_TRANSACTION,

    /**
     * Event for the end of a transaction.
     */
    END_TRANSACTION,

    /**
     * Event for the start of a data loop (logical grouping of segments).
     */
    START_LOOP,

    /**
     * Event for the end of a data loop (logical grouping of segments).
     */
    END_LOOP,

    /**
     * Event for an error relating to a segment
     */
    SEGMENT_ERROR(true),

    /**
     * Event for an error relating to the data received in an element.
     */
    ELEMENT_DATA_ERROR(true),

    /**
     * Event for an error relating to the an occurrence of an element. E.g. a
     * missing required element.
     */
    ELEMENT_OCCURRENCE_ERROR(true);

    private final boolean error;

    private EDIStreamEvent(boolean error) {
        this.error = error;
    }

    private EDIStreamEvent() {
        this(false);
    }

    /**
     * Indicates whether a particular EDIStreamEvent represents a validation
     * error.
     *
     * @return true when the event represents a validation error, otherwise
     *         false.
     */
    public boolean isError() {
        return error;
    }
}
