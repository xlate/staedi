package io.xlate.edi.stream.validation;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.Test;

import io.xlate.edi.schema.EDISimpleType;
import io.xlate.edi.stream.EDIStreamValidationError;
import io.xlate.edi.stream.internal.EDIException;

public class NumericValidatorTest {

    @Test
    public void testValidateInvalidNegative() {
        assertEquals(-2, NumericValidator.getInstance().validate("20-"));
    }

    @Test
    public void testValidateValidNegative() {
        assertEquals(2, NumericValidator.getInstance().validate("-20"));
    }

    @Test
    public void testValidateLengthTooShort() {
        EDISimpleType element = mock(EDISimpleType.class);
        when(element.getMinLength()).thenReturn(5);
        when(element.getMaxLength()).thenReturn(10);
        when(element.getValueSet()).thenReturn(Collections.emptySet());
        ElementValidator v = NumericValidator.getInstance();
        List<EDIStreamValidationError> errors = new ArrayList<>();
        v.validate(element, "1234", errors);
        assertEquals(1, errors.size());
        assertEquals(EDIStreamValidationError.DATA_ELEMENT_TOO_SHORT, errors.get(0));
    }

    @Test
    public void testValidateLengthTooLong() {
        EDISimpleType element = mock(EDISimpleType.class);
        when(element.getMinLength()).thenReturn(5);
        when(element.getMaxLength()).thenReturn(10);
        when(element.getValueSet()).thenReturn(Collections.emptySet());
        ElementValidator v = NumericValidator.getInstance();
        List<EDIStreamValidationError> errors = new ArrayList<>();
        v.validate(element, "12345678901", errors);
        assertEquals(1, errors.size());
        assertEquals(EDIStreamValidationError.DATA_ELEMENT_TOO_LONG, errors.get(0));
    }

    @Test
    public void testValidateInvalidCharacter() {
        EDISimpleType element = mock(EDISimpleType.class);
        when(element.getMinLength()).thenReturn(5);
        when(element.getMaxLength()).thenReturn(10);
        when(element.getValueSet()).thenReturn(Collections.emptySet());
        ElementValidator v = NumericValidator.getInstance();
        List<EDIStreamValidationError> errors = new ArrayList<>();
        v.validate(element, "1234F", errors);
        assertEquals(1, errors.size());
        assertEquals(EDIStreamValidationError.INVALID_CHARACTER_DATA, errors.get(0));
    }

    @Test
    public void testFormatValueTooLong() {
        EDISimpleType element = mock(EDISimpleType.class);
        when(element.getMinLength()).thenReturn(4);
        when(element.getMaxLength()).thenReturn(5);
        ElementValidator v = NumericValidator.getInstance();
        StringBuilder output = new StringBuilder();
        try {
            v.format(element, "123456", output);
            fail("Exception was expected:" + output.toString());
        } catch (EDIException e) {
            assertEquals("EDIE005", e.getMessage().subSequence(0, 7));
        }
    }

    @Test
    public void testFormatInvalidCharacterData() {
        EDISimpleType element = mock(EDISimpleType.class);
        when(element.getMinLength()).thenReturn(4);
        when(element.getMaxLength()).thenReturn(5);
        ElementValidator v = NumericValidator.getInstance();
        StringBuilder output = new StringBuilder();
        try {
            v.format(element, "1234F", output);
            fail("Exception was expected: " + output.toString());
        } catch (EDIException e) {
            assertEquals("EDIE004", e.getMessage().subSequence(0, 7));
        }
    }

    @Test
    public void testFormatValidNumber() throws EDIException {
        EDISimpleType element = mock(EDISimpleType.class);
        when(element.getMinLength()).thenReturn(4);
        when(element.getMaxLength()).thenReturn(8);
        when(element.getValueSet()).thenReturn(Collections.emptySet());
        ElementValidator v = NumericValidator.getInstance();
        StringBuilder output = new StringBuilder();
        v.format(element, "1234", output);
        assertEquals("1234", output.toString());
    }

    @Test
    public void testFormatValidNumberPadded() throws EDIException {
        EDISimpleType element = mock(EDISimpleType.class);
        when(element.getMinLength()).thenReturn(6);
        when(element.getMaxLength()).thenReturn(8);
        when(element.getValueSet()).thenReturn(Collections.emptySet());
        ElementValidator v = NumericValidator.getInstance();
        StringBuilder output = new StringBuilder();
        v.format(element, "123", output);
        assertEquals("000123", output.toString());
    }
}
