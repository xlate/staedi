package io.xlate.edi.internal.stream.tokenization;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class CharacterSetTest {

    @Test
    void testGetClassAscii() {
        CharacterSet target = new CharacterSet();
        assertEquals(CharacterClass.ALPHANUMERIC, target.getClass('X'));
    }

    @Test
    void testGetClassUTF8() {
        CharacterSet target = new CharacterSet();
        assertEquals(CharacterClass.OTHER, target.getClass('£'));
    }

    @Test
    void testSetClassAscii() {
        CharacterSet target = new CharacterSet();
        target.setClass('\\', CharacterClass.RELEASE_CHARACTER);
        assertTrue(target.isRelease('\\'));
    }

    @Test
    void testSetClassUTF8() {
        CharacterSet target = new CharacterSet();
        Throwable thrown = assertThrows(ArrayIndexOutOfBoundsException.class,
                                        () -> target.setClass('£', CharacterClass.RELEASE_CHARACTER));
        assertEquals("Array index out of range: " + ((int) '£'), thrown.getMessage());
    }

    @Test
    void testGetDelimiterValidClass() {
        CharacterSet target = new CharacterSet();
        target.setClass('`', CharacterClass.ELEMENT_REPEATER);
        assertEquals('`', target.getDelimiter(CharacterClass.ELEMENT_REPEATER));
    }

    @Test
    void testGetDelimiterInvalidClass() {
        CharacterSet target = new CharacterSet();
        target.setClass('X', CharacterClass.ALPHANUMERIC);
        Throwable thrown = assertThrows(IllegalArgumentException.class,
                                        () -> target.getDelimiter(CharacterClass.ALPHANUMERIC));
        assertEquals("Nondelimiter class: " + CharacterClass.ALPHANUMERIC, thrown.getMessage());
    }

    @Test
    void testIsReleaseAscii() {
        CharacterSet target = new CharacterSet();
        target.setClass('\\', CharacterClass.RELEASE_CHARACTER);
        assertTrue(target.isRelease('\\'));
    }

    @Test
    void testIsReleaseUTF8() {
        CharacterSet target = new CharacterSet();
        assertFalse(target.isRelease('£'));
    }

    @Test
    void testIsValidAscii() {
        assertTrue(CharacterSet.isValid('A'));
    }

    @Test
    void testIsValidUTF8() {
        assertTrue(CharacterSet.isValid('ü'));
    }
}
