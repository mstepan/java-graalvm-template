package com.github.mstepan.template.concurrent;

import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.StructuredTaskScope;
import java.util.concurrent.atomic.AtomicReference;

@SuppressWarnings("preview")
public final class AllSuccessfulOrFailWithRateLimiterJoiner<T>
        implements StructuredTaskScope.Joiner<T, List<T>> {

    private static final int MAX_ALLOWED_COUNT = 10_000;

    private final Semaphore semaphore;
    private final AtomicReference<Throwable> firstFailure;

    public AllSuccessfulOrFailWithRateLimiterJoiner(int allowedCount) {
        if (allowedCount <= 0 || allowedCount > MAX_ALLOWED_COUNT) {
            throw new IllegalArgumentException(
                    "Semaphore allowed count should be in range [1...%d]"
                            .formatted(MAX_ALLOWED_COUNT));
        }
        this.semaphore = new Semaphore(allowedCount);
        this.firstFailure = new AtomicReference<>();
    }

    @Override
    public boolean onFork(StructuredTaskScope.Subtask<? extends T> subtask) {
        if (firstFailure.get() != null) {
            return true;
        }

        try {
            semaphore.acquire();
        } catch (InterruptedException interEx) {
            Thread.currentThread().interrupt();
            return true;
        }

        return false;
    }

    @Override
    public boolean onComplete(StructuredTaskScope.Subtask<? extends T> subtask) {
        try {
            switch (subtask.state()) {
                case FAILED -> {
                    firstFailure.compareAndSet(null, subtask.exception());
                    return true;
                }
                case UNAVAILABLE ->
                        throw new IllegalStateException("Subtask result is unavailable");
            }

            return false;
        } finally {
            semaphore.release();
        }
    }

    @Override
    public List<T> result() throws Throwable {
        Throwable failure = firstFailure.get();
        if (failure != null) {
            throw failure;
        }
        return null;
    }
}
