package com.github.mstepan.template.scopes;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.StructuredTaskScope;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

/**
 * A StructuredTaskScope.Joiner that completes as soon as any subtask completes successfully.
 * Captures the first successful result (if any) and exposes it via {@link #result()}. Failed
 * subtasks are recorded for optional diagnostic printing via {@link #showExceptionsIfAny()}.
 *
 * <p>This joiner short-circuits: once a success is observed, {@link
 * #onComplete(StructuredTaskScope.Subtask)} returns {@code true} to allow the enclosing
 * StructuredTaskScope to stop joining early.
 *
 * <p>Thread-safety: uses AtomicReference for storing the first successful result and a concurrent
 * queue for exceptions.
 */
@SuppressWarnings({"preview", "unused"})
public class AtLeastOneCompleted<T> implements StructuredTaskScope.Joiner<T, Stream<T>> {

    private final AtomicReference<T> result = new AtomicReference<>();
    private final Queue<Throwable> exceptions = new ConcurrentLinkedQueue<>();

    /**
     * Called when a subtask completes. Records the first successful result if not already set.
     * Failed subtasks are collected for later inspection via {@link #showExceptionsIfAny()}.
     *
     * @param subtask the completed subtask
     * @return {@code true} if a successful result was captured and joining can stop; {@code false}
     *     otherwise
     */
    @Override
    public boolean onComplete(StructuredTaskScope.Subtask<? extends T> subtask) {
        if (subtask.state() == StructuredTaskScope.Subtask.State.SUCCESS) {
            result.compareAndExchange(null, subtask.get());
            return true;
        } else if (subtask.state() == StructuredTaskScope.Subtask.State.FAILED) {
            exceptions.add(subtask.exception());
        }
        return false;
    }

    /**
     * Returns a stream containing at most one element: the first successful result, if any.
     *
     * @return a stream with zero or one element; empty if no subtask completed successfully
     */
    @Override
    public Stream<T> result() {
        showExceptionsIfAny();
        final T resValue = result.get();
        return (resValue == null) ? Stream.empty() : Stream.of(resValue);
    }

    private void showExceptionsIfAny() {
        for (Throwable singleEx : exceptions) {
            System.err.printf("Exception: '%s'%n", singleEx.getMessage());
        }
    }
}
