package io.xlate.edi.schema;

import io.xlate.edi.stream.Location;

/**
 * Specification of the position of an element. Provides information about the
 * element position within a segment and (optionally) a component element
 * position within a composite element.
 *
 * @since 1.18
 * @see io.xlate.edi.schema.implementation.Discriminator
 */
public interface EDIElementPosition {

    /**
     * Get the element position within a segment.
     *
     * @return the element position within a segment
     */
    int getElementPosition();

    /**
     * Get the component position within a composite element. Returns -1 if not
     * available.
     *
     * @return the component position with a composite element or -1
     */
    int getComponentPosition();

    /**
     * Determine if the given location's element and component matches this
     * element position specification.
     *
     * @param location
     *            the location to match against this position
     * @return true if the element and component positions match, otherwise
     *         false
     */
    default boolean matchesLocation(Location location) {
        return location.getElementPosition() == getElementPosition()
                && location.getComponentPosition() == getComponentPosition();
    }

}
