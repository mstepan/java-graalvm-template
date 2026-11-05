package com.github.mstepan.template.scopes;

import static org.junit.jupiter.api.Assertions.*;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.StructuredTaskScope;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

@Timeout(10)
@SuppressWarnings("preview")
class RateLimiterTaskScopeTest {
    @Test
    void requiresPositivePermitCount() {
        assertThrows(IllegalArgumentException.class, () -> new RateLimiterTaskScope<>(0));
        assertThrows(IllegalArgumentException.class, () -> new RateLimiterTaskScope<>(-1));
    }

    @Test
    void limitsConcurrencyAndReleasesPermitAfterFailure() throws InterruptedException {
        var running = new AtomicInteger();
        var maximum = new AtomicInteger();
        try (var scope = StructuredTaskScope.open(new RateLimiterTaskScope<String>(1))) {
            for (int index = 0; index < 5; ++index) {
                final int taskIndex = index;
                scope.fork(
                        () -> {
                            int count = running.incrementAndGet();
                            maximum.accumulateAndGet(count, Math::max);
                            try {
                                if (taskIndex == 0) {
                                    throw new IllegalStateException("failed");
                                }
                                return "value";
                            } finally {
                                running.decrementAndGet();
                            }
                        });
            }
            assertEquals(0L, scope.join().count());
            assertFalse(scope.isCancelled());
        }
        assertEquals(1, maximum.get());
        assertEquals(0, running.get());
    }

    @Test
    void interruptedAcquisitionRestoresInterruptStatus() throws InterruptedException {
        var release = new CountDownLatch(1);
        var joiner = new RateLimiterTaskScope<String>(1);
        try (var scope = StructuredTaskScope.<String>open()) {
            var unfinished =
                    scope.fork(
                            () -> {
                                release.await();
                                return "value";
                            });
            try {
                Thread.currentThread().interrupt();
                var failure =
                        assertThrows(IllegalStateException.class, () -> joiner.onFork(unfinished));
                assertInstanceOf(InterruptedException.class, failure.getCause());
                assertTrue(Thread.currentThread().isInterrupted());
            } finally {
                Thread.interrupted();
                release.countDown();
            }
            scope.join();
        }
    }

    @Test
    void releasesPermitWhenCompletionCallbackRejectsUnavailableTask() throws InterruptedException {
        var release = new CountDownLatch(1);
        var joiner = new RateLimiterTaskScope<String>(1);
        try (var scope = StructuredTaskScope.<String>open()) {
            var unfinished =
                    scope.fork(
                            () -> {
                                release.await();
                                return "value";
                            });
            try {
                assertFalse(joiner.onFork(unfinished));
                assertThrows(IllegalArgumentException.class, () -> joiner.onComplete(unfinished));
                assertFalse(joiner.onFork(unfinished));
            } finally {
                release.countDown();
            }
            scope.join();
        }
    }
}
