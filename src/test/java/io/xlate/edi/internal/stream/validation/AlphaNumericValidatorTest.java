package io.xlate.edi.internal.stream.validation;

import static org.junit.jupiter.api.Assertions.assertEquals;
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

class AlphaNumericValidatorTest implements ValueSetTester {

    Dialect dialect;
    AlphaNumericValidator v;

    @BeforeEach
    void setUp() throws EDIException {
        dialect = DialectFactory.getDialect("UNA");
        v = new AlphaNumericValidator();
        CharacterSet chars = new CharacterSet();
        "UNA=*.?^~UNB*UNOA=3*005435656=1*006415160=1*060515=1434*00000000000778~".chars().forEach(c -> dialect.appendHeader(chars, (char) c));
    }

    @Test
    void testValidateLengthTooShort() {
        EDISimpleType element = mock(EDISimpleType.class);
        when(element.getMinLength(anyString())).thenCallRealMethod();
        when(element.getMaxLength(anyString())).thenCallRealMethod();
        when(element.getValueSet(anyString())).thenCallRealMethod();

        when(element.getMinLength()).thenReturn(5L);
        when(element.getMaxLength()).thenReturn(5L);
        when(element.getValueSet()).thenReturn(setOf());
        List<EDIStreamValidationError> errors = new ArrayList<>();
        v.validate(dialect, element, "TEST", errors);
        assertEquals(1, errors.size());
        assertEquals(EDIStreamValidationError.DATA_ELEMENT_TOO_SHORT, errors.get(0));
    }

    @Test
    void testValidateLengthTooLong() {
        EDISimpleType element = mock(EDISimpleType.class);
        when(element.getMinLength(anyString())).thenCallRealMethod();
        when(element.getMaxLength(anyString())).thenCallRealMethod();
        when(element.getValueSet(anyString())).thenCallRealMethod();

        when(element.getMinLength()).thenReturn(5L);
        when(element.getMaxLength()).thenReturn(5L);
        when(element.getValueSet()).thenReturn(setOf());
        List<EDIStreamValidationError> errors = new ArrayList<>();
        v.validate(dialect, element, "TESTTEST", errors);
        assertEquals(1, errors.size());
        assertEquals(EDIStreamValidationError.DATA_ELEMENT_TOO_LONG, errors.get(0));
    }

    @Test
    void testValidateValueNotInSet() {
        EDISimpleType element = mock(EDISimpleType.class);
        when(element.getMinLength(anyString())).thenCallRealMethod();
        when(element.getMaxLength(anyString())).thenCallRealMethod();
        when(element.getValueSet(anyString())).thenCallRealMethod();
        when(element.getValues(anyString())).thenCallRealMethod();

        when(element.getMinLength()).thenReturn(4L);
        when(element.getMaxLength()).thenReturn(5L);
        when(element.getValues()).thenReturn(mapOf("VAL1", "Title1", "VAL2", "Title2"));
        List<EDIStreamValidationError> errors = new ArrayList<>();
        v.validate(dialect, element, "TEST", errors);
        assertEquals(1, errors.size());
        assertEquals(EDIStreamValidationError.INVALID_CODE_VALUE, errors.get(0));
    }

    @Test
    void testValidateValueInSetBadCharacter() {
        EDISimpleType element = mock(EDISimpleType.class);
        when(element.getMinLength(anyString())).thenCallRealMethod();
        when(element.getMaxLength(anyString())).thenCallRealMethod();
        when(element.getValueSet(anyString())).thenCallRealMethod();

        when(element.getMinLength()).thenReturn(4L);
        when(element.getMaxLength()).thenReturn(5L);
        when(element.getValueSet()).thenReturn(setOf("VAL1", "VAL\u0008"));
        List<EDIStreamValidationError> errors = new ArrayList<>();
        v.validate(dialect, element, "VAL\u0008", errors);
        assertEquals(1, errors.size());
        assertEquals(EDIStreamValidationError.INVALID_CHARACTER_DATA, errors.get(0));
    }

    @Test
    void testFormatValueTooLong() {
        EDISimpleType element = mock(EDISimpleType.class);
        when(element.getMinLength(anyString())).thenCallRealMethod();
        when(element.getMaxLength(anyString())).thenCallRealMethod();
        when(element.getValueSet(anyString())).thenCallRealMethod();

        when(element.getMinLength()).thenReturn(4L);
        when(element.getMaxLength()).thenReturn(5L);
        StringBuilder output = new StringBuilder();
        v.format(dialect, element, "TESTTEST", output);
        assertHasError(v, dialect, element, output, EDIStreamValidationError.DATA_ELEMENT_TOO_LONG);
    }

    @Test
    void testFormatValueNotInSet() {
        EDISimpleType element = mock(EDISimpleType.class);
        when(element.getMinLength(anyString())).thenCallRealMethod();
        when(element.getMaxLength(anyString())).thenCallRealMethod();
        when(element.getValueSet(anyString())).thenCallRealMethod();
        when(element.getValues(anyString())).thenCallRealMethod();

        when(element.getMinLength()).thenReturn(4L);
        when(element.getMaxLength()).thenReturn(8L);
        when(element.getValues()).thenReturn(mapOf("VAL1", "Title1", "VAL2", "Title2"));
        StringBuilder output = new StringBuilder();
        v.format(dialect, element, "TESTTEST", output);
        assertHasError(v, dialect, element, output, EDIStreamValidationError.INVALID_CODE_VALUE);
    }

    @Test
    void testFormatValueInSet() throws EDIException {
        EDISimpleType element = mock(EDISimpleType.class);
        when(element.getMinLength(anyString())).thenCallRealMethod();
        when(element.getMaxLength(anyString())).thenCallRealMethod();
        when(element.getValueSet(anyString())).thenCallRealMethod();

        when(element.getMinLength()).thenReturn(4L);
        when(element.getMaxLength()).thenReturn(8L);
        when(element.getValueSet()).thenReturn(setOf("VAL1", "VAL2"));
        StringBuilder output = new StringBuilder();
        v.format(dialect, element, "VAL1", output);
        assertEquals("VAL1", output.toString());
    }

    @Test
    void testFormatInvalidCharacterData() {
        EDISimpleType element = mock(EDISimpleType.class);
        when(element.getMinLength(anyString())).thenCallRealMethod();
        when(element.getMaxLength(anyString())).thenCallRealMethod();
        when(element.getValueSet(anyString())).thenCallRealMethod();

        when(element.getMinLength()).thenReturn(4L);
        when(element.getMaxLength()).thenReturn(4L);
        StringBuilder output = new StringBuilder();
        v.format(dialect, element, "TES\u0008", output);
        assertHasError(v, dialect, element, output, EDIStreamValidationError.INVALID_CHARACTER_DATA);
    }

    @Test
    void testFormatValidValuePaddedLength() throws EDIException {
        EDISimpleType element = mock(EDISimpleType.class);
        when(element.getMinLength(anyString())).thenCallRealMethod();
        when(element.getMaxLength(anyString())).thenCallRealMethod();
        when(element.getValueSet(anyString())).thenCallRealMethod();

        when(element.getMinLength()).thenReturn(10L);
        when(element.getMaxLength()).thenReturn(10L);
        StringBuilder output = new StringBuilder();
        v.format(dialect, element, "TEST", output);
        assertEquals("TEST      ", output.toString());
    }
}
