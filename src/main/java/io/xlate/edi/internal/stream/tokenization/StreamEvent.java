package io.xlate.edi.internal.stream.tokenization;

import java.nio.CharBuffer;

import io.xlate.edi.internal.stream.StaEDIStreamLocation;
import io.xlate.edi.stream.EDIStreamEvent;
import io.xlate.edi.stream.EDIStreamValidationError;
import io.xlate.edi.stream.Location;

public class StreamEvent {

    private static final String TOSTRING_FORMAT = "type: %s, error: %s, data: %s, referenceCode: %s, location: { %s }";

    EDIStreamEvent type;
    EDIStreamValidationError errorType;
    CharBuffer data;
    CharSequence referenceCode;
    StaEDIStreamLocation location;

    @Override
    public String toString() {
        return String.format(TOSTRING_FORMAT, type, errorType, data, referenceCode, location);
    }

    public EDIStreamEvent getType() {
        return type;
    }

    public CharBuffer getData() {
        return data;
    }

    public CharSequence getReferenceCode() {
        return referenceCode;
    }

    public void setReferenceCode(CharSequence referenceCode) {
        this.referenceCode = referenceCode;
    }

    public Location getLocation() {
        return location;
    }
}
