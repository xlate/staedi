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
        Element e = new Element("E1", Base.STRING, 1, 0L, 5L, Collections.emptySet());
        assertEquals("id: E1, type: ELEMENT, base: STRING, number: 1, minLength: 0, maxLength: 5, values: []", e.toString());
    }

    @Test
    void testElementContructorWithValues() {
        Element e = new Element("E1", Base.STRING, 1, 0L, 5L, new HashSet<>(Arrays.asList("ABCDE", "FGHIJ")));
        assertEquals("id: E1, type: ELEMENT, base: STRING, number: 1, minLength: 0, maxLength: 5, values: [ABCDE, FGHIJ]", e.toString());
    }

}
