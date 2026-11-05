package com.github.mstepan.template.concurrent;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Modifier;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.StructuredTaskScope;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;

class LocalTicksCounterTest {

    @Test
    void defaultCounterStartsAtOne() {
        assertEquals(1, new LocalTicksCounter().tick());
    }

    @Test
    void rejectsNegativeInitialCounter() {
        assertThrows(IllegalArgumentException.class, () -> new LocalTicksCounter(() -> 0L, -1));
    }

    @Test
    void concurrentTicksHaveUniqueSequenceNumbers() throws InterruptedException {
        LocalTicksCounter counter = new LocalTicksCounter(() -> 0L);
        Set<Short> results = ConcurrentHashMap.newKeySet();
        try (var scope = StructuredTaskScope.open()) {
            for (int worker = 0; worker < 8; ++worker) {
                scope.fork(
                        () -> {
                            for (int tick = 0; tick < 100; ++tick) {
                                assertTrue(results.add(counter.tick()));
                            }
                        });
            }
            scope.join();
        }
        assertEquals(800, results.size());
        assertEquals(801, counter.tick());
    }

    @Test
    void firstTickReturnsOne() {
        AtomicLong currentTime = new AtomicLong(1_000L);
        LocalTicksCounter counter = new LocalTicksCounter(currentTime::get);

        short firstTick = counter.tick();

        assertEquals(1, firstTick);
    }

    @Test
    void tickIsNotSynchronized() throws NoSuchMethodException {
        int modifiers = LocalTicksCounter.class.getDeclaredMethod("tick").getModifiers();

        assertFalse(Modifier.isSynchronized(modifiers));
    }

    @Test
    void tickAfterOneSecondResetIsCounted() {
        AtomicLong currentTime = new AtomicLong(1_000L);
        LocalTicksCounter counter = new LocalTicksCounter(currentTime::get);

        assertEquals(1, counter.tick());
        assertEquals(2, counter.tick());

        currentTime.addAndGet(1_000_000_000L);

        assertEquals(1, counter.tick());
    }

    @Test
    void throwsWhenCounterOverflowsWithinOneSecond() {
        AtomicLong currentTime = new AtomicLong(1_000L);
        LocalTicksCounter counter = new LocalTicksCounter(currentTime::get, Short.MAX_VALUE);

        assertThrows(IllegalStateException.class, counter::tick);
    }

    @Test
    void rejectsInitialCounterGreaterThanShortMaxValue() {
        AtomicLong currentTime = new AtomicLong(1_000L);

        assertThrows(
                IllegalArgumentException.class,
                () -> new LocalTicksCounter(currentTime::get, Short.MAX_VALUE + 1));
    }
}
