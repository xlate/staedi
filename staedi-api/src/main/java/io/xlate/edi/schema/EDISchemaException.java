/*******************************************************************************
 * Copyright 2017 xlate.io LLC, http://www.xlate.io
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 ******************************************************************************/
package io.xlate.edi.schema;

import javax.xml.stream.Location;

public class EDISchemaException extends Exception {

    private static final long serialVersionUID = -1232370584780899896L;

    protected final transient Location location;
    protected final String message;

    public EDISchemaException() {
        super();
        this.location = null;
        this.message = null;
    }

    /**
     * Construct an exception with the associated message.
     *
     * @param message
     *            the message to report
     */
    public EDISchemaException(String message) {
        super(message);
        this.location = null;
        this.message = message;
    }

    /**
     * Construct an exception with the associated exception
     *
     * @param cause
     *            a nested exception
     */
    public EDISchemaException(Throwable cause) {
        super(cause);
        this.location = null;
        this.message = null;
    }

    /**
     * Construct an exception with the assocated message and exception
     *
     * @param cause
     *            a nested exception
     * @param message
     *            the message to report
     */
    public EDISchemaException(String message, Throwable cause) {
        super(message, cause);
        this.location = null;
        this.message = message;
    }

    /**
     * Construct an exception with the associated message, exception and
     * location.
     *
     * @param th
     *            a nested exception
     * @param msg
     *            the message to report
     * @param location
     *            the location of the error
     */
    public EDISchemaException(String message, Location location, Throwable cause) {
        super("EDISchemaException at [row,col]:[" + location.getLineNumber()
                + "," + location.getColumnNumber() + "]\n" + "Message: "
                + message,
              cause);
        this.location = location;
        this.message = message;
    }

    /**
     * Construct an exception with the assocated message, exception and
     * location.
     *
     * @param msg
     *            the message to report
     * @param location
     *            the location of the error
     */
    public EDISchemaException(String message, Location location) {
        super("EDISchemaException at [row,col]:[" + location.getLineNumber()
                + "," + location.getColumnNumber() + "]\n" + "Message: "
                + message);
        this.location = location;
        this.message = message;
    }

    /**
     * Gets the location of the exception
     *
     * @return the location of the exception, may be null if none is available
     */
    public Location getLocation() {
        return location;
    }

    public String getOriginalMessage() {
        return this.message;
    }
}
