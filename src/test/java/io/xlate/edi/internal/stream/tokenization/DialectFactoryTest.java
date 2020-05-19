package io.xlate.edi.internal.stream.tokenization;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class DialectFactoryTest {

    static char[] X12 = { '\0', 'I', 'S', 'A', '\0' };
    static char[] EDIFACT_A = { '\0', 'U', 'N', 'A', '\0' };
    static char[] EDIFACT_B = { '\0', 'U', 'N', 'B', '\0' };
    static char[] BAD = { 'B', 'A', 'D', '\0' };

    @Test
    void testX12() throws EDIException {
        Dialect d1 = DialectFactory.getDialect(X12, 1, 3);
        assertEquals("ISA", d1.getHeaderTag());
    }

    @Test
    void testEDIFACT_A() throws EDIException {
        Dialect d1 = DialectFactory.getDialect(EDIFACT_A, 1, 3);
        assertEquals("UNA", d1.getHeaderTag());
    }

    @Test
    void testEDIFACT_B() throws EDIException {
        Dialect d1 = DialectFactory.getDialect(EDIFACT_B, 1, 3);
        assertEquals("UNB", d1.getHeaderTag());
    }

    @Test
    void testInvalidTag() {
        assertThrows(EDIException.class, () -> DialectFactory.getDialect(BAD, 1, 3));
    }
}
