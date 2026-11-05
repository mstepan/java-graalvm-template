package com.github.mstepan.template.concurrent;

import static org.junit.jupiter.api.Assertions.*;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.StructuredTaskScope;
import java.util.concurrent.StructuredTaskScope.Subtask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

@Timeout(10)
@SuppressWarnings("preview")
class AwaitAllSuccessfulOrFailWithRateLimiterJoinerTest {
    @Test
    void validatesPermitRangeIncludingBoundaries() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new AwaitAllSuccessfulOrFailWithRateLimiterJoiner<>(0));
        assertThrows(
                IllegalArgumentException.class,
                () -> new AwaitAllSuccessfulOrFailWithRateLimiterJoiner<>(-1));
        assertThrows(
                IllegalArgumentException.class,
                () -> new AwaitAllSuccessfulOrFailWithRateLimiterJoiner<>(10_001));
        assertDoesNotThrow(() -> new AwaitAllSuccessfulOrFailWithRateLimiterJoiner<>(1));
        assertDoesNotThrow(() -> new AwaitAllSuccessfulOrFailWithRateLimiterJoiner<>(10_000));
    }

    @Test
    void awaitsAllSuccessesWhileLimitingConcurrency() throws InterruptedException {
        var running = new AtomicInteger();
        var maximum = new AtomicInteger();
        var completed = new AtomicInteger();
        try (var scope =
                StructuredTaskScope.open(
                        new AwaitAllSuccessfulOrFailWithRateLimiterJoiner<String>(1))) {
            for (int index = 0; index < 5; ++index) {
                scope.fork(
                        () -> {
                            int count = running.incrementAndGet();
                            maximum.accumulateAndGet(count, Math::max);
                            try {
                                completed.incrementAndGet();
                                return "value";
                            } finally {
                                running.decrementAndGet();
                            }
                        });
            }
            assertNull(scope.join());
            assertFalse(scope.isCancelled());
        }
        assertEquals(5, completed.get());
        assertEquals(1, maximum.get());
    }

    @Test
    void failureCancelsRunningSiblingAndBecomesJoinCause() throws InterruptedException {
        var started = new CountDownLatch(1);
        var interrupted = new CountDownLatch(1);
        var expected = new IllegalArgumentException("failed");
        try (var scope =
                StructuredTaskScope.open(
                        new AwaitAllSuccessfulOrFailWithRateLimiterJoiner<String>(2))) {
            scope.fork(
                    () -> {
                        started.countDown();
                        try {
                            new CountDownLatch(1).await();
                        } catch (InterruptedException ex) {
                            interrupted.countDown();
                            throw ex;
                        }
                        return "unreachable";
                    });
            started.await();
            scope.fork(
                    () -> {
                        throw expected;
                    });
            var failure = assertThrows(StructuredTaskScope.FailedException.class, scope::join);
            assertSame(expected, failure.getCause());
            assertTrue(scope.isCancelled());
        }
        assertEquals(0L, interrupted.getCount());
    }

    @Test
    void preservesFirstFailureAndRejectsLaterForks() throws InterruptedException {
        var joiner = new AwaitAllSuccessfulOrFailWithRateLimiterJoiner<String>(1);
        var expected = new IllegalArgumentException("first");
        assertTrue(joiner.onComplete(failedTask(expected)));
        assertTrue(joiner.onComplete(failedTask(new IllegalStateException("second"))));
        assertTrue(joiner.onFork(failedTask(expected)));
        assertSame(expected, assertThrows(IllegalArgumentException.class, joiner::result));
    }

    @Test
    void rejectsUnavailableCompletion() throws InterruptedException {
        var joiner = new AwaitAllSuccessfulOrFailWithRateLimiterJoiner<String>(1);
        var release = new CountDownLatch(1);
        try (var scope = StructuredTaskScope.<String>open()) {
            var unfinished =
                    scope.fork(
                            () -> {
                                release.await();
                                return "value";
                            });
            try {
                assertThrows(IllegalStateException.class, () -> joiner.onComplete(unfinished));
            } finally {
                release.countDown();
            }
            scope.join();
        }
    }

    @Test
    void interruptionCancelsForkAndRestoresInterruptStatus() throws InterruptedException {
        var joiner = new AwaitAllSuccessfulOrFailWithRateLimiterJoiner<String>(1);
        var subtask = failedTask(new IllegalStateException());
        try {
            Thread.currentThread().interrupt();
            assertTrue(joiner.onFork(subtask));
            assertTrue(Thread.currentThread().isInterrupted());
            assertThrows(InterruptedException.class, joiner::result);
        } finally {
            Thread.interrupted();
        }
    }

    @Test
    void failureWhileWaitingForPermitCancelsWaitingFork() throws InterruptedException {
        var joiner = new AwaitAllSuccessfulOrFailWithRateLimiterJoiner<String>(1);
        var expected = new IllegalStateException("failed while waiting");
        var unfinished = failedTask(expected);
        assertFalse(joiner.onFork(unfinished));
        var cancelled = new AtomicBoolean();
        Thread waiter = Thread.ofVirtual().start(() -> cancelled.set(joiner.onFork(unfinished)));
        try {
            long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(2);
            while (waiter.getState() != Thread.State.WAITING && System.nanoTime() < deadline) {
                Thread.onSpinWait();
            }
            assertEquals(Thread.State.WAITING, waiter.getState());
            assertTrue(joiner.onComplete(failedTask(expected)));
            waiter.join();
            assertTrue(cancelled.get());
            assertSame(expected, assertThrows(IllegalStateException.class, joiner::result));
        } finally {
            waiter.interrupt();
            waiter.join();
        }
    }

    private static Subtask<String> failedTask(RuntimeException failure)
            throws InterruptedException {
        try (var scope = StructuredTaskScope.open(StructuredTaskScope.Joiner.<String>awaitAll())) {
            var task =
                    scope.fork(
                            () -> {
                                throw failure;
                            });
            scope.join();
            return task;
        }
    }
}
