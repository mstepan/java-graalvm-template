package com.github.mstepan.template.scopes;

import java.util.Collection;
import java.util.Collections;
import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.StructuredTaskScope;

public final class CollectionTaskScope<T> extends StructuredTaskScope<T> {

    private final Queue<T> results = new LinkedBlockingQueue<>();
    private final Queue<Throwable> exceptions = new LinkedBlockingQueue<>();

    @Override
    protected void handleComplete(Subtask<? extends T> subtask) {
        switch (subtask.state()) {
            case SUCCESS -> {
                results.add(subtask.get());
            }
            case FAILED -> {
                exceptions.add(subtask.exception());
            }
            case UNAVAILABLE -> {
                throw new IllegalArgumentException("'UNAVAILABLE' subtask state");
            }
        }
    }

    public Collection<T> getResults() {
        ensureOwnerAndJoined();
        return Collections.unmodifiableCollection(results);
    }

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
