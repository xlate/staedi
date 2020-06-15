/*******************************************************************************
 * Copyright 2020 xlate.io LLC, http://www.xlate.io
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 ******************************************************************************/
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

    public char[] getText() {
        return text;
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
    @SuppressWarnings("java:S1210") // equals & hashCode are not used with this class
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
        return (length > 0) ? new String(text, start, length) : "";
    }
}