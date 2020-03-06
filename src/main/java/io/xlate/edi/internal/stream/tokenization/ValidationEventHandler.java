package io.xlate.edi.internal.stream.tokenization;

import io.xlate.edi.stream.EDIStreamEvent;
import io.xlate.edi.stream.EDIStreamValidationError;

public interface ValidationEventHandler {

    void loopBegin(CharSequence id);

    void loopEnd(CharSequence id);

    void segmentError(CharSequence token, EDIStreamValidationError error);

    void elementError(EDIStreamEvent event,
                      EDIStreamValidationError error,
                      CharSequence referenceCode,
                      int element,
                      int component,
                      int repetition);

}
