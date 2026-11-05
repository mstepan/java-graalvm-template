package com.github.mstepan.template.scopes;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.StructuredTaskScope;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

@Timeout(10)
@SuppressWarnings("preview")
class CollectionTaskScopeTest {
    @Test
    void collectsSuccessesAndAllFailuresWithoutCancelling() throws InterruptedException {
        var joiner = new CollectionTaskScope<String>();
        var first = new IllegalArgumentException("first");
        var second = new IllegalStateException("second");
        assertTrue(joiner.exception().isEmpty());
        try (var scope = StructuredTaskScope.open(joiner)) {
            scope.fork(() -> "value");
            scope.fork(
                    () -> {
                        throw first;
                    });
            scope.fork(
                    () -> {
                        throw second;
                    });
            assertEquals(List.of("value"), scope.join().toList());
            assertFalse(scope.isCancelled());
        }
        var combined = joiner.exception().orElseThrow();
        assertEquals("Not all subtask completed successfully", combined.getMessage());
        assertEquals(2, combined.getSuppressed().length);
        assertTrue(List.of(combined.getSuppressed()).containsAll(List.of(first, second)));
    }

    @Test
    void rejectsUnavailableSubtask() throws InterruptedException {
        var release = new CountDownLatch(1);
        var joiner = new CollectionTaskScope<String>();
        try (var scope = StructuredTaskScope.<String>open()) {
            var unfinished =
                    scope.fork(
                            () -> {
                                release.await();
                                return "value";
                            });
            try {
                assertThrows(IllegalArgumentException.class, () -> joiner.onComplete(unfinished));
            } finally {
                release.countDown();
            }
            scope.join();
        }
        assertEquals(0L, joiner.result().count());
    }
}
