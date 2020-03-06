package io.xlate.edi.internal.schema.implementation;

import java.util.List;

import io.xlate.edi.schema.implementation.Discriminator;
import io.xlate.edi.schema.implementation.EDITypeImplementation;
import io.xlate.edi.schema.implementation.LoopImplementation;

public class LoopImpl extends BaseComplexImpl implements LoopImplementation {

    private static final String TOSTRING_FORMAT = "id: %s, minOccurs: %d, maxOccurs: %d, discriminator: { %s }, standard: { %s }";
    private final String id;
    private final Discriminator discriminator;

    public LoopImpl(int minOccurs,
            int maxOccurs,
            String id,
            String typeId,
            Discriminator discriminator,
            List<EDITypeImplementation> sequence,
            String title,
            String description) {
        super(sequence, title, description);
        this.minOccurs = minOccurs;
        this.maxOccurs = maxOccurs;
        this.id = id;
        this.typeId = typeId;
        this.discriminator = discriminator;
    }

    @Override
    public String toString() {
        return String.format(TOSTRING_FORMAT, id, minOccurs, maxOccurs, discriminator, standard);
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public Discriminator getDiscriminator() {
        return discriminator;
    }

}
