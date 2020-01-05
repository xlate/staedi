package io.xlate.edi.internal.stream;

import java.nio.CharBuffer;

public class CharArraySequence implements CharSequence, Comparable<CharSequence> {
    private static final char[] EMPTY = {};

    private char[] text = EMPTY;
    private int start = 0;
    private int length = 0;

    public void set(char[] text, int start, int length) {
        this.text = text;
        this.start = start;
        this.length = length;
    }

    public void clear() {
        this.text = EMPTY;
        this.start = 0;
        this.length = 0;
    }

    public void putToBuffer(CharBuffer buffer) {
        buffer.put(text, start, length);
    }

    @Override
    public int length() {
        return length;
    }

    @Override
    public char charAt(int index) {
        if (index < 0 || index >= length) {
            throw new StringIndexOutOfBoundsException(index);
        }

        return text[start + index];
    }

    @Override
    public CharSequence subSequence(int start, int end) {
        if (start < 0) {
            throw new IndexOutOfBoundsException(Integer.toString(start));
        }
        if (end > length) {
            throw new IndexOutOfBoundsException(Integer.toString(end));
        }
        if (start > end) {
            throw new IndexOutOfBoundsException(Integer.toString(end - start));
        }
        return ((start == 0) && (end == length))
                ? this
                : new String(text, this.start + start, end - start);
    }

    @Override
    public int compareTo(CharSequence other) {
        int len1 = length;
        int len2 = other.length();
        int n = Math.min(len1, len2);
        char[] v1 = text;
        int i = start;
        int j = 0;

        while (n-- != 0) {
            char c1 = v1[i++];
            char c2 = other.charAt(j++);
            if (c1 != c2) {
                return c1 - c2;
            }
        }

        return len1 - len2;
    }

    @Override
    public String toString() {
        return new String(text, start, length);
    }
}