package io.xlate.edi.internal.stream.validation;

import java.util.function.BiConsumer;

import io.xlate.edi.stream.EDIStreamValidationError;

class UsageError {
    String code;
    EDIStreamValidationError error;
    int depth;

    UsageError(String code, EDIStreamValidationError error, int depth) {
        super();
        this.code = code;
        this.error = error;
        this.depth = depth;
    }

    void handle(BiConsumer<String, EDIStreamValidationError> handler) {
        handler.accept(code, error);
    }
}
