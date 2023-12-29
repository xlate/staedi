package io.xlate.edi.internal;

import java.util.function.Function;

/**
 * A {@link Runnable} that can throw exceptions/errors. Internal use only.
 *
 * @param <T>
 *            the type of exception that may be thrown
 *
 * @since 1.24
 */
public interface ThrowingRunnable<T extends Exception> {

    /**
     * Run any code provided by an implementation, including code that may throw
     * an exception.
     *
     * @throws T
     *             any exception thrown by the implementation of this method
     */
    void run() throws T;

    /**
     * Execute the provided task and apply the given exceptionWrapper function
     * on any exception thrown by it.
     *
     * @param <T>
     *            the type of exception that may be thrown
     * @param <E>
     *            the type of exception that this method may throw, wrapping any
     *            thrown by task
     * @param task
     *            runnable to execute that may thrown an exception T
     * @param exceptionWrapper
     *            wrapper function to wrap/convert an exception T to an
     *            exception E
     * @throws E
     *             exception thrown when task throws an exception T
     */
    static <T extends Exception, E extends Exception> void run(ThrowingRunnable<T> task, Function<T, E> exceptionWrapper) throws E {
        try {
            task.run();
        } catch (Exception e) {
            @SuppressWarnings("unchecked")
            T thrown = (T) e;
            throw exceptionWrapper.apply(thrown);
        }
    }
}
