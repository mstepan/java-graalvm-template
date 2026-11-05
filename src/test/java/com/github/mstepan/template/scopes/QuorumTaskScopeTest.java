package com.github.mstepan.template.scopes;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.StructuredTaskScope;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

@Timeout(10)
@SuppressWarnings("preview")
class QuorumTaskScopeTest {
    @Test
    void requiresPositiveQuorum() {
        assertThrows(IllegalArgumentException.class, () -> new QuorumTaskScope(0));
        assertThrows(IllegalArgumentException.class, () -> new QuorumTaskScope(-1));
    }

    @Test
    void reportsUnmetQuorumAfterFailure() throws InterruptedException {
        var joiner = new QuorumTaskScope(2);
        assertEquals(List.of(false), joiner.result().toList());
        try (var scope = StructuredTaskScope.open(joiner)) {
            scope.fork(
                    () -> {
                        throw new IllegalStateException("failed");
                    });
            scope.fork(() -> {});
            assertEquals(List.of(false), scope.join().toList());
            assertFalse(scope.isCancelled());
        }
    }

    @Test
    void reachingQuorumCancelsRemainingTasks() throws InterruptedException {
        var started = new CountDownLatch(1);
        var interrupted = new CountDownLatch(1);
        var joiner = new QuorumTaskScope(1);
        try (var scope = StructuredTaskScope.open(joiner)) {
            scope.fork(
                    () -> {
                        started.countDown();
                        try {
                            new CountDownLatch(1).await();
                        } catch (InterruptedException ex) {
                            interrupted.countDown();
                            throw ex;
                        }
                        return null;
                    });
            started.await();
            scope.fork(() -> {});
            assertEquals(List.of(true), scope.join().toList());
            assertTrue(scope.isCancelled());
        }
        assertEquals(0L, interrupted.getCount());
    }
}
