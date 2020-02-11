package io.xlate.edi.internal.schema.implementation;

import java.util.Set;

import io.xlate.edi.schema.implementation.Discriminator;

public class DiscriminatorImpl implements Discriminator {

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
