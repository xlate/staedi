package io.xlate.edi.schema.implementation;

import java.util.Set;

import io.xlate.edi.schema.EDIElementPosition;

public interface Discriminator extends EDIElementPosition {

    Set<String> getValueSet();

}
