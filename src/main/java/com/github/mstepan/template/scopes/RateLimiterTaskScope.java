package com.github.mstepan.template.scopes;

import java.util.concurrent.Callable;
import java.util.concurrent.Semaphore;
import java.util.concurrent.StructuredTaskScope;

public class RateLimiterTaskScope extends StructuredTaskScope<Void> {
    private final Semaphore semaphore;

    public RateLimiterTaskScope(int permissionsCount) {
        super("RateLimiterTaskScope", Thread.ofVirtual().factory());
        this.semaphore = new Semaphore(permissionsCount);
    }

    @Override
    public <U extends Void> Subtask<U> fork(Callable<? extends U> task) {
        try {
            semaphore.acquire();
            return super.fork(task);
        } catch (InterruptedException interEx) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(interEx);
        }
    }

    @Override
    protected void handleComplete(Subtask<? extends Void> subtask) {
        try {
            super.handleComplete(subtask);
        } finally {
            semaphore.release();
        }
    }
}
