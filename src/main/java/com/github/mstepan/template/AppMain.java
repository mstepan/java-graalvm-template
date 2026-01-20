package com.github.mstepan.template;

import com.github.mstepan.template.failire_detector.PhiAccrualFailureDetector;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

public class AppMain {


    //    @SuppressWarnings("preview")
    static void main() {

        PhiAccrualFailureDetector phiAccrualFailureDetector = new PhiAccrualFailureDetector();

        double[] delays = {1.03, 1.1, 1.2, 1.3, 1.5, 1.7, 2.0};

        for (double delay : delays) {
            phiAccrualFailureDetector.addDelay(delay);
        }

        double phi = phiAccrualFailureDetector.computePhi(5.6);
        System.out.println(phi);

//        final int tasksCount = 10_000;
//
//        final Runnable ioTask =
//                () -> {
//                    try {
//                        Thread.sleep(250L);
//                    } catch (InterruptedException interEx) {
//                        Thread.currentThread().interrupt();
//                    }
//                };
//
//        measureThroughput(
//                "256 fixed thread pool", Executors.newFixedThreadPool(256), ioTask, tasksCount);
//        measureThroughput(
//                "1000 fixed thread pool", Executors.newFixedThreadPool(1000), ioTask, tasksCount);
//        measureThroughput(
//                "Virtual thread pool",
//                Executors.newVirtualThreadPerTaskExecutor(),
//                ioTask,
//                tasksCount);
    }


    /*
    256 fixed thread pool ===============> Duration: 10030 ms, Throughput: 997.0 rps
    1000 fixed thread pool ===============> Duration: 2625 ms, Throughput: 3809.5 rps
    Virtual thread pool ===============> Duration: 321 ms, Throughput: 31152.6 rps
     */
    static void measureThroughput(
            String title, ExecutorService pool, Runnable task, int numOfTasks) {

        final AtomicInteger completedTasksCount = new AtomicInteger();
        final Instant start = Instant.now();

        try (pool) {
            IntStream.range(0, numOfTasks)
                    .forEach(
                            _ -> {
                                pool.execute(
                                        () -> {
                                            task.run();
                                            completedTasksCount.incrementAndGet();
                                        });
                            });
        }

        long durationInMs = Duration.between(start, Instant.now()).toMillis();

        double throughput = (((double) completedTasksCount.get()) / durationInMs) * 1000.0;

        System.out.printf(
                "%30s: ===============> Duration: %d ms, Throughput: %.1f rps %n",
                title, durationInMs, throughput);
    }
}
