package io.xlate.edi.internal.schema.implementation;

import java.util.List;

import io.xlate.edi.schema.EDIComplexType;
import io.xlate.edi.schema.EDIType;
import io.xlate.edi.schema.implementation.CompositeImplementation;
import io.xlate.edi.schema.implementation.EDITypeImplementation;

public class CompositeImpl implements CompositeImplementation, Positioned {

    private final int minOccurs;
    private final int maxOccurs;
    private final String id;
    private final int position;
    private final List<EDITypeImplementation> sequence;

    private EDIComplexType standard;

    public CompositeImpl(int minOccurs,
            int maxOccurs,
            String id,
            int position,
            List<EDITypeImplementation> sequence) {
        this.minOccurs = minOccurs;
        this.maxOccurs = maxOccurs;
        this.id = id;
        this.position = position;
        this.sequence = sequence;
    }

    @Override
    public EDIType getReferencedType() {
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
    public List<EDITypeImplementation> getSequence() {
        return sequence;
    }

    @Override
    public int getPosition() {
        return position;
    }

    public void setStandard(EDIComplexType standard) {
        this.standard = standard;
    }
}
