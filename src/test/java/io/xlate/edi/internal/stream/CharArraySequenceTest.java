package io.xlate.edi.internal.stream;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.CharBuffer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CharArraySequenceTest {

    CharArraySequence target;

    @BeforeEach
    void setUp() {
        target = new CharArraySequence();
    }

    @Test
    void testClear() {
        char[] text = "ANYVALUEEXCEPTTHIS".toCharArray();
        int start = 3;
        int length = 5;
        target.set(text, start, length);
        assertEquals(5, target.length());
        assertEquals("VALUE", target.toString());
        target.clear();
        assertEquals("", target.toString());
    }

    @Test
    void testPutToBuffer() {
        char[] text = "ANYVALUEEXCEPTTHIS".toCharArray();
        int start = 3;
        int length = 5;
        target.set(text, start, length);
        CharBuffer buffer = CharBuffer.allocate(20);
        target.putToBuffer(buffer);
        buffer.flip();
        assertEquals("VALUE", buffer.toString());
    }

    @Test
    void testCharAt() {
        char[] text = "ANYVALUEEXCEPTTHIS".toCharArray();
        int start = 3;
        int length = 5;
        target.set(text, start, length);
        assertThrows(StringIndexOutOfBoundsException.class, () -> target.charAt(-1));
        assertEquals('V', target.charAt(0));
        assertEquals('E', target.charAt(4));
        assertThrows(StringIndexOutOfBoundsException.class, () -> target.charAt(5));
    }

    @Test
    void testSubSequence() {
        char[] text = "ANYVALUEEXCEPTTHIS".toCharArray();
        int start = 3;
        int length = 5;
        target.set(text, start, length);
        assertThrows(IndexOutOfBoundsException.class, () -> target.subSequence(-10, 3));
        assertEquals("VALUE", target.subSequence(0, 5).toString());
        assertEquals("VAL", target.subSequence(0, 3).toString());
        assertEquals("ALUE", target.subSequence(1, 5).toString());
        assertThrows(IndexOutOfBoundsException.class, () -> target.subSequence(0, 10));
        assertThrows(IndexOutOfBoundsException.class, () -> target.subSequence(6, 5));
    }

    @Test
    void testCompareTo() {
        char[] text = "ANYVALUEEXCEPTTHIS".toCharArray();
        int start = 3;
        int length = 5;
        target.set(text, start, length);
        assertEquals(0, target.compareTo("VALUE"));
        assertTrue(target.compareTo("VALUE1") < 0);
        assertTrue(target.compareTo("A") > 0);
        assertTrue(target.compareTo("Z") < 0);
    }

}
