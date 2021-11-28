package io.xlate.edi.test;

import java.util.Objects;

import io.xlate.edi.stream.EDIStreamEvent;
import io.xlate.edi.stream.EDIStreamReader;
import io.xlate.edi.stream.EDIStreamValidationError;
import io.xlate.edi.stream.Location;

public class StaEDITestEvent {

    public final EDIStreamEvent event;
    public final EDIStreamValidationError error;
    public final String text;
    public final String referenceCode;
    public final Location location;

    public StaEDITestEvent(EDIStreamEvent event, EDIStreamValidationError error, String text, String referenceCode, Location location) {
        super();
        this.event = event;
        this.error = error;
        this.text = text;
        this.referenceCode = referenceCode;
        this.location = location;
    }

    public static StaEDITestEvent from(EDIStreamReader reader, boolean includeLocation) {
        EDIStreamEvent event = reader.getEventType();
        boolean error;

        switch (event) {
        case SEGMENT_ERROR:
        case ELEMENT_OCCURRENCE_ERROR:
        case ELEMENT_DATA_ERROR:
            error = true;
            break;
        default:
            error = false;
            break;
        }

        return new StaEDITestEvent(reader.getEventType(),
                                   error ? reader.getErrorType() : null,
                                   reader.hasText() ? reader.getText() : null,
                                   reader.getReferenceCode(),
                                   includeLocation ? reader.getLocation().copy() : null);
    }

    public static StaEDITestEvent forError(EDIStreamValidationError error, String text, String referenceCode) {
        return new StaEDITestEvent(error.getCategory(), error, text, referenceCode, null);
    }

    public static StaEDITestEvent forEvent(EDIStreamEvent event, String text, String referenceCode) {
        return new StaEDITestEvent(event, null, text, referenceCode, null);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof StaEDITestEvent) {
            StaEDITestEvent other = (StaEDITestEvent) obj;

            return event == other.event
                    && error == other.error
                    && Objects.equals(text, other.text)
                    && Objects.equals(referenceCode, other.referenceCode)
                    && Objects.equals(location, other.location);
        }

        return false;
    }

    @Override
    public String toString() {
        StringBuilder buffer = new StringBuilder("StaEDITestEvent");
        buffer.append('(');

        buffer.append("event=").append(event);
        buffer.append(", ").append("error=").append(error);
        buffer.append(", ").append("referenceCode=").append(referenceCode);
        buffer.append(", ").append("text=").append(text);
        buffer.append(", ").append("location=").append(location);

        return buffer.append(')').append('\n').toString();
    }
}
