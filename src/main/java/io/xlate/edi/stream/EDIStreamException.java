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
package io.xlate.edi.stream;

public class EDIStreamException extends Exception {

    private static final long serialVersionUID = -1232370584780899896L;

    protected final transient Location location;

    /**
     * Construct an exception with the associated message.
     *
     * @param message
     *            the message to report
     */
    public EDIStreamException(String message) {
        super(message);
        location = null;
    }

    /**
     * Construct an exception with the associated exception
     *
     * @param cause
     *            a nested exception
     */
    public EDIStreamException(Throwable cause) {
        super(cause);
        location = null;
    }

    private static String displayLocation(Location location) {
        StringBuilder display = new StringBuilder();

        display.append("segment ");

        if (location.getSegmentTag() != null) {
            display.append(location.getSegmentTag());
        }

        display.append(" at position ");
        display.append(String.valueOf(location.getSegmentPosition()));

        if (location.getElementPosition() > -1) {
            display.append(", element ");
            display.append(String.valueOf(location.getElementPosition()));

            if (location.getElementOccurrence() > -1) {
                display.append("(occurrence ");
                display.append(String.valueOf(location.getElementOccurrence()));
                display.append(')');
            }
        }

        if (location.getComponentPosition() > -1) {
            display.append(", component ");
            display.append(String.valueOf(location.getComponentPosition()));
        }

        return display.toString();
    }

    /**
     * Construct an exception with the associated message, exception and
     * location.
     *
     * @param message
     *            the message to report
     * @param location
     *            the location of the error
     * @param cause
     *            a nested error / exception
     */
    public EDIStreamException(String message, Location location, Throwable cause) {
        super(message + " in " + displayLocation(location), cause);
        this.location = location;
    }

    /**
     * Construct an exception with the associated message, exception and
     * location.
     *
     * @param message
     *            the message to report
     * @param location
     *            the location of the error
     */
    public EDIStreamException(String message, Location location) {
        super(message + " in " + displayLocation(location));
        this.location = location;
    }

    /**
     * Gets the location of the exception
     *
     * @return the location of the exception, may be null if none is available
     */
    public Location getLocation() {
        return location;
    }
}
