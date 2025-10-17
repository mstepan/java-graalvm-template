package com.github.mstepan.template.scopes;

import java.util.concurrent.Semaphore;
import java.util.concurrent.StructuredTaskScope;
import java.util.stream.Stream;

/**
 * A {@link java.util.concurrent.StructuredTaskScope.Joiner} that throttles the number of
 * concurrently running subtasks using a {@link java.util.concurrent.Semaphore}.
 *
 * <p>Each time a subtask is forked, a permit is acquired; the permit is released when the subtask
 * completes (whether normally or exceptionally). This effectively limits the degree of concurrency
 * to the number of available permits.
 *
 * <p>This joiner does not aggregate results; its {@link #result()} always returns an empty stream.
 *
 * <p>Thread-safety: Instances are safe for concurrent use by {@link
 * java.util.concurrent.StructuredTaskScope}.
 *
 * @param <T> the result type of the subtasks
 * @see java.util.concurrent.Semaphore
 * @see java.util.concurrent.StructuredTaskScope.Joiner
 */
@SuppressWarnings({"preview", "unused"})
public final class RateLimiterTaskScope<T> implements StructuredTaskScope.Joiner<T, Stream<T>> {

    private final Semaphore semaphore;

    /**
     * Creates a rate-limiting joiner with the given number of permits.
     *
     * @param permissionsCount the maximum number of subtasks allowed to run concurrently; must be >
     *     0
     * @throws IllegalArgumentException if {@code permissionsCount <= 0}
     */
    public RateLimiterTaskScope(int permissionsCount) {
        if (permissionsCount <= 0) {
            throw new IllegalArgumentException(
                    String.format(
                            "'permissionsCount' should be positive value,"
                                    + "permissionsCount = %d",
                            permissionsCount));
        }
        this.semaphore = new Semaphore(permissionsCount);
    }

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
