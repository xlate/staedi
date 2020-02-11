package io.xlate.edi.internal.schema.implementation;

import java.util.List;

import io.xlate.edi.schema.EDIComplexType;
import io.xlate.edi.schema.implementation.Discriminator;
import io.xlate.edi.schema.implementation.EDITypeImplementation;
import io.xlate.edi.schema.implementation.LoopImplementation;

public class LoopImpl implements LoopImplementation {

    private final EDIComplexType standard;
    private final int minOccurs;
    private final int maxOccurs;
    private final String id;
    private final Discriminator discriminator;
    private final List<EDITypeImplementation<EDIComplexType>> sequence;

    public LoopImpl(EDIComplexType standard,
            int minOccurs,
            int maxOccurs,
            String id,
            Discriminator discriminator,
            List<EDITypeImplementation<EDIComplexType>> sequence) {
        super();
        this.standard = standard;
        this.minOccurs = minOccurs;
        this.maxOccurs = maxOccurs;
        this.id = id;
        this.discriminator = discriminator;
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
    public Discriminator getDiscriminator() {
        return discriminator;
    }

    @Override
    public List<EDITypeImplementation<EDIComplexType>> getSequence() {
        return sequence;
    }

}
