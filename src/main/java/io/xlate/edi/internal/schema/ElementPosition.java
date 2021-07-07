package io.xlate.edi.internal.schema;

import java.util.Objects;

import io.xlate.edi.schema.EDIElementPosition;

public class ElementPosition implements EDIElementPosition {

    private static final String TOSTRING_FORMAT = "%d.%d";
    private final int elementPosition;
    private final int componentPosition;

    public ElementPosition(int elementPosition, int componentPosition) {
        super();
        this.elementPosition = elementPosition;
        this.componentPosition = componentPosition;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (!getClass().isInstance(o)) {
            return false;
        }

        ElementPosition other = (ElementPosition) o;

        return Objects.equals(elementPosition, other.elementPosition) &&
                Objects.equals(componentPosition, other.componentPosition);
    }

    @Override
    public int hashCode() {
        return Objects.hash(elementPosition, componentPosition);
    }

    @Override
    public String toString() {
        return String.format(TOSTRING_FORMAT, elementPosition, componentPosition);
    }

    @Override
    public int getElementPosition() {
        return elementPosition;
    }

    @Override
    public int getComponentPosition() {
        return componentPosition > 0 ? componentPosition : -1;
    }

}
