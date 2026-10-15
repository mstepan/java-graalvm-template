package com.github.mstepan.template.concurrent;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.jetbrains.lincheck.Lincheck;
import org.junit.jupiter.api.Test;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;

public class ConcurrentCounterTest {

    static class Counter {
        int value;

        static final VarHandle handleValue;

        static {
            try {
                handleValue =
                        MethodHandles.lookup().findVarHandle(Counter.class, "value", int.class);
            } catch (ReflectiveOperationException e) {
                throw new ExceptionInInitializerError(e);
            }
        }

        int inc() {
            return (int) handleValue.getAndAdd(this, 1) + 1;
        }

        int value() {
            return (int) handleValue.getAcquire(this);
        }
    }

    @Test
    void increment() {

        final Counter counter = new Counter();

        Lincheck.runConcurrentTest(
                () -> {
                    Thread t1 = Thread.ofPlatform().unstarted(counter::inc);
                    Thread t2 = Thread.ofPlatform().unstarted(counter::inc);

                    t1.start();
                    t2.start();

                    try {
                        t1.join();
                        t2.join();
                    } catch (InterruptedException ex) {
                        Thread.currentThread().interrupt();
                        throw new AssertionError("Thread interrupted unexpectedly", ex);
                    }

                    assertEquals(2, counter.value());
                });
    }
}
