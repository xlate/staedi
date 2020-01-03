package io.xlate.edi.internal.stream.tokenization;

import java.io.InputStream;

public interface ElementDataHandler {

    void elementData(char[] text, int start, int length);

    void binaryData(InputStream binary);

}
