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
    void setUp() {
        syntax = mock(EDISyntaxRule.class);
        handler = mock(ValidationEventHandler.class);

        structureType = mock(EDIType.class);
        when(structureType.getType()).thenReturn(EDIType.Type.SEGMENT);

        structureRef = mock(EDIReference.class);
        when(structureRef.getReferencedType()).thenReturn(structureType);

        structure = mock(UsageNode.class);
        when(structure.isNodeType(EDIType.Type.SEGMENT, EDIType.Type.COMPOSITE)).thenReturn(true);
        when(structure.isNodeType(EDIType.Type.SEGMENT)).thenReturn(true);
    }

    protected UsageNode mockUsageNode(String referenceCode, boolean used, int index) {
        UsageNode node = mock(UsageNode.class);
        EDIReference typeReference = mock(EDIReference.class);
        EDIType type = mock(EDIType.class);

        when(type.getCode()).thenReturn(referenceCode);
        when(typeReference.getReferencedType()).thenReturn(type);
        when(node.getLink()).thenReturn(typeReference);
        when(node.isUsed()).thenReturn(used);
        when(node.getParent()).thenReturn(structure);
        when(node.getIndex()).thenReturn(index);

        return node;
    }

    protected UsageNode mockUsageNode(boolean used, int index) {
        return mockUsageNode(null, used, index);
    }
}
