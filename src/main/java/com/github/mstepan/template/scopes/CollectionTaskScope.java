package com.github.mstepan.template.scopes;

import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.StructuredTaskScope;
import java.util.stream.Stream;

/**
 * A StructuredTaskScope.Joiner that collects results from all successfully completed subtasks and
 * records failures for later inspection. This joiner never short-circuits; it allows all subtasks
 * to complete regardless of individual successes or failures.
 *
 * <p>The result of joining is a Stream of successful results. Any failures are available via the
 * {@link #exception()} method as a combined exception with individual causes suppressed.
 */
@SuppressWarnings({"preview", "unused"})
public final class CollectionTaskScope<T> implements StructuredTaskScope.Joiner<T, Stream<T>> {

    private final Queue<T> results = new LinkedBlockingQueue<>();
    private final Queue<Throwable> exceptions = new LinkedBlockingQueue<>();

    /**
     * Called when a subtask completes. If the subtask succeeded, its result is added to the
     * internal collection; if it failed, the throwable is recorded for later inspection.
     *
     * @param subtask the completed subtask
     * @return {@code false} always; this joiner does not stop early based on completion
     */
    @Override
    public boolean onComplete(StructuredTaskScope.Subtask<? extends T> subtask) {
        switch (subtask.state()) {
            case SUCCESS -> results.add(subtask.get());
            case FAILED -> exceptions.add(subtask.exception());
            case UNAVAILABLE -> throw new IllegalArgumentException("'UNAVAILABLE' subtask state");
        }
        return false;
    }

    /**
     * Returns a stream of all results that completed successfully. The order of elements is
     * unspecified and may reflect completion timing.
     *
     * @return a stream of successful results; may be empty
     */
    @Override
    public Stream<T> result() {
        return results.stream();
    }

    /**
     * Returns an Optional containing a combined RuntimeException if at least one subtask failed.
     * The combined exception includes each individual failure as a suppressed exception.
     *
     * @return an Optional with the combined exception when failures occurred; otherwise {@code
     *     Optional.empty()}
     */
    public Optional<Throwable> exception() {
        if (exceptions.isEmpty()) {
            return Optional.empty();
        }

        RuntimeException combinedEx =
                new RuntimeException("Not all subtask completed successfully");

        for (Throwable ex : exceptions) {
            combinedEx.addSuppressed(ex);
        }

        return Optional.of(combinedEx);
    }
}
