package com.github.mstepan.template.scopes;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.StructuredTaskScope;
import java.util.concurrent.atomic.AtomicReference;

public class AtLeastOneCompleted<T> extends StructuredTaskScope<T> {

    private final AtomicReference<T> result = new AtomicReference<>();

    private final Queue<Throwable> exceptions = new ConcurrentLinkedQueue<>();

    public AtLeastOneCompleted() {
        this("AtLeastOneCompleted-scope");
    }

    public AtLeastOneCompleted(String name) {
        super(name, Thread.ofVirtual().factory());
    }

    @Override
    protected void handleComplete(Subtask<? extends T> subtask) {
        if (subtask.state() == Subtask.State.SUCCESS) {
            result.compareAndExchange(null, subtask.get());
            shutdown();
        } else if (subtask.state() == Subtask.State.FAILED) {
            exceptions.add(subtask.exception());
        }
    }

    public T getResult() {
        ensureOwnerAndJoined();
        return result.get();
    }

    public void showExceptionsIfAny() {
        ensureOwnerAndJoined();
        for (Throwable singleEx : exceptions) {
            System.err.printf("Exception: '%s'%n", singleEx.getMessage());
        }
    }
}
