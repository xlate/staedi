package io.xlate.edi.internal.schema.implementation;

import java.util.Objects;
import java.util.Set;

import io.xlate.edi.schema.EDIElementPosition;
import io.xlate.edi.schema.implementation.Discriminator;

public class DiscriminatorImpl implements Discriminator {

    private static final String TOSTRING_FORMAT = "position: %s, values: %s";
    private final EDIElementPosition position;
    private final Set<String> valueSet;

    public DiscriminatorImpl(EDIElementPosition position, Set<String> valueSet) {
        this.position = position;
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
        return Objects.equals(position, other.position) && 
                Objects.equals(valueSet, other.valueSet);
    }

    @Override
    public int hashCode() {
        return  Objects.hash(position, valueSet);
    }

    @Override
    public String toString() {
        return String.format(TOSTRING_FORMAT, position, valueSet);
    }

    @Override
    public int getElementPosition() {
        return position.getElementPosition();
    }

    @Override
    public int getComponentPosition() {
        return position.getComponentPosition();
    }

    @Override
    public Set<String> getValueSet() {
        return valueSet;
    }

}
