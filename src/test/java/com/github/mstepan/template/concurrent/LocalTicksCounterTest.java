package com.github.mstepan.template.concurrent;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Modifier;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;

class LocalTicksCounterTest {

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
