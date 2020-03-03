package io.xlate.edi.internal.schema.implementation;

import java.util.Set;

import io.xlate.edi.schema.EDISimpleType;
import io.xlate.edi.schema.implementation.ElementImplementation;

public class ElementImpl extends BaseImpl<EDISimpleType> implements ElementImplementation, Positioned {

    private static final String TOSTRING_FORMAT = "typeId: %s, minOccurs: %d, maxOccurs: %d, position: %d, values: %s, standard: { %s }";
    private final int position;
    private final Set<String> values;

    public ElementImpl(int minOccurs,
            int maxOccurs,
            String typeId,
            int position,
            Set<String> values,
            String title,
            String description) {
        super(title, description);
        this.minOccurs = minOccurs;
        this.maxOccurs = maxOccurs;
        this.typeId = typeId;
        this.position = position;
        this.values = values;
    }

    @Override
    public String toString() {
        return String.format(TOSTRING_FORMAT, typeId, minOccurs, maxOccurs, position, values, standard);
    }

    @Override
    public Set<String> getValueSet() {
        return values;
    }

    @Override
    public int getPosition() {
        return position;
    }
}
