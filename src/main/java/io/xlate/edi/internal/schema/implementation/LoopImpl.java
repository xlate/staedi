package io.xlate.edi.internal.schema.implementation;

import java.util.List;
import java.util.Objects;

import io.xlate.edi.schema.EDIReference;
import io.xlate.edi.schema.implementation.Discriminator;
import io.xlate.edi.schema.implementation.EDITypeImplementation;
import io.xlate.edi.schema.implementation.LoopImplementation;

public class LoopImpl extends BaseComplexImpl implements LoopImplementation {

    private static final String TOSTRING_FORMAT = "id: %s, minOccurs: %d, maxOccurs: %d, discriminator: { %s }, sequence { %s }, standard: { %s }";
    private final String code;
    private final Discriminator discriminator;

    @SuppressWarnings("java:S107")
    public LoopImpl(int minOccurs,
            int maxOccurs,
            String code,
            String typeId,
            Discriminator discriminator,
            List<EDITypeImplementation> sequence,
            String title,
            String description) {
        super(sequence, title, description);
        this.minOccurs = minOccurs;
        this.maxOccurs = maxOccurs;
        this.code = code;
        this.typeId = typeId;
        this.discriminator = discriminator;
    }

    public LoopImpl(EDIReference standardReference, List<EDITypeImplementation> sequence) {
        super(sequence, null, null);
        this.setStandardReference(standardReference);
        this.code = standard.getCode();
        this.discriminator = null;
    }

    @Override
    public boolean equals(Object o) {
        return super.equals(o) &&
                Objects.equals(code, ((LoopImpl) o).code) &&
                Objects.equals(discriminator, ((LoopImpl) o).discriminator);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), code, discriminator);
    }

    @Override
    public String toString() {
        return String.format(TOSTRING_FORMAT, code, minOccurs, maxOccurs, discriminator, sequence, standard);
    }

    @Override
    public String getId() {
        return getCode();
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
