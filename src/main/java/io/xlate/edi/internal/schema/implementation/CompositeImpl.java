package io.xlate.edi.internal.schema.implementation;

import java.util.List;

import io.xlate.edi.schema.EDIComplexType;
import io.xlate.edi.schema.EDISimpleType;
import io.xlate.edi.schema.implementation.CompositeImplementation;
import io.xlate.edi.schema.implementation.EDITypeImplementation;

public class CompositeImpl implements CompositeImplementation {

    private final EDIComplexType standard;
    private final int minOccurs;
    private final int maxOccurs;
    private final String id;
    private final List<EDITypeImplementation<EDISimpleType>> sequence;

    public CompositeImpl(EDIComplexType standard,
            int minOccurs,
            int maxOccurs,
            String id,
            List<EDITypeImplementation<EDISimpleType>> sequence) {
        super();
        this.standard = standard;
        this.minOccurs = minOccurs;
        this.maxOccurs = maxOccurs;
        this.id = id;
        this.sequence = sequence;
    }

    @Override
    public EDIComplexType getStandard() {
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
    public List<EDITypeImplementation<EDISimpleType>> getSequence() {
        return sequence;
    }

}
