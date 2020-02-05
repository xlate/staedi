package io.xlate.edi.schema.implementation;

import java.util.Set;

public interface Discriminator {

    int getElementPosition();

    int getComponentPosition();

    Set<String> getValueSet();

}
