package io.xlate.edi.internal.schema.implementation;

import java.util.List;

import io.xlate.edi.schema.implementation.CompositeImplementation;
import io.xlate.edi.schema.implementation.EDITypeImplementation;

public class CompositeImpl extends BaseComplexImpl implements CompositeImplementation, Positioned {

    private static final String TOSTRING_FORMAT = "typeId: %s, minOccurs: %d, maxOccurs: %d, position: %d, standard: { %s }";
    private final int position;

    public CompositeImpl(int minOccurs,
            int maxOccurs,
            String typeId,
            int position,
            List<EDITypeImplementation> sequence) {
        super(sequence);
        this.minOccurs = minOccurs;
        this.maxOccurs = maxOccurs;
        this.typeId = typeId;
        this.position = position;
    }

    @Override
    public String toString() {
        return String.format(TOSTRING_FORMAT, typeId, minOccurs, maxOccurs, position, standard);
    }

    @Override
    public int getPosition() {
        return position;
    }
}
