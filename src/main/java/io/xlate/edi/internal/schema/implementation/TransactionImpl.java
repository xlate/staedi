package io.xlate.edi.internal.schema.implementation;

import java.util.List;

import io.xlate.edi.schema.implementation.EDITypeImplementation;

public class TransactionImpl extends LoopImpl {

    public TransactionImpl(String id,
            String typeId,
            List<EDITypeImplementation> sequence) {
        super(0, 0, id, typeId, null, sequence);
    }

    @Override
    public Type getType() {
        return Type.TRANSACTION;
    }
}
