package io.xlate.edi.internal.schema.implementation;

import java.util.Set;

import io.xlate.edi.schema.EDISimpleType;
import io.xlate.edi.schema.implementation.ElementImplementation;

public class ElementImpl implements ElementImplementation {

    private final EDISimpleType standard;
    private final int minOccurs;
    private final int maxOccurs;
    private final String id;
    private final Set<String> values;

    public ElementImpl(EDISimpleType standard, int minOccurs, int maxOccurs, String id, Set<String> values) {
        super();
        this.standard = standard;
        this.minOccurs = minOccurs;
        this.maxOccurs = maxOccurs;
        this.id = id;
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

}
