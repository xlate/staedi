package io.xlate.edi.internal.stream.validation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.stubbing.Answer;

import io.xlate.edi.schema.EDIReference;
import io.xlate.edi.schema.EDISyntaxRule.Type;
import io.xlate.edi.schema.EDIType;
import io.xlate.edi.stream.EDIStreamEvent;
import io.xlate.edi.stream.EDIStreamValidationError;

class ListSyntaxValidatorTest extends SyntaxValidatorTestBase {

    ListSyntaxValidator validator;

    @BeforeEach
    void setUp() {
        validator = (ListSyntaxValidator) SyntaxValidator.getInstance(Type.LIST);
        super.setUp();
    }

    @Test
    void testValidateListConditionalAllUsed() {
        when(syntax.getPositions()).thenReturn(Arrays.asList(1, 3, 4));
        List<UsageNode> children = Arrays.asList(mockUsageNode(true, 1),
                                                 mockUsageNode(false, 2),
                                                 mockUsageNode(true, 3),
                                                 mockUsageNode(true, 4));
        when(structure.getChildren()).thenReturn(children);
        final AtomicInteger count = new AtomicInteger(0);

        doAnswer((Answer<Void>) invocation -> {
            count.incrementAndGet();
            return null;
        }).when(handler)
          .elementError(eq(EDIStreamEvent.ELEMENT_OCCURRENCE_ERROR),
                        eq(EDIStreamValidationError.CONDITIONAL_REQUIRED_DATA_ELEMENT_MISSING),
                        nullable(EDIReference.class),
                        nullable(CharSequence.class),
                        any(Integer.class),
                        any(Integer.class),
                        any(Integer.class));

        validator.validate(syntax, structure, handler);
        assertEquals(0, count.get());
    }

    @Test
    void testValidateListConditionalAnchorUnused() {
        when(syntax.getPositions()).thenReturn(Arrays.asList(1, 3, 4));
        List<UsageNode> children = Arrays.asList(mockUsageNode(false, 1),
                                                 mockUsageNode(false, 2),
                                                 mockUsageNode(true, 3));
        when(structure.getChildren()).thenReturn(children);
        final AtomicInteger count = new AtomicInteger(0);

        doAnswer((Answer<Void>) invocation -> {
            count.incrementAndGet();
            return null;
        }).when(handler)
          .elementError(eq(EDIStreamEvent.ELEMENT_OCCURRENCE_ERROR),
                        eq(EDIStreamValidationError.CONDITIONAL_REQUIRED_DATA_ELEMENT_MISSING),
                        nullable(EDIReference.class),
                        nullable(CharSequence.class),
                        any(Integer.class),
                        any(Integer.class),
                        any(Integer.class));

        validator.validate(syntax, structure, handler);
        assertEquals(0, count.get());
    }

    @Test
    void testValidateListConditionalMissingRequired() {
        when(syntax.getPositions()).thenReturn(Arrays.asList(1, 3, 4));
        List<UsageNode> children = Arrays.asList(mockUsageNode(true, 1),
                                                 mockUsageNode(false, 2),
                                                 mockUsageNode(false, 3)/*,
                                                                        mockUsageNode(false, 4)*/);
        when(structure.getChildren()).thenReturn(children);
        final AtomicInteger count = new AtomicInteger(0);

        doAnswer((Answer<Void>) invocation -> {
            count.incrementAndGet();
            return null;
        }).when(handler)
          .elementError(eq(EDIStreamEvent.ELEMENT_OCCURRENCE_ERROR),
                        eq(EDIStreamValidationError.CONDITIONAL_REQUIRED_DATA_ELEMENT_MISSING),
                        nullable(EDIReference.class),
                        nullable(CharSequence.class),
                        any(Integer.class),
                        any(Integer.class),
                        any(Integer.class));

        validator.validate(syntax, structure, handler);
        assertEquals(2, count.get()); // Error for both positions 3 and 4
    }


    @Test
    void testValidateListConditionalMissingRequired_Composite() {
        when(structure.isNodeType(EDIType.Type.COMPOSITE)).thenReturn(true);

        UsageNode parent = mock(UsageNode.class);
        when(structure.getParent()).thenReturn(parent);
        when(parent.getIndex()).thenReturn(0); // composite is the first element in the parent segment

        when(syntax.getPositions()).thenReturn(Arrays.asList(1, 3, 4));

        List<UsageNode> children = Arrays.asList(mockUsageNode(true, 1),
                                                 mockUsageNode(false, 2),
                                                 mockUsageNode(false, 3)/*,
                                                                        mockUsageNode(false, 4)*/);
        when(structure.getChildren()).thenReturn(children);
        final AtomicInteger count = new AtomicInteger(0);
        final AtomicInteger element = new AtomicInteger(0);

        doAnswer((Answer<Void>) invocation -> {
            count.incrementAndGet();
            element.set(invocation.getArgument(4));
            return null;
        }).when(handler)
          .elementError(eq(EDIStreamEvent.ELEMENT_OCCURRENCE_ERROR),
                        eq(EDIStreamValidationError.CONDITIONAL_REQUIRED_DATA_ELEMENT_MISSING),
                        nullable(EDIReference.class),
                        nullable(CharSequence.class),
                        any(Integer.class),
                        any(Integer.class),
                        any(Integer.class));

        validator.validate(syntax, structure, handler);
        assertEquals(1, element.get());
        assertEquals(2, count.get()); // Error for both positions 3 and 4
    }
}
