package com.github.mstepan.template.scopes;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.StructuredTaskScope;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

@Timeout(10)
@SuppressWarnings("preview")
class AtLeastOneCompletedTest {
    @Test
    void returnsEmptyWhenNoTasksSucceed() throws InterruptedException {
        var joiner = new AtLeastOneCompleted<String>();
        try (var scope = StructuredTaskScope.open(joiner)) {
            scope.fork(
                    () -> {
                        throw new IllegalArgumentException("failed");
                    });
            assertEquals(List.of(), scope.join().toList());
        }
    }

    @Test
    void firstSuccessCancelsUnfinishedTask() throws InterruptedException {
        var started = new CountDownLatch(1);
        var interrupted = new CountDownLatch(1);
        var joiner = new AtLeastOneCompleted<String>();
        try (var scope = StructuredTaskScope.open(joiner)) {
            var unfinished =
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
            assertFalse(joiner.onComplete(unfinished));
            scope.fork(() -> "first");
            assertEquals(List.of("first"), scope.join().toList());
            assertTrue(scope.isCancelled());
        }
        assertEquals(0L, interrupted.getCount());
    }

    @Test
    void keepsFirstSuccessfulResult() throws InterruptedException {
        var joiner = new AtLeastOneCompleted<String>();
        assertEquals(0L, joiner.result().count());
        try (var scope = StructuredTaskScope.open(StructuredTaskScope.Joiner.<String>awaitAll())) {
            var first = scope.fork(() -> "first");
            var second = scope.fork(() -> "second");
            scope.join();
            assertTrue(joiner.onComplete(first));
            assertTrue(joiner.onComplete(second));
            assertEquals(List.of("first"), joiner.result().toList());
        }
    }
}
