package io.xlate.edi.internal.schema;

import java.util.Objects;

import io.xlate.edi.schema.EDIElementPosition;

public class ElementPosition implements EDIElementPosition {

    private static final String TOSTRING_FORMAT = "%d.%d";

    final int elementIndex;
    final int componentIndex;

    public ElementPosition(int elementPosition, int componentPosition) {
        super();
        this.elementIndex = elementPosition;
        this.componentIndex = componentPosition;
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

        return Objects.equals(elementIndex, other.elementIndex) &&
                Objects.equals(componentIndex, other.componentIndex);
    }

    @Override
    public int hashCode() {
        return Objects.hash(elementIndex, componentIndex);
    }

    @Override
    public String toString() {
        return String.format(TOSTRING_FORMAT, elementIndex, componentIndex);
    }

    @Override
    public int getElementPosition() {
        return elementIndex;
    }

    @Override
    public int getComponentPosition() {
        return componentIndex > 0 ? componentIndex : -1;
    }

}
