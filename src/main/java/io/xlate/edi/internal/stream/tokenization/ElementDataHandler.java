package io.xlate.edi.internal.stream.tokenization;

import java.io.InputStream;

public interface ElementDataHandler {

    boolean elementData(CharSequence text, boolean fromStream);

    boolean binaryData(InputStream binary);

}
