package io.xlate.edi.internal.schema.implementation;

import java.util.Objects;
import java.util.Set;

import io.xlate.edi.schema.implementation.Discriminator;

public class DiscriminatorImpl implements Discriminator {

    private static final String TOSTRING_FORMAT = "position: [%d, %d], values: %s";
    private final int elementPosition;
    private final int componentPosition;
    private final Set<String> valueSet;

    public DiscriminatorImpl(int elementPosition, int componentPosition, Set<String> valueSet) {
        super();
        this.elementPosition = elementPosition;
        this.componentPosition = componentPosition;
        this.valueSet = valueSet;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (!(o instanceof DiscriminatorImpl)) {
            return false;
        }

        DiscriminatorImpl other = (DiscriminatorImpl) o;

        return Objects.equals(elementPosition, other.elementPosition) &&
                Objects.equals(componentPosition, other.componentPosition) &&
                Objects.equals(valueSet, other.valueSet);
    }

    @Override
    public int hashCode() {
        return Objects.hash(elementPosition, componentPosition, valueSet);
    }

    @Override
    public String toString() {
        return String.format(TOSTRING_FORMAT, elementPosition, componentPosition, valueSet);
    }

    @Override
    public int getElementPosition() {
        return elementPosition;
    }

    @Override
    public int getComponentPosition() {
        return componentPosition;
    }

    @Override
    public Set<String> getValueSet() {
        return valueSet;
    }

}
