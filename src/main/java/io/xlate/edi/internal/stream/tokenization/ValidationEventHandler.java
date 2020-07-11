package io.xlate.edi.internal.stream.tokenization;

import io.xlate.edi.schema.EDIReference;
import io.xlate.edi.stream.EDIStreamEvent;
import io.xlate.edi.stream.EDIStreamValidationError;

public interface ValidationEventHandler {

    void loopBegin(EDIReference typeReference);

    void loopEnd(EDIReference typeReference);

    void segmentError(CharSequence token, EDIReference typeReference, EDIStreamValidationError error);

    void elementError(EDIStreamEvent event,
                      EDIStreamValidationError error,
                      EDIReference typeReference,
                      CharSequence text,
                      int element,
                      int component,
                      int repetition);

}
