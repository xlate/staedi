package io.xlate.edi.stream;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class EDIValidationExceptionTest {

    @Test
    void testSetNextException() {
        EDIValidationException[] exceptions = { new EDIValidationException(null, null, null, null),
                                                new EDIValidationException(null, null, null, null),
                                                new EDIValidationException(null, null, null, null),
                                                new EDIValidationException(null, null, null, null),
                                                new EDIValidationException(null, null, null, null)
        };

        EDIValidationException primary = new EDIValidationException(null, null, null, null);
        primary.setNextException(exceptions[0]);
        primary.setNextException(exceptions[1]);
        primary.setNextException(exceptions[2]);
        primary.setNextException(exceptions[3]);
        primary.setNextException(exceptions[4]);

        assertSame(exceptions[0], primary.getNextException());
        assertSame(exceptions[1], primary.getNextException()
                   .getNextException());
        assertSame(exceptions[2], primary.getNextException()
                   .getNextException()
                   .getNextException());
        assertSame(exceptions[3], primary.getNextException()
                   .getNextException()
                   .getNextException()
                   .getNextException());
        assertSame(exceptions[4], primary.getNextException()
                   .getNextException()
                   .getNextException()
                   .getNextException()
                   .getNextException());
    }

}
