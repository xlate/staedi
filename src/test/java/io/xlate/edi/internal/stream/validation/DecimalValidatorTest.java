package io.xlate.edi.internal.stream.validation;

import static org.junit.Assert.*;

import org.junit.Test;

import io.xlate.edi.internal.stream.validation.DecimalValidator;

public class DecimalValidatorTest {

    @Test
    public void testValidateInvalidNegative() {
        assertEquals(-2, DecimalValidator.getInstance().validate("20-"));
    }

    @Test
    public void testValidateValidNegative() {
        assertEquals(2, DecimalValidator.getInstance().validate("-20"));
    }

    @Test
    public void testValidateValidDecimal() {
        assertEquals(4, DecimalValidator.getInstance().validate("20.00"));
    }

    @Test
    public void testValidateInvalidTooManyDecimalPoints() {
        assertEquals(-4, DecimalValidator.getInstance().validate("20.00."));
    }

    @Test
    public void testValidateExponentialsValid() {
        DecimalValidator v = DecimalValidator.getInstance();
        assertEquals(6, v.validate("1.234E-56"));
        assertEquals(6, v.validate("-1.234E-56"));
        assertEquals(2, v.validate("1E2"));
    }

    @Test
    public void testValidateExponentialsInvalid() {
        DecimalValidator v = DecimalValidator.getInstance();
        assertEquals(-6, v.validate("1.234E-5.6"));
        assertEquals(-6, v.validate("-1.234E-5E-6"));
        assertEquals(-2, v.validate("1E--2."));
    }

    @Test
    public void testValidateBadExponentialCharacter() {
        DecimalValidator v = DecimalValidator.getInstance();
        assertEquals(-7, v.validate("1.234e56"));
    }
}
