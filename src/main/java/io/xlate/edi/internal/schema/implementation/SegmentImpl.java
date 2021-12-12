package io.xlate.edi.internal.schema.implementation;

import java.util.List;
import java.util.Objects;

import io.xlate.edi.schema.EDIReference;
import io.xlate.edi.schema.implementation.Discriminator;
import io.xlate.edi.schema.implementation.EDITypeImplementation;
import io.xlate.edi.schema.implementation.SegmentImplementation;

public class SegmentImpl extends BaseComplexImpl implements SegmentImplementation {

    private static final String TOSTRING_FORMAT = "typeId: %s, code: %s, minOccurs: %d, maxOccurs: %d, discriminator: { %s }, sequence { %s }, standard: { %s }";
    private final String code;
    private final Discriminator discriminator;

    @SuppressWarnings("java:S107")
    public SegmentImpl(int minOccurs,
            int maxOccurs,
            String typeId,
            String code,
            Discriminator discriminator,
            List<EDITypeImplementation> sequence,
            String title,
            String description) {
        super(sequence, title, description);
        super.minOccurs = minOccurs;
        super.maxOccurs = maxOccurs;
        super.typeId = typeId;
        this.code = code;
        this.discriminator = discriminator;
    }

    public SegmentImpl(EDIReference standardReference, List<EDITypeImplementation> sequence) {
        super(sequence, null, null);
        this.setStandardReference(standardReference);
        this.code = standard.getCode();
        this.discriminator = null;
    }

    @Override
    public boolean equals(Object o) {
        return super.equals(o) &&
                Objects.equals(code, ((SegmentImpl) o).code) &&
                Objects.equals(discriminator, ((SegmentImpl) o).discriminator);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), code, discriminator);
    }

    @Override
    public String toString() {
        return String.format(TOSTRING_FORMAT, typeId, code, minOccurs, maxOccurs, discriminator, sequence, standard);
    }

    @Override
    public String getCode() {
        return code;
    }

    @Override
    public Discriminator getDiscriminator() {
        return discriminator;
    }
}
