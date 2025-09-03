package com.github.mstepan.template.lock;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;

public class BrokenSpinLockCounter {

    private boolean locked;
    private volatile int count;

    private static final VarHandle LOCK_HANDLER;

    static {
        try {
            LOCK_HANDLER =
                    MethodHandles.lookup()
                            .in(BrokenSpinLockCounter.class)
                            .findVarHandle(BrokenSpinLockCounter.class, "locked", boolean.class);

        } catch (NoSuchFieldException | IllegalAccessException ex) {
            throw new ExceptionInInitializerError(ex);
        }
    }

    @SuppressFBWarnings(
            value = {"VO_VOLATILE_INCREMENT", "AT_NONATOMIC_OPERATIONS_ON_SHARED_VARIABLE"})
    public void increment() {
        while (!LOCK_HANDLER.weakCompareAndSetAcquire(this, false, true)) {}

        // below code execute by single thread as critical section
        count = count + 1;

        LOCK_HANDLER.setRelease(this, false);
    }

    /**
     * This method should fail on ARM multiprocessor architecture, such as MacOS M1/M2 but will work
     * at x86-64 architecture.
     */
    @SuppressFBWarnings(
            value = {"VO_VOLATILE_INCREMENT", "AT_NONATOMIC_OPERATIONS_ON_SHARED_VARIABLE"})
    public void incrementBroken() {
        while (!LOCK_HANDLER.weakCompareAndSet(this, false, true)) {}

        // below code execute by single thread as critical section
        count = count + 1;

        LOCK_HANDLER.setOpaque(this, false);
    }

    public int count() {
        return count;
    }
}
