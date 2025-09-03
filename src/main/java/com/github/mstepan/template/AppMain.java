package com.github.mstepan.template;

import com.github.mstepan.template.lock.BrokenSpinLockCounter;
import java.util.concurrent.StructuredTaskScope;

public class AppMain {

    @SuppressWarnings("preview")
    public static void main(String[] args) throws Exception {

        BrokenSpinLockCounter lockCounter = new BrokenSpinLockCounter();

        try (var scope = new StructuredTaskScope.ShutdownOnFailure()) {

            for (int threadsIdx = 0; threadsIdx < 100; ++threadsIdx) {
                scope.fork(
                        () -> {
                            for (int it = 0; it < 1000; ++it) {
                                lockCounter.incrementBroken();
                            }
                            return null;
                        });
            }

            scope.join();
            scope.throwIfFailed();

            System.out.printf("Counter value: %d%n", lockCounter.count());
        }

        System.out.printf("Java version: %s. Main done...%n", System.getProperty("java.version"));
    }
}
