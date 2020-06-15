package io.xlate.edi.internal.schema;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;

import org.junit.jupiter.api.Test;

import io.xlate.edi.schema.EDISimpleType.Base;

class ElementTest {

    @Test
    void testElementContructorNoValues() {
        ElementType e = new ElementType("E1", Base.STRING, "1", 1, 0L, 5L, Collections.emptySet(), Collections.emptyList());
        assertEquals("id: E1, type: ELEMENT, base: STRING, code: 1, minLength: 0, maxLength: 5, values: []", e.toString());
    }

    @Test
    void testElementContructorWithValues() {
        ElementType e = new ElementType("E1", Base.STRING, "1", 1, 0L, 5L, new HashSet<>(Arrays.asList("ABCDE", "FGHIJ")), Collections.emptyList());
        assertEquals("id: E1, type: ELEMENT, base: STRING, code: 1, minLength: 0, maxLength: 5, values: [ABCDE, FGHIJ]", e.toString());
    }

}
