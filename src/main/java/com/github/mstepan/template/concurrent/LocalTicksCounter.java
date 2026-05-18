package com.github.mstepan.template.concurrent;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.LongSupplier;

public final class LocalTicksCounter {

    private static final long ONE_SECOND_NANOS = 1_000_000_000L;

    private final LongSupplier nanoTimeSource;
    private final AtomicReference<State> state;

    public LocalTicksCounter() {
        this(System::nanoTime);
    }

    LocalTicksCounter(LongSupplier nanoTimeSource) {
        this(nanoTimeSource, 0);
    }

    LocalTicksCounter(LongSupplier nanoTimeSource, int initialCounter) {
        if (initialCounter < 0 || initialCounter > Short.MAX_VALUE) {
            throw new IllegalArgumentException(
                    "initialCounter should be between 0 and Short.MAX_VALUE");
        }

        this.nanoTimeSource = Objects.requireNonNull(nanoTimeSource);
        this.state =
                new AtomicReference<>(
                        new State(nanoTimeSource.getAsLong(), (short) initialCounter));
    }

    public short tick() {
        while (true) {
            final State curState = state.get();
            final long curTime = nanoTimeSource.getAsLong();

            State newState;
            if (curTime - curState.windowStartNanos >= ONE_SECOND_NANOS) {
                newState = new State(curTime, (short) 1);
            } else {
                if (curState.counter == Short.MAX_VALUE) {
                    if (state.compareAndSet(curState, curState)) {
                        throw new IllegalStateException("Too many ticks in one second");
                    }

                    Thread.onSpinWait();
                    continue;
                }

                newState = new State(curState.windowStartNanos, (short) (curState.counter + 1));
            }

            if (state.compareAndSet(curState, newState)) {
                return newState.counter;
            }

            Thread.onSpinWait();
        }
    }

    private record State(long windowStartNanos, short counter) {}
}
