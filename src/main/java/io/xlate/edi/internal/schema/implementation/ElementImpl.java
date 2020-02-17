package io.xlate.edi.internal.schema.implementation;

import java.util.Set;

import io.xlate.edi.schema.EDISimpleType;
import io.xlate.edi.schema.implementation.ElementImplementation;

public class ElementImpl implements ElementImplementation, Positioned {

    private final int minOccurs;
    private final int maxOccurs;
    private final String id;
    private final int position;
    private final Set<String> values;

    private EDISimpleType standard;

    public ElementImpl(int minOccurs,
            int maxOccurs,
            String id,
            int position,
            Set<String> values) {
        this.minOccurs = minOccurs;
        this.maxOccurs = maxOccurs;
        this.id = id;
        this.position = position;
        this.values = values;
    }

    @Override
    public EDISimpleType getStandard() {
        return standard;
    }

    @Override
    public int getMinOccurs() {
        return minOccurs;
    }

    @Override
    public int getMaxOccurs() {
        return maxOccurs;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public Set<String> getValueSet() {
        return values;
    }

    @Override
    public int getPosition() {
        return position;
    }

    public void setStandard(EDISimpleType standard) {
        this.standard = standard;
    }
}
