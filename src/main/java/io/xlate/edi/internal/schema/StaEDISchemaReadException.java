package io.xlate.edi.internal.schema;

import javax.xml.stream.Location;

class StaEDISchemaReadException extends RuntimeException {

    private static final long serialVersionUID = -5555580673080112522L;
    protected final transient Location location;

    public StaEDISchemaReadException(String message, Location location, Throwable cause) {
        super(message, cause);
        this.location = location;
    }

    public Location getLocation() {
        return location;
    }
}
