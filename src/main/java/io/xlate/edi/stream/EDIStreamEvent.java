package io.xlate.edi.stream;

public enum EDIStreamEvent {
    ELEMENT_DATA,
    ELEMENT_DATA_BINARY,

    START_COMPOSITE,
    END_COMPOSITE,

    START_SEGMENT,
    END_SEGMENT,

    START_INTERCHANGE,
    END_INTERCHANGE,

    START_GROUP,
    END_GROUP,

    START_TRANSACTION,
    END_TRANSACTION,

    START_LOOP,
    END_LOOP,

    SEGMENT_ERROR(true),
    ELEMENT_DATA_ERROR(true),
    ELEMENT_OCCURRENCE_ERROR(true);

    private final boolean error;

    private EDIStreamEvent(boolean error) {
        this.error = error;
    }

    private EDIStreamEvent() {
        this(false);
    }

    public boolean isError() {
        return error;
    }
}
