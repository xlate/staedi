package io.xlate.edi.internal.schema;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;

import org.junit.jupiter.api.Test;

import io.xlate.edi.schema.EDIReference;
import io.xlate.edi.schema.EDISyntaxRule;
import io.xlate.edi.schema.EDIType.Type;

class StructureTest {

    @Test
    void testStructureConstruction() {
        EDIReference ref = new Reference("E1", "element", 1, 1);
        EDISyntaxRule rule = new SyntaxRestriction(EDISyntaxRule.Type.EXCLUSION, Arrays.asList(1, 8));
        Structure s = new Structure("SEG", Type.SEGMENT, "SEG", Arrays.asList(ref), Arrays.asList(rule));
        assertEquals("id: SEG, type: SEGMENT, code: SEG, references: [{refId: E1, minOccurs: 1, maxOccurs: 1}], syntaxRestrictions: [{type: EXCLUSION, positions: [1, 8]}]", s.toString());
    }

}
