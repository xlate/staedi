package io.xlate.edi.internal.stream.validation;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.xlate.edi.internal.stream.tokenization.CharacterSet;
import io.xlate.edi.internal.stream.tokenization.Dialect;
import io.xlate.edi.internal.stream.tokenization.DialectFactory;
import io.xlate.edi.internal.stream.tokenization.EDIException;

class DecimalValidatorTest {

    Dialect dialectEdifact;
    Dialect dialectX12;

    @BeforeEach
    void setUp() throws EDIException {
        dialectEdifact = DialectFactory.getDialect("UNA");
        CharacterSet charsEdifact = new CharacterSet();
        "UNA=*,?^~UNB*UNOA=3*005435656=1*006415160=1*060515=1434*00000000000778~".chars().forEach(c -> dialectEdifact.appendHeader(charsEdifact, (char) c));

        dialectX12 = DialectFactory.getDialect("ISA");
        CharacterSet charsX12 = new CharacterSet();
        "ISA*00*          *00*          *ZZ*ReceiverID     *ZZ*Sender         *050812*1953*^*00501*508121953*0*P*:~".chars().forEach(c -> dialectX12.appendHeader(charsX12, (char) c));

    }

    @Test
    void testValidateInvalidNegative() {
        assertEquals(-2, DecimalValidator.getInstance().validate(dialectEdifact, "20-"));
    }

    @Test
    void testValidateValidNegative() {
        assertEquals(10, DecimalValidator.getInstance().validate(dialectEdifact, "-1234567890"));
    }

    @Test
    void testValidateValidDecimal() {
        assertEquals(4, DecimalValidator.getInstance().validate(dialectEdifact, "20,00"));
        assertEquals(4, DecimalValidator.getInstance().validate(dialectX12, "20.00"));
    }

    @Test
    void testValidateInvalidTooManyDecimalPoints() {
        assertEquals(-4, DecimalValidator.getInstance().validate(dialectEdifact, "20,00,"));
        assertEquals(-4, DecimalValidator.getInstance().validate(dialectX12, "20.00."));
    }

    @Test
    void testValidateExponentialsValid() {
        DecimalValidator v = DecimalValidator.getInstance();
        assertEquals(6, v.validate(dialectEdifact, "1,234E-56"));
        assertEquals(6, v.validate(dialectEdifact, "-1,234E-56"));
        assertEquals(2, v.validate(dialectEdifact, "1E2"));

        assertEquals(6, v.validate(dialectX12, "1.234E-56"));
        assertEquals(6, v.validate(dialectX12, "-1.234E-56"));
        assertEquals(2, v.validate(dialectX12, "1E2"));
    }

    @Test
    void testValidateExponentialsInvalid() {
        DecimalValidator v = DecimalValidator.getInstance();
        assertEquals(-6, v.validate(dialectEdifact, "1,234E-5,6"));
        assertEquals(-6, v.validate(dialectEdifact, "-1,234E-5E-6"));
        assertEquals(-2, v.validate(dialectEdifact, "1E--2,"));

        assertEquals(-6, v.validate(dialectX12, "1.234E-5.6"));
        assertEquals(-6, v.validate(dialectX12, "-1.234E-5E-6"));
        assertEquals(-2, v.validate(dialectX12, "1E--2."));
    }

    @Test
    void testValidateBadExponentialCharacter() {
        DecimalValidator v = DecimalValidator.getInstance();
        assertEquals(-7, v.validate(dialectEdifact, "1,234e56"));
        assertEquals(-7, v.validate(dialectX12, "1.234e56"));
    }
}
