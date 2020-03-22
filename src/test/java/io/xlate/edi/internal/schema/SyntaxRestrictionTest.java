package io.xlate.edi.internal.schema;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Arrays;

import org.junit.jupiter.api.Test;

import io.xlate.edi.schema.EDISyntaxRule;

public class SyntaxRestrictionTest {

    @Test
    public void testConstructorEmptyList() {
        assertThrows(IllegalArgumentException.class, () -> new SyntaxRestriction(EDISyntaxRule.Type.PAIRED, Arrays.asList()));
    }

    @Test
    public void testToString() {
        EDISyntaxRule rule = new SyntaxRestriction(EDISyntaxRule.Type.PAIRED, Arrays.asList(1, 2));
        assertEquals("type: PAIRED, positions: [1, 2]", rule.toString());
    }

    @Test
    public void testGetType() {
        EDISyntaxRule rule = new SyntaxRestriction(EDISyntaxRule.Type.PAIRED, Arrays.asList(1, 2));
        assertEquals(EDISyntaxRule.Type.PAIRED, rule.getType());
    }

    @Test
    public void testGetPositions() {
        EDISyntaxRule rule = new SyntaxRestriction(EDISyntaxRule.Type.PAIRED, Arrays.asList(1, 2));
        assertEquals(Arrays.asList(1, 2), rule.getPositions());
    }

}
