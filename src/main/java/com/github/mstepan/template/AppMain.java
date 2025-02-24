package com.github.mstepan.template;

import java.util.concurrent.StructuredTaskScope;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class AppMain {

    public static void main(String[] args) {

        final ReadWriteLock rwLock = new ReentrantReadWriteLock();
        final AtomicInteger threadsCount = new AtomicInteger(0);

        try (var scope = new NeverShutdown()) {

            for (int i = 0; i < 100_000; ++i) {
                scope.fork(
                        () -> {
                            rwLock.readLock().lock();
                            try {
                                threadsCount.incrementAndGet();
                                TimeUnit.SECONDS.sleep(1);
                            } finally {
                                rwLock.readLock().unlock();
                            }

                            return true;
                        });
            }

            scope.join().printStats();

            System.out.printf(
                    "threadsCount acquired read lock from StampedLock: %d%n", threadsCount.get());
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }

        System.out.println("Main done...");
    }

    /**
     * Custom implementation for a StructuredTaskScope that tracks count of successful, failed and
     * undefined tasks.
     */
    private static final class NeverShutdown<T> extends StructuredTaskScope<T> {

        private final AtomicInteger failedCnt = new AtomicInteger(0);
        private final AtomicInteger succeededCnt = new AtomicInteger(0);
        private final AtomicInteger undefinedCnt = new AtomicInteger(0);

        @Override
        protected void handleComplete(Subtask<? extends T> subtask) {
            switch (subtask.state()) {
                case SUCCESS -> succeededCnt.incrementAndGet();
                case FAILED -> failedCnt.incrementAndGet();
                default -> undefinedCnt.incrementAndGet();
            }
        }

        @Override
        public NeverShutdown<T> join() throws InterruptedException {
            super.join();
            return this;
        }

        public void printStats() {
            System.out.printf("Succeeded: %d%n", succeededCnt.get());
            System.out.printf("Failed: %d%n", failedCnt.get());
            System.out.printf("Undefined: %d%n", undefinedCnt.get());
        }
    }
}
