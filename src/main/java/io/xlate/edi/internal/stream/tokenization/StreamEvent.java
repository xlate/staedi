package io.xlate.edi.internal.stream.tokenization;

import java.nio.CharBuffer;

import io.xlate.edi.internal.stream.CharArraySequence;
import io.xlate.edi.internal.stream.StaEDIStreamLocation;
import io.xlate.edi.stream.EDIStreamEvent;
import io.xlate.edi.stream.EDIStreamValidationError;
import io.xlate.edi.stream.Location;

public class StreamEvent {

    private static final String TOSTRING_FORMAT = "type: %s, error: %s, data: %s, referenceCode: %s, location: { %s }";

    EDIStreamEvent type;
    EDIStreamValidationError errorType;

    CharBuffer data;
    boolean dataNull = true;

    CharBuffer referenceCode;
    boolean referenceCodeNull = true;

    StaEDIStreamLocation location;

    @Override
    public String toString() {
        return String.format(TOSTRING_FORMAT, type, errorType, data, referenceCode, location);
    }

    public EDIStreamEvent getType() {
        return type;
    }

    public CharBuffer getData() {
        return dataNull ? null : data;
    }

    public void setData(CharSequence data) {
        if (data instanceof CharArraySequence) {
            this.data = put(this.data, (CharArraySequence) data);
            this.dataNull = false;
        } else if (data != null) {
            this.data = put(this.data, data);
            this.dataNull = false;
        } else {
            this.dataNull = true;
        }
    }

    public CharSequence getReferenceCode() {
        return referenceCodeNull ? null : referenceCode;
    }

    public String getReferenceCodeString() {
        return referenceCodeNull ? null : referenceCode.toString();
    }

    public void setReferenceCode(CharSequence referenceCode) {
        this.referenceCodeNull = (referenceCode == null);

        if (!this.referenceCodeNull) {
            this.referenceCode = put(this.referenceCode, referenceCode);
        } else {
            this.referenceCode = put(this.referenceCode, "");
        }
    }

    public Location getLocation() {
        return location;
    }

    public void setLocation(Location location) {
        if (this.location == null) {
            this.location = new StaEDIStreamLocation(location);
        }

        this.location.set(location);
    }

    static CharBuffer put(CharBuffer buffer, CharArraySequence data) {
        final int length = data.length();

        if (buffer == null || buffer.capacity() < length) {
            buffer = CharBuffer.allocate(length);
        }

        buffer.clear();

        if (length > 0) {
            data.putToBuffer(buffer);
        }

        buffer.flip();

        return buffer;
    }

    static CharBuffer put(CharBuffer buffer, CharSequence text) {
        int length = text.length();

        if (buffer == null || buffer.capacity() < length) {
            buffer = CharBuffer.allocate(length);
        }

        buffer.clear();
        for (int i = 0; i < length; i++) {
            buffer.put(text.charAt(i));
        }
        buffer.flip();

        return buffer;
    }
}
