package io.xlate.edi.internal.schema.implementation;

import java.util.List;
import java.util.Objects;

import io.xlate.edi.schema.EDIReference;
import io.xlate.edi.schema.implementation.CompositeImplementation;
import io.xlate.edi.schema.implementation.EDITypeImplementation;

public class CompositeImpl extends BaseComplexImpl implements CompositeImplementation, Positioned {

    private static final String TOSTRING_FORMAT = "typeId: %s, minOccurs: %d, maxOccurs: %d, position: %d, sequence { %s }, standard: { %s }";
    private final int position;

    public CompositeImpl(int minOccurs,
            int maxOccurs,
            String typeId,
            int position,
            List<EDITypeImplementation> sequence,
            String title,
            String description) {

        super(sequence, title, description);
        super.minOccurs = minOccurs;
        super.maxOccurs = maxOccurs;
        super.typeId = typeId;
        this.position = position;
    }

    public CompositeImpl(EDIReference standardReference, int position, List<EDITypeImplementation> sequence) {
        super(sequence, null, null);
        this.setStandardReference(standardReference);
        this.position = position;
    }

    @Override
    public boolean equals(Object o) {
        return super.equals(o) &&
                Objects.equals(position, ((CompositeImpl) o).position);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), position);
    }

    @Override
    public String toString() {
        return String.format(TOSTRING_FORMAT, typeId, minOccurs, maxOccurs, position, sequence, standard);
    }

    @Override
    public int getPosition() {
        return position;
    }
}
