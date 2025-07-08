package com.github.mstepan.template;

import com.github.mstepan.template.scopes.AtLeastOneCompleted;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

public class AppMain {

    public static void main(String[] args) throws Exception {

        try (AtLeastOneCompleted<Long> scope = new AtLeastOneCompleted<>()) {
            scope.fork(
                    new Task("Task-1", 1L, new IllegalStateException("Task-1 custom exception")));
            scope.fork(new Task("Task-2", 2L, null));
            scope.fork(new Task("Task-3", 3L, null));

            scope.join();
            scope.showExceptionsIfAny();

            System.out.printf("result: %d%n", scope.getResult());
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
