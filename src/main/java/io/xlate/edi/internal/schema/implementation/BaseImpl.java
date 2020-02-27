package io.xlate.edi.internal.schema.implementation;

import io.xlate.edi.schema.EDIReference;
import io.xlate.edi.schema.EDIType;
import io.xlate.edi.schema.implementation.EDITypeImplementation;

public abstract class BaseImpl<T extends EDIType> implements EDITypeImplementation {

    protected String typeId;
    protected T standard;
    protected int minOccurs;
    protected int maxOccurs;

    @Override
    public String getId() {
        return typeId;
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

    public String getTypeId() {
        return typeId;
    }

    public T getStandard() {
        return standard;
    }

    @SuppressWarnings("unchecked")
    public void setStandardReference(EDIReference reference) {
        this.standard = (T) reference.getReferencedType();

        if (this.typeId == null) {
            this.typeId = standard.getId();
        }

        if (this.minOccurs < 0) {
            this.minOccurs = reference.getMinOccurs();
        }

        if (this.maxOccurs < 0) {
            this.maxOccurs = reference.getMaxOccurs();
        }
    }

}
