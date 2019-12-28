package io.xlate.edi.internal.stream.validation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.xlate.edi.internal.stream.tokenization.CharacterSet;
import io.xlate.edi.internal.stream.tokenization.Dialect;
import io.xlate.edi.internal.stream.tokenization.DialectFactory;
import io.xlate.edi.internal.stream.tokenization.EDIException;
import io.xlate.edi.schema.EDISimpleType;
import io.xlate.edi.stream.EDIStreamValidationError;

public class TimeValidatorTest {

    Dialect dialect;

    @BeforeEach
    public void setUp() throws EDIException {
        dialect = DialectFactory.getDialect("UNA");
        CharacterSet chars = new CharacterSet();
        "UNA=*.?^~UNB*UNOA=3*005435656=1*006415160=1*060515=1434*00000000000778~".chars().forEach(c -> dialect.appendHeader(chars, (char) c));
    }

    @Test
    public void testValidateValueTooShort() {
        EDISimpleType element = mock(EDISimpleType.class);
        when(element.getMinLength()).thenReturn(4L);
        when(element.getMaxLength()).thenReturn(8L);
        when(element.getValueSet()).thenReturn(Collections.<String>emptySet());
        ElementValidator v = TimeValidator.getInstance();
        List<EDIStreamValidationError> errors = new ArrayList<>();
        v.validate(dialect, element, "09", errors);
        assertEquals(2, errors.size());
        assertEquals(EDIStreamValidationError.DATA_ELEMENT_TOO_SHORT, errors.get(0));
        assertEquals(EDIStreamValidationError.INVALID_TIME, errors.get(1));
    }

    @Test
    public void testValidateValueTooLong() {
        EDISimpleType element = mock(EDISimpleType.class);
        when(element.getMinLength()).thenReturn(4L);
        when(element.getMaxLength()).thenReturn(8L);
        when(element.getValueSet()).thenReturn(Collections.<String>emptySet());
        ElementValidator v = TimeValidator.getInstance();
        List<EDIStreamValidationError> errors = new ArrayList<>();
        v.validate(dialect, element, "1230599999", errors);
        assertEquals(2, errors.size());
        assertEquals(EDIStreamValidationError.DATA_ELEMENT_TOO_LONG, errors.get(0));
        assertEquals(EDIStreamValidationError.INVALID_TIME, errors.get(1));
    }

    @Test
    public void testValidateInvalidValue() {
        EDISimpleType element = mock(EDISimpleType.class);
        when(element.getMinLength()).thenReturn(4L);
        when(element.getMaxLength()).thenReturn(8L);
        when(element.getValueSet()).thenReturn(Collections.<String>emptySet());
        ElementValidator v = TimeValidator.getInstance();
        List<EDIStreamValidationError> errors = new ArrayList<>();
        v.validate(dialect, element, "123059AA", errors);
        assertEquals(1, errors.size());
        assertEquals(EDIStreamValidationError.INVALID_TIME, errors.get(0));
    }

    @Test
    public void testValidateValidValue() {
        EDISimpleType element = mock(EDISimpleType.class);
        when(element.getMinLength()).thenReturn(4L);
        when(element.getMaxLength()).thenReturn(8L);
        when(element.getValueSet()).thenReturn(Collections.<String>emptySet());
        ElementValidator v = TimeValidator.getInstance();
        List<EDIStreamValidationError> errors = new ArrayList<>();
        v.validate(dialect, element, "12305900", errors);
        assertEquals(0, errors.size());
    }

    @Test
    public void testValidValues() {
        assertTrue(TimeValidator.validValue("003059"));
        assertTrue(TimeValidator.validValue("00305999"));
    }

    @Test
    public void testInvalidValues() {
        assertFalse(TimeValidator.validValue("A03059"));
        assertFalse(TimeValidator.validValue("003060"));
        assertFalse(TimeValidator.validValue("006059"));
        assertFalse(TimeValidator.validValue("245959"));
    }

    @Test
    public void testFormatValueTooLong() {
        EDISimpleType element = mock(EDISimpleType.class);
        when(element.getMinLength()).thenReturn(4L);
        when(element.getMaxLength()).thenReturn(8L);
        when(element.getValueSet()).thenReturn(Collections.<String>emptySet());
        ElementValidator v = TimeValidator.getInstance();
        StringBuilder output = new StringBuilder();
        try {
            v.format(dialect, element, "1230599999", output);
            fail("Exception was expected");
        } catch (EDIException e) {
            assertTrue(e.getMessage().startsWith("EDIE005"));
        }
    }

    @Test
    public void testFormatInvalidTime() {
        EDISimpleType element = mock(EDISimpleType.class);
        when(element.getMinLength()).thenReturn(4L);
        when(element.getMaxLength()).thenReturn(8L);
        when(element.getValueSet()).thenReturn(Collections.<String>emptySet());
        ElementValidator v = TimeValidator.getInstance();
        StringBuilder output = new StringBuilder();
        try {
            v.format(dialect, element, "123059AA", output);
            fail("Exception was expected");
        } catch (EDIException e) {
            assertTrue(e.getMessage().startsWith("EDIE009"));
        }
    }

    @Test
    public void testFormatValidTime() throws EDIException {
        EDISimpleType element = mock(EDISimpleType.class);
        when(element.getMinLength()).thenReturn(4L);
        when(element.getMaxLength()).thenReturn(8L);
        when(element.getValueSet()).thenReturn(Collections.<String>emptySet());
        ElementValidator v = TimeValidator.getInstance();
        StringBuilder output = new StringBuilder();
        v.format(dialect, element, "123059", output);
        assertEquals("123059", output.toString());
    }

    @Test
    public void testFormatValidTimePadded() throws EDIException {
        EDISimpleType element = mock(EDISimpleType.class);
        when(element.getMinLength()).thenReturn(6L);
        when(element.getMaxLength()).thenReturn(8L);
        when(element.getValueSet()).thenReturn(Collections.<String>emptySet());
        ElementValidator v = TimeValidator.getInstance();
        StringBuilder output = new StringBuilder();
        v.format(dialect, element, "1230", output);
        assertEquals("123000", output.toString());
    }

}
