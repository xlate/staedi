package io.xlate.edi.internal.stream.validation;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import io.xlate.edi.internal.stream.tokenization.Dialect;
import io.xlate.edi.schema.EDISimpleType;
import io.xlate.edi.stream.EDIStreamValidationError;

interface ValueSetTester {

    default Set<String> setOf() {
        return Collections.emptySet();
    }

    default Set<String> setOf(String... values) {
        return new HashSet<>(Arrays.asList(values));
    }

    default Map<String, String> mapOf(String... values) {
        Map<String, String> map = new HashMap<>(values.length / 2);
        for (int i = 0; i < values.length; i+=2) {
            map.put(values[i], values[i + 1]);
        }
        return map;
    }

    default void assertHasError(ElementValidator v, Dialect dialect, EDISimpleType element, CharSequence value, EDIStreamValidationError expected) {
        List<EDIStreamValidationError> errors = new ArrayList<>();
        v.validate(dialect, element, value, errors);
        assertTrue(errors.contains(expected));
    }
}
