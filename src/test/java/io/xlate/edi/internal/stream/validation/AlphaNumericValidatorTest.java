package io.xlate.edi.internal.stream.validation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.xlate.edi.internal.stream.tokenization.CharacterSet;
import io.xlate.edi.internal.stream.tokenization.Dialect;
import io.xlate.edi.internal.stream.tokenization.DialectFactory;
import io.xlate.edi.internal.stream.tokenization.EDIException;
import io.xlate.edi.schema.EDISimpleType;
import io.xlate.edi.stream.EDIStreamValidationError;

public class AlphaNumericValidatorTest {

    Dialect dialect;

    @BeforeEach
    public void setUp() throws EDIException {
        dialect = DialectFactory.getDialect("UNA");
        CharacterSet chars = new CharacterSet();
        "UNA=*.?^~UNB*UNOA=3*005435656=1*006415160=1*060515=1434*00000000000778~".chars().forEach(c -> dialect.appendHeader(chars, (char) c));
    }

    @Test
    public void testValidateLengthTooShort() {
        EDISimpleType element = mock(EDISimpleType.class);
        when(element.getMinLength()).thenReturn(5L);
        when(element.getMaxLength()).thenReturn(5L);
        when(element.getValueSet()).thenReturn(Collections.<String>emptySet());
        ElementValidator v = AlphaNumericValidator.getInstance();
        List<EDIStreamValidationError> errors = new ArrayList<>();
        v.validate(dialect, element, "TEST", errors);
        assertEquals(1, errors.size());
        assertEquals(EDIStreamValidationError.DATA_ELEMENT_TOO_SHORT, errors.get(0));
    }

    @Test
    public void testValidateLengthTooLong() {
        EDISimpleType element = mock(EDISimpleType.class);
        when(element.getMinLength()).thenReturn(5L);
        when(element.getMaxLength()).thenReturn(5L);
        when(element.getValueSet()).thenReturn(Collections.<String>emptySet());
        ElementValidator v = AlphaNumericValidator.getInstance();
        List<EDIStreamValidationError> errors = new ArrayList<>();
        v.validate(dialect, element, "TESTTEST", errors);
        assertEquals(1, errors.size());
        assertEquals(EDIStreamValidationError.DATA_ELEMENT_TOO_LONG, errors.get(0));
    }

    @Test
    public void testValidateValueNotInSet() {
        EDISimpleType element = mock(EDISimpleType.class);
        when(element.getMinLength()).thenReturn(4L);
        when(element.getMaxLength()).thenReturn(5L);
        when(element.getValueSet()).thenReturn(new HashSet<String>(Arrays.asList("VAL1", "VAL2")));
        ElementValidator v = AlphaNumericValidator.getInstance();
        List<EDIStreamValidationError> errors = new ArrayList<>();
        v.validate(dialect, element, "TEST", errors);
        assertEquals(1, errors.size());
        assertEquals(EDIStreamValidationError.INVALID_CODE_VALUE, errors.get(0));
    }

    @Test
    public void testValidateValueInSetBadCharacter() {
        EDISimpleType element = mock(EDISimpleType.class);
        when(element.getMinLength()).thenReturn(4L);
        when(element.getMaxLength()).thenReturn(5L);
        when(element.getValueSet()).thenReturn(new HashSet<String>(Arrays.asList("VAL1", "VAL\u0008")));
        ElementValidator v = AlphaNumericValidator.getInstance();
        List<EDIStreamValidationError> errors = new ArrayList<>();
        v.validate(dialect, element, "VAL\u0008", errors);
        assertEquals(1, errors.size());
        assertEquals(EDIStreamValidationError.INVALID_CHARACTER_DATA, errors.get(0));
    }

    @Test
    public void testFormatValueTooLong() {
        EDISimpleType element = mock(EDISimpleType.class);
        when(element.getMinLength()).thenReturn(4L);
        when(element.getMaxLength()).thenReturn(5L);
        ElementValidator v = AlphaNumericValidator.getInstance();
        StringBuilder output = new StringBuilder();
        try {
            v.format(dialect, element, "TESTTEST", output);
            fail("Exception was expected");
        } catch (EDIException e) {
            assertTrue(e.getMessage().startsWith("EDIE005"));
        }
    }

    @Test
    public void testFormatValueNotInSet() {
        EDISimpleType element = mock(EDISimpleType.class);
        when(element.getMinLength()).thenReturn(4L);
        when(element.getMaxLength()).thenReturn(8L);
        when(element.getValueSet()).thenReturn(new HashSet<String>(Arrays.asList("VAL1", "VAL2")));
        ElementValidator v = AlphaNumericValidator.getInstance();
        StringBuilder output = new StringBuilder();
        try {
            v.format(dialect, element, "TESTTEST", output);
            fail("Exception was expected");
        } catch (EDIException e) {
            assertTrue(e.getMessage().startsWith("EDIE006"));
        }
    }

    @Test
    public void testFormatValueInSet() throws EDIException {
        EDISimpleType element = mock(EDISimpleType.class);
        when(element.getMinLength()).thenReturn(4L);
        when(element.getMaxLength()).thenReturn(8L);
        when(element.getValueSet()).thenReturn(new HashSet<String>(Arrays.asList("VAL1", "VAL2")));
        ElementValidator v = AlphaNumericValidator.getInstance();
        StringBuilder output = new StringBuilder();
        v.format(dialect, element, "VAL1", output);
        assertEquals("VAL1", output.toString());
    }

    @Test
    public void testFormatInvalidCharacterData() {
        EDISimpleType element = mock(EDISimpleType.class);
        when(element.getMinLength()).thenReturn(4L);
        when(element.getMaxLength()).thenReturn(4L);
        ElementValidator v = AlphaNumericValidator.getInstance();
        StringBuilder output = new StringBuilder();
        try {
            v.format(dialect, element, "TES\u0008", output);
            fail("Exception was expected");
        } catch (EDIException e) {
            assertTrue(e.getMessage().startsWith("EDIE004"));
        }
    }

    @Test
    public void testFormatValidValuePaddedLength() throws EDIException {
        EDISimpleType element = mock(EDISimpleType.class);
        when(element.getMinLength()).thenReturn(10L);
        when(element.getMaxLength()).thenReturn(10L);
        ElementValidator v = AlphaNumericValidator.getInstance();
        StringBuilder output = new StringBuilder();
        v.format(dialect, element, "TEST", output);
        assertEquals("TEST      ", output.toString());
    }
}
