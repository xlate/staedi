package io.xlate.edi.internal.stream.tokenization;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class CharacterSetTest {

    @Test
    void testGetClassAscii() {
        CharacterSet target = new CharacterSet();
        assertEquals(CharacterClass.ALPHANUMERIC, target.getClass('Y'));
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
        assertTrue(target.isCharacterClass('\\', CharacterClass.RELEASE_CHARACTER));
    }

    @Test
    void testSetClassUTF8() {
        CharacterSet target = new CharacterSet();
        target.setClass('£', CharacterClass.RELEASE_CHARACTER);
        assertTrue(target.isCharacterClass('£', CharacterClass.RELEASE_CHARACTER));
    }

    @Test
    void testGetDelimiterValidClass() {
        CharacterSet target = new CharacterSet();
        target.setClass('`', CharacterClass.ELEMENT_REPEATER);
        assertEquals(CharacterClass.ELEMENT_REPEATER, target.getClass('`'));
    }

    @Test
    void testIsReleaseAscii() {
        CharacterSet target = new CharacterSet();
        target.setClass('\\', CharacterClass.RELEASE_CHARACTER);
        assertEquals(CharacterClass.RELEASE_CHARACTER, target.getClass('\\'));
    }

    @Test
    void testIsReleaseUTF8() {
        CharacterSet target = new CharacterSet();
        assertNotEquals(CharacterClass.RELEASE_CHARACTER, target.getClass('£'));
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
