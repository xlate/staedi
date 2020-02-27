package io.xlate.edi.internal.schema.implementation;

import java.util.List;

import io.xlate.edi.schema.EDIComplexType;
import io.xlate.edi.schema.implementation.EDITypeImplementation;

public abstract class BaseComplexImpl extends BaseImpl<EDIComplexType> {

    private final List<EDITypeImplementation> sequence;

    public BaseComplexImpl(List<EDITypeImplementation> sequence) {
        this.sequence = sequence;
    }

    public List<EDITypeImplementation> getSequence() {
        return sequence;
    }
}
