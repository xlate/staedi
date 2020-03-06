package io.xlate.edi.internal.stream.tokenization;

import java.io.InputStream;

public interface ElementDataHandler {

    boolean elementData(char[] text, int start, int length);

    boolean binaryData(InputStream binary);

}
