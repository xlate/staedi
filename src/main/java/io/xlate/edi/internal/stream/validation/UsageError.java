package io.xlate.edi.internal.stream.validation;

import java.util.function.BiConsumer;

import io.xlate.edi.stream.EDIStreamValidationError;

public class UsageError {
    final String code;
    final EDIStreamValidationError error;
    final int depth;

    UsageError(String code, EDIStreamValidationError error, int depth) {
        super();
        this.code = code;
        this.error = error;
        this.depth = depth;
    }

    UsageError(EDIStreamValidationError error) {
        super();
        this.error = error;
        this.code = null;
        this.depth = -1;
    }

    UsageError(UsageNode node, EDIStreamValidationError error) {
        super();
        this.code = node.getCode();
        this.depth = node.getDepth();
        this.error = error;
    }

    void handle(BiConsumer<String, EDIStreamValidationError> handler) {
        handler.accept(code, error);
    }

    public String getCode() {
        return code;
    }

    public EDIStreamValidationError getError() {
        return error;
    }

    public int getDepth() {
        return depth;
    }
}
