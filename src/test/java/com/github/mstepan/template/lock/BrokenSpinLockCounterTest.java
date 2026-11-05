package com.github.mstepan.template.lock;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

class BrokenSpinLockCounterTest {

    @Test
    void bothVariantsIncrementSequentially() {
        var counter = new BrokenSpinLockCounter();
        assertEquals(0, counter.count());
        counter.increment();
        counter.incrementBroken();
        counter.incrementBroken();
        assertEquals(3, counter.count());
    }

    @Test
    @Timeout(10)
    void acquireReleaseVariantPreservesConcurrentIncrements() throws Exception {
        var counter = new BrokenSpinLockCounter();
        try (var executor = Executors.newFixedThreadPool(4)) {
            var tasks = new ArrayList<Future<?>>();
            for (int worker = 0; worker < 4; worker++) {
                tasks.add(
                        executor.submit(
                                () -> {
                                    for (int i = 0; i < 1_000; i++) {
                                        counter.increment();
                                    }
                                }));
            }
            for (Future<?> task : tasks) {
                task.get();
            }
        }
        assertEquals(4_000, counter.count());
    }
}
