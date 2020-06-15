package io.xlate.edi.internal.stream.validation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.xlate.edi.internal.stream.tokenization.CharacterSet;
import io.xlate.edi.internal.stream.tokenization.Dialect;
import io.xlate.edi.internal.stream.tokenization.DialectFactory;
import io.xlate.edi.internal.stream.tokenization.EDIException;
import io.xlate.edi.schema.EDISimpleType;
import io.xlate.edi.stream.EDIStreamValidationError;
import io.xlate.edi.stream.EDIValidationException;

class TimeValidatorTest implements ValueSetTester {

    Dialect dialect;

    @BeforeEach
    void setUp() throws EDIException {
        dialect = DialectFactory.getDialect("UNA");
        CharacterSet chars = new CharacterSet();
        "UNA=*.?^~UNB*UNOA=3*005435656=1*006415160=1*060515=1434*00000000000778~".chars().forEach(c -> dialect.appendHeader(chars, (char) c));
    }

    @Test
    void testValidateValueTooShort() {
        EDISimpleType element = mock(EDISimpleType.class);
        when(element.getMinLength(anyString())).thenCallRealMethod();
        when(element.getMaxLength(anyString())).thenCallRealMethod();
        when(element.getValueSet(anyString())).thenCallRealMethod();

        when(element.getMinLength()).thenReturn(4L);
        when(element.getMaxLength()).thenReturn(8L);
        when(element.getValueSet()).thenReturn(setOf());
        ElementValidator v = TimeValidator.getInstance();
        List<EDIStreamValidationError> errors = new ArrayList<>();
        v.validate(dialect, element, "09", errors);
        assertEquals(2, errors.size());
        assertEquals(EDIStreamValidationError.DATA_ELEMENT_TOO_SHORT, errors.get(0));
        assertEquals(EDIStreamValidationError.INVALID_TIME, errors.get(1));
    }

    @Test
    void testValidateValueTooLong() {
        EDISimpleType element = mock(EDISimpleType.class);
        when(element.getMinLength(anyString())).thenCallRealMethod();
        when(element.getMaxLength(anyString())).thenCallRealMethod();
        when(element.getValueSet(anyString())).thenCallRealMethod();

        when(element.getMinLength()).thenReturn(4L);
        when(element.getMaxLength()).thenReturn(8L);
        when(element.getValueSet()).thenReturn(setOf());
        ElementValidator v = TimeValidator.getInstance();
        List<EDIStreamValidationError> errors = new ArrayList<>();
        v.validate(dialect, element, "1230599999", errors);
        assertEquals(2, errors.size());
        assertEquals(EDIStreamValidationError.DATA_ELEMENT_TOO_LONG, errors.get(0));
        assertEquals(EDIStreamValidationError.INVALID_TIME, errors.get(1));
    }

    @Test
    void testValidateInvalidValue() {
        EDISimpleType element = mock(EDISimpleType.class);
        when(element.getMinLength(anyString())).thenCallRealMethod();
        when(element.getMaxLength(anyString())).thenCallRealMethod();
        when(element.getValueSet(anyString())).thenCallRealMethod();

        when(element.getMinLength()).thenReturn(4L);
        when(element.getMaxLength()).thenReturn(8L);
        when(element.getValueSet()).thenReturn(setOf());
        ElementValidator v = TimeValidator.getInstance();
        List<EDIStreamValidationError> errors = new ArrayList<>();
        v.validate(dialect, element, "123059AA", errors);
        assertEquals(1, errors.size());
        assertEquals(EDIStreamValidationError.INVALID_TIME, errors.get(0));
    }

    @Test
    void testValidateValidValue() {
        EDISimpleType element = mock(EDISimpleType.class);
        when(element.getMinLength(anyString())).thenCallRealMethod();
        when(element.getMaxLength(anyString())).thenCallRealMethod();
        when(element.getValueSet(anyString())).thenCallRealMethod();

        when(element.getMinLength()).thenReturn(4L);
        when(element.getMaxLength()).thenReturn(8L);
        when(element.getValueSet()).thenReturn(setOf());
        ElementValidator v = TimeValidator.getInstance();
        List<EDIStreamValidationError> errors = new ArrayList<>();
        v.validate(dialect, element, "12305900", errors);
        assertEquals(0, errors.size());
    }

    @Test
    void testValidValues() {
        assertTrue(TimeValidator.validValue("003059"));
        assertTrue(TimeValidator.validValue("00305999"));
    }

    @Test
    void testInvalidValues() {
        assertFalse(TimeValidator.validValue("A03059"));
        assertFalse(TimeValidator.validValue("003060"));
        assertFalse(TimeValidator.validValue("006059"));
        assertFalse(TimeValidator.validValue("245959"));
    }

    @Test
    void testFormatValueTooLong() {
        EDISimpleType element = mock(EDISimpleType.class);
        when(element.getMinLength(anyString())).thenCallRealMethod();
        when(element.getMaxLength(anyString())).thenCallRealMethod();
        when(element.getValueSet(anyString())).thenCallRealMethod();

        when(element.getMinLength()).thenReturn(4L);
        when(element.getMaxLength()).thenReturn(8L);
        when(element.getValueSet()).thenReturn(setOf());
        ElementValidator v = TimeValidator.getInstance();
        StringBuilder output = new StringBuilder();
        EDIValidationException e = assertThrows(EDIValidationException.class, () -> v.format(dialect, element, "1230599999", output));
        assertEquals(EDIStreamValidationError.DATA_ELEMENT_TOO_LONG, e.getError());
    }

    @Test
    void testFormatInvalidTime() {
        EDISimpleType element = mock(EDISimpleType.class);
        when(element.getMinLength(anyString())).thenCallRealMethod();
        when(element.getMaxLength(anyString())).thenCallRealMethod();
        when(element.getValueSet(anyString())).thenCallRealMethod();

        when(element.getMinLength()).thenReturn(4L);
        when(element.getMaxLength()).thenReturn(8L);
        when(element.getValueSet()).thenReturn(setOf());
        ElementValidator v = TimeValidator.getInstance();
        StringBuilder output = new StringBuilder();
        EDIValidationException e = assertThrows(EDIValidationException.class, () -> v.format(dialect, element, "123059AA", output));
        assertEquals(EDIStreamValidationError.INVALID_TIME, e.getError());
    }

    @Test
    void testFormatValidTime() throws EDIException {
        EDISimpleType element = mock(EDISimpleType.class);
        when(element.getMinLength(anyString())).thenCallRealMethod();
        when(element.getMaxLength(anyString())).thenCallRealMethod();
        when(element.getValueSet(anyString())).thenCallRealMethod();

        when(element.getMinLength()).thenReturn(4L);
        when(element.getMaxLength()).thenReturn(8L);
        when(element.getValueSet()).thenReturn(setOf());
        ElementValidator v = TimeValidator.getInstance();
        StringBuilder output = new StringBuilder();
        v.format(dialect, element, "123059", output);
        assertEquals("123059", output.toString());
    }

    @Test
    void testFormatValidTimePadded() throws EDIException {
        EDISimpleType element = mock(EDISimpleType.class);
        when(element.getMinLength(anyString())).thenCallRealMethod();
        when(element.getMaxLength(anyString())).thenCallRealMethod();
        when(element.getValueSet(anyString())).thenCallRealMethod();

        when(element.getMinLength()).thenReturn(6L);
        when(element.getMaxLength()).thenReturn(8L);
        when(element.getValueSet()).thenReturn(setOf());
        ElementValidator v = TimeValidator.getInstance();
        StringBuilder output = new StringBuilder();
        v.format(dialect, element, "1230", output);
        assertEquals("123000", output.toString());
    }

}
