package io.xlate.edi.stream.validation;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Before;
import org.junit.Test;
import org.mockito.stubbing.Answer;

import io.xlate.edi.schema.EDISyntaxRule;
import io.xlate.edi.stream.EDIStreamEvent;
import io.xlate.edi.stream.EDIStreamValidationError;
import io.xlate.edi.stream.Location;
import io.xlate.edi.stream.internal.EventHandler;

public class ListSyntaxValidatorTest {

    ListSyntaxValidator validator;

    EDISyntaxRule syntax;
    Location location;
    EventHandler handler;
    UsageNode used;
    UsageNode unused;

    @Before
    public void setUp() {
        validator = ListSyntaxValidator.getInstance();

        syntax = mock(EDISyntaxRule.class);
        when(syntax.getType()).thenReturn(EDISyntaxRule.Type.LIST);

        location = mock(Location.class);
        handler = mock(EventHandler.class);

        used = mock(UsageNode.class);
        when(used.isUsed()).thenReturn(true);

        unused = mock(UsageNode.class);
        when(unused.isUsed()).thenReturn(false);
    }

    @Test
    public void testValidateListConditionalAllUsed() {
        when(syntax.getPositions()).thenReturn(Arrays.asList(1, 3, 4));
        List<UsageNode> children = Arrays.asList(used, unused, used, used);
        final AtomicInteger count = new AtomicInteger(0);

        doAnswer((Answer<Void>) invocation -> {
            count.incrementAndGet();
            return null;
        }).when(handler)
          .elementError(eq(EDIStreamEvent.ELEMENT_OCCURRENCE_ERROR),
                        eq(EDIStreamValidationError.CONDITIONAL_REQUIRED_DATA_ELEMENT_MISSING),
                        any(Integer.class),
                        any(Integer.class),
                        any(Integer.class));

        validator.validate(syntax, location, children, handler);
        assertEquals(0, count.get());
    }

    @Test
    public void testValidateListConditionalAnchorUnused() {
        when(syntax.getPositions()).thenReturn(Arrays.asList(1, 3, 4));
        List<UsageNode> children = Arrays.asList(unused, unused, used);
        final AtomicInteger count = new AtomicInteger(0);

        doAnswer((Answer<Void>) invocation -> {
            count.incrementAndGet();
            return null;
        }).when(handler)
          .elementError(eq(EDIStreamEvent.ELEMENT_OCCURRENCE_ERROR),
                        eq(EDIStreamValidationError.CONDITIONAL_REQUIRED_DATA_ELEMENT_MISSING),
                        any(Integer.class),
                        any(Integer.class),
                        any(Integer.class));

        validator.validate(syntax, location, children, handler);
        assertEquals(0, count.get());
    }

    @Test
    public void testValidateListConditionalMissingRequired() {
        when(syntax.getPositions()).thenReturn(Arrays.asList(1, 3, 4));
        List<UsageNode> children = Arrays.asList(used, unused, unused, unused);
        final AtomicInteger count = new AtomicInteger(0);

        doAnswer((Answer<Void>) invocation -> {
            count.incrementAndGet();
            return null;
        }).when(handler)
          .elementError(eq(EDIStreamEvent.ELEMENT_OCCURRENCE_ERROR),
                        eq(EDIStreamValidationError.CONDITIONAL_REQUIRED_DATA_ELEMENT_MISSING),
                        any(Integer.class),
                        any(Integer.class),
                        any(Integer.class));

        validator.validate(syntax, location, children, handler);
        assertEquals(2, count.get()); // Error for both positions 3 and 4
    }
}
