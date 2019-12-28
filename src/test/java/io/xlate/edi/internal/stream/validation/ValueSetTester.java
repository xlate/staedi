package io.xlate.edi.internal.stream.validation;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

interface ValueSetTester {

    default Set<String> setOf() {
        return Collections.emptySet();
    }

    default Set<String> setOf(String... values) {
        return new HashSet<>(Arrays.asList(values));
    }
}
