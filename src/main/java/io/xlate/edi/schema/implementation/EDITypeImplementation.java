package io.xlate.edi.schema.implementation;

import io.xlate.edi.schema.EDIType;

public interface EDITypeImplementation<T extends EDIType> extends EDIType {

    T getStandard();

    int getMinOccurs();

    int getMaxOccurs();

}
