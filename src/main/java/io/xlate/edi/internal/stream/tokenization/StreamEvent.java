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
    CharBuffer referenceCode;
    StaEDIStreamLocation location;

    @Override
    public String toString() {
        return String.format(TOSTRING_FORMAT, type, errorType, data, referenceCode, location);
    }

    public EDIStreamEvent getType() {
        return type;
    }

    public CharSequence getData() {
        return data;
    }

    public void setData(CharSequence data) {
        if (data instanceof CharArraySequence) {
            this.data = put(this.data, (CharArraySequence) data);
        } else if (data != null) {
            this.data = put(this.data, data);
        } else {
            this.data = null;
        }
    }

    public CharSequence getReferenceCode() {
        return referenceCode;
    }

    public void setReferenceCode(CharSequence referenceCode) {
        if (referenceCode != null) {
            this.referenceCode = put(this.referenceCode, referenceCode);
        } else {
            this.referenceCode = null;
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

    static CharBuffer put(CharBuffer buffer, CharArraySequence holder) {
        final int length = holder != null ? holder.length() : 50;

        if (buffer == null || buffer.capacity() < length) {
            buffer = CharBuffer.allocate(length);
        }

        buffer.clear();

        if (holder != null && length > 0) {
            holder.putToBuffer(buffer);
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
