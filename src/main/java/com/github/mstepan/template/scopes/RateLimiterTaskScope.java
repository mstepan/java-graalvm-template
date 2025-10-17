package com.github.mstepan.template.scopes;

import java.util.concurrent.Semaphore;
import java.util.concurrent.StructuredTaskScope;
import java.util.stream.Stream;

/**
 * Joiner that limits the number of concurrently running subtasks using a semaphore. A permit is
 * acquired in {@link #onFork(StructuredTaskScope.Subtask)} before delegating to the default
 * behavior and released in {@link #onComplete(StructuredTaskScope.Subtask)}, capping parallelism to
 * the number of available permits.
 *
 * @param <T> the subtask result type
 * @param semaphore the semaphore that controls the maximum number of concurrent subtasks
 * @implNote Uses Java Structured Concurrency (preview) APIs.
 */
@SuppressWarnings({"preview", "unused"})
public record RateLimiterTaskScope<T>(Semaphore semaphore)
        implements StructuredTaskScope.Joiner<T, Stream<T>> {

    /**
     * Acquires a permit before forking the given subtask and delegates to the default Joiner
     * behavior.
     *
     * <p>If interrupted while waiting for a permit, the interrupt status is restored and a
     * RuntimeException is thrown.
     *
     * @param subtask the subtask about to be forked
     * @return true if the subtask should be started
     * @throws RuntimeException if interrupted while acquiring a permit
     */
    @Override
    public boolean onFork(StructuredTaskScope.Subtask<? extends T> subtask) {
        try {
            semaphore.acquire();
            return StructuredTaskScope.Joiner.super.onFork(subtask);
        } catch (InterruptedException interEx) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(interEx);
        }
    }

    /**
     * Releases the acquired permit after delegating to the default completion behavior.
     *
     * @param subtask the subtask that has just completed
     * @return the value returned by the default implementation
     */
    @Override
    public boolean onComplete(StructuredTaskScope.Subtask<? extends T> subtask) {
        try {
            return StructuredTaskScope.Joiner.super.onComplete(subtask);
        } finally {
            semaphore.release();
        }
    }

    /**
     * This joiner only throttles concurrency and does not aggregate results.
     *
     * @return an empty stream
     */
    @Override
    public Stream<T> result() {
        return Stream.empty();
    }
}
