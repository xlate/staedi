package io.xlate.edi.internal.schema;

import static org.junit.Assert.*;

import java.util.Arrays;

import org.junit.Test;

import io.xlate.edi.internal.schema.SyntaxRestriction;
import io.xlate.edi.schema.EDISyntaxRule;

public class SyntaxRestrictionTest {

    @Test(expected = IllegalArgumentException.class)
    public void testConstructorEmptyList() {
        EDISyntaxRule rule = new SyntaxRestriction(EDISyntaxRule.Type.PAIRED, Arrays.asList());
        fail("Exception was not thrown, " + rule);
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
