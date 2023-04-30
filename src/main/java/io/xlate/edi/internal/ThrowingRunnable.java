package io.xlate.edi.internal;

import java.util.function.Function;

/**
 * A {@link Runnable} that can throw exceptions/errors. Internal use only.
 *
 * @since 1.24
 */
public interface ThrowingRunnable<T extends Exception> {

    void run() throws T;

    @SuppressWarnings("unchecked")
    static <T extends Exception, E extends Exception> void run(ThrowingRunnable<T> task, Function<T, E> exceptionWrapper) throws E {
        try {
            task.run();
        } catch (Exception e) {
            throw exceptionWrapper.apply((T) e);
        }
    }
}
