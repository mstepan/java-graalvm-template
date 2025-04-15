package com.github.mstepan.template;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

public class AppMain {

    public static void main(String[] args) throws Exception {
        final int tasksCount = 10_000;

        Runnable ioTask =
                () -> {
                    try {
                        TimeUnit.MILLISECONDS.sleep(500L);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                };

        measureThroughput(
                "virtual", Executors.newVirtualThreadPerTaskExecutor(), tasksCount, ioTask);
//        measureThroughput("pool-100", Executors.newFixedThreadPool(100), tasksCount, ioTask);
//        measureThroughput("pool-250", Executors.newFixedThreadPool(250), tasksCount, ioTask);
        measureThroughput("pool-1000", Executors.newFixedThreadPool(1000), tasksCount, ioTask);

        System.out.println("Main done...");
    }

    private static void measureThroughput(
            String type, ExecutorService pool, int tasksCount, Runnable task) {
        Instant start = Instant.now();

        AtomicLong completedTasks = new AtomicLong();

        try (pool) {
            for (int i = 0; i < tasksCount; i++) {
                pool.execute(
                        () -> {
                            task.run();
                            completedTasks.incrementAndGet();
                        });
            }
        }

        Instant end = Instant.now();

        long durationInMs = Duration.between(start, end).toMillis();

        System.out.printf(
                "[%s], time: %d ms, throughput: %.1f RPS%n",
                type, durationInMs, (((double) completedTasks.get() * 1000.0) / durationInMs));
    }
}
