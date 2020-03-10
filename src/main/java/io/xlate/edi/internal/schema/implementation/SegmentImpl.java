package io.xlate.edi.internal.schema.implementation;

import java.util.List;

import io.xlate.edi.schema.EDIReference;
import io.xlate.edi.schema.implementation.Discriminator;
import io.xlate.edi.schema.implementation.EDITypeImplementation;
import io.xlate.edi.schema.implementation.SegmentImplementation;

public class SegmentImpl extends BaseComplexImpl implements SegmentImplementation {

    private static final String TOSTRING_FORMAT = "typeId: %s, minOccurs: %d, maxOccurs: %d, discriminator: { %s }, standard: { %s }";
    private final Discriminator discriminator;

    public SegmentImpl(int minOccurs,
            int maxOccurs,
            String typeId,
            Discriminator discriminator,
            List<EDITypeImplementation> sequence,
            String title,
            String description) {
        super(sequence, title, description);
        this.minOccurs = minOccurs;
        this.maxOccurs = maxOccurs;
        this.typeId = typeId;
        this.discriminator = discriminator;
    }

    public SegmentImpl(EDIReference standardReference, Discriminator discriminator, List<EDITypeImplementation> sequence) {
        super(sequence, null, null);
        this.setStandardReference(standardReference);
        this.typeId = standard.getId();
        this.discriminator = discriminator;
    }

    @Override
    public String toString() {
        return String.format(TOSTRING_FORMAT, typeId, minOccurs, maxOccurs, discriminator, standard);
    }

    @Override
    public Discriminator getDiscriminator() {
        return discriminator;
    }
}
