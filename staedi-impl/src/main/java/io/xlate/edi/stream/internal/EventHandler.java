package io.xlate.edi.stream.internal;

import java.io.InputStream;

public interface EventHandler {

	void interchangeBegin();

	void interchangeEnd();

	void loopBegin(CharSequence id);

	void loopEnd(CharSequence id);

	void segmentBegin(char[] text, int start, int length);

	void segmentEnd();

	void compositeBegin(boolean isNil);

	void compositeEnd(boolean isNil);

	void elementData(char[] text, int start, int length);

	void binaryData(InputStream binary);

	void segmentError(CharSequence token, int error);

	void elementError(int event, int error, int element, int component, int repetition);
}
