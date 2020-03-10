package io.xlate.edi.internal.stream.validation;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;

import io.xlate.edi.internal.stream.tokenization.ValidationEventHandler;
import io.xlate.edi.schema.EDIReference;
import io.xlate.edi.schema.EDISyntaxRule;
import io.xlate.edi.schema.EDIType;

abstract class SyntaxValidatorTestBase {

    protected EDISyntaxRule syntax;
    protected ValidationEventHandler handler;
    protected UsageNode structure;
    protected EDIReference structureRef;
    protected EDIType structureType;

    @BeforeEach
    public void setUp() {
        syntax = mock(EDISyntaxRule.class);
        when(syntax.getType()).thenReturn(EDISyntaxRule.Type.CONDITIONAL);

        handler = mock(ValidationEventHandler.class);

        structureType = mock(EDIType.class);
        when(structureType.getType()).thenReturn(EDIType.Type.SEGMENT);

        structureRef = mock(EDIReference.class);
        when(structureRef.getReferencedType()).thenReturn(structureType);

        structure = mock(UsageNode.class);
        when(structure.getReferencedType()).thenReturn(structureType);
    }

    protected UsageNode mockUsageNode(String referenceCode, boolean used, int index) {
        UsageNode node = mock(UsageNode.class);
        when(node.getCode()).thenReturn(referenceCode);
        when(node.isUsed()).thenReturn(used);
        when(node.getParent()).thenReturn(structure);
        when(node.getIndex()).thenReturn(index);
        return node;
    }

    protected UsageNode mockUsageNode(boolean used, int index) {
        return mockUsageNode(null, used, index);
    }
}
