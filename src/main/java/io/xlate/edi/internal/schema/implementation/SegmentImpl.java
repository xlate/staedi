package io.xlate.edi.internal.schema.implementation;

import java.util.ArrayList;
import java.util.List;

import io.xlate.edi.schema.EDIComplexType;
import io.xlate.edi.schema.EDIType;
import io.xlate.edi.schema.implementation.Discriminator;
import io.xlate.edi.schema.implementation.EDITypeImplementation;
import io.xlate.edi.schema.implementation.SegmentImplementation;

public class SegmentImpl implements SegmentImplementation {

    private final EDIComplexType standard;
    private final int minOccurs;
    private final int maxOccurs;
    private final String id;
    private final Discriminator discriminator;
    private final List<EDITypeImplementation<EDIType>> sequence;

    @SuppressWarnings("unchecked")
    public SegmentImpl(EDIComplexType standard,
            int minOccurs,
            int maxOccurs,
            String id,
            Discriminator discriminator,
            List<EDITypeImplementation<? extends EDIType>> sequence) {
        super();
        this.standard = standard;
        this.minOccurs = minOccurs;
        this.maxOccurs = maxOccurs;
        this.id = id;
        this.discriminator = discriminator;
        this.sequence = new ArrayList<>(sequence.size());
        sequence.forEach(e -> this.sequence.add((EDITypeImplementation<EDIType>) e));
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
    public Discriminator getDiscriminator() {
        return discriminator;
    }

    @Override
    public List<EDITypeImplementation<EDIType>> getSequence() {
        return sequence;
    }

}
