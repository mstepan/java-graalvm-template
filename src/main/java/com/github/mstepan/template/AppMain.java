package com.github.mstepan.template;

import java.time.Instant;
import java.util.concurrent.Callable;
import java.util.concurrent.StructuredTaskScope;
import java.util.concurrent.TimeUnit;

public class AppMain {

    public static void main(String[] args) throws Exception {
        try (var scope = new StructuredTaskScope.ShutdownOnSuccess<String>()) {
            scope.fork(
                    () -> {
                        try (var outerScope = new StructuredTaskScope.ShutdownOnSuccess<String>()) {
                            outerScope.fork(
                                    () -> {
                                        TimeUnit.MILLISECONDS.sleep(500L);
                                        return "hello from 1";
                                    });

                            outerScope.joinUntil(Instant.now().plusSeconds(1L));
                            return outerScope.result();
                        }
                    });

            scope.fork(
                    () -> {
                        TimeUnit.SECONDS.sleep(3L);
                        return "hello from 2";
                    });

            scope.join();

            System.out.printf("result: %s%n", scope.result());
        }

        System.out.printf("Java version: %s. Main done...%n", System.getProperty("java.version"));
    }

    static class Task implements Callable<Long> {

        private final String taskName;
        private final long sleepDelay;
        private final Exception customException;

        public Task(String taskName, long sleepDelay, Exception customException) {
            this.taskName = taskName;
            this.sleepDelay = sleepDelay;
            this.customException = customException;
        }

        @Override
        public Long call() throws Exception {
            try {
                TimeUnit.SECONDS.sleep(sleepDelay);
                if (customException != null) {
                    throw customException;
                }
            } catch (InterruptedException interEx) {
                Thread.currentThread().interrupt();
                System.out.printf("%s interrupted%n", taskName);
            }
            return sleepDelay;
        }
    }
}
