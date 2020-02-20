package io.xlate.edi.internal.schema.implementation;

import java.util.List;

import io.xlate.edi.schema.EDIComplexType;
import io.xlate.edi.schema.EDIType;
import io.xlate.edi.schema.implementation.Discriminator;
import io.xlate.edi.schema.implementation.EDITypeImplementation;
import io.xlate.edi.schema.implementation.LoopImplementation;

public class LoopImpl implements LoopImplementation {

    private final int minOccurs;
    private final int maxOccurs;
    private final String id;
    private final String typeId;
    private final Discriminator discriminator;
    private final List<EDITypeImplementation> sequence;

    private EDIComplexType standard;

    public LoopImpl(int minOccurs,
            int maxOccurs,
            String id,
            String typeId,
            Discriminator discriminator,
            List<EDITypeImplementation> sequence) {
        this.minOccurs = minOccurs;
        this.maxOccurs = maxOccurs;
        this.id = id;
        this.typeId = typeId;
        this.discriminator = discriminator;
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
    public Discriminator getDiscriminator() {
        return discriminator;
    }

    @Override
    public List<EDITypeImplementation> getSequence() {
        return sequence;
    }

    public String getTypeId() {
        return typeId;
    }

    public void setStandard(EDIComplexType standard) {
        this.standard = standard;
    }
}
