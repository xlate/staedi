package io.xlate.edi.internal.schema.implementation;

import java.util.Set;

import io.xlate.edi.schema.EDISimpleType;
import io.xlate.edi.schema.EDIType;
import io.xlate.edi.schema.implementation.ElementImplementation;

public class ElementImpl implements ElementImplementation, Positioned {

    private static final String TOSTRING_FORMAT = "id: %s, minOccurs: %d, maxOccurs: %d, position: %d, values: %s, standard: { %s }";
    private final int minOccurs;
    private final int maxOccurs;
    private final String id;
    private final int position;
    private final Set<String> values;

    private EDISimpleType standard;

    public ElementImpl(int minOccurs,
            int maxOccurs,
            String id,
            int position,
            Set<String> values) {
        this.minOccurs = minOccurs;
        this.maxOccurs = maxOccurs;
        this.id = id;
        this.position = position;
        this.values = values;
    }

    @Override
    public String toString() {
        return String.format(TOSTRING_FORMAT, id, minOccurs, maxOccurs, position, values, standard);
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
    public Set<String> getValueSet() {
        return values;
    }

    @Override
    public int getPosition() {
        return position;
    }

    public void setStandard(EDISimpleType standard) {
        this.standard = standard;
    }
}
