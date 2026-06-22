package com.github.mstepan.template.notification;

import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

/**
 * Thread safe.
 *
 * <p>Solved task from https://habr.com/ru/articles/1047160/.
 */
public final class NotificationService {

    private final ConcurrentMap<String, String> processedRequests = new ConcurrentHashMap<>();
    private final Metrics metrics;

    private final Consumer<NotificationRequest> notificationSender;

    public NotificationService(Consumer<NotificationRequest> notificationSender, Metrics metrics) {
        this.notificationSender = Objects.requireNonNull(notificationSender);
        this.metrics = Objects.requireNonNull(metrics);
    }

    public NotificationService() {
        this(
                NotificationService::emulateSendingNotification,
                new Metrics(new AtomicLong(0L), new AtomicLong(0L), new AtomicLong(0L)));
    }

    public void send(NotificationRequest singleRequest) {
        Objects.requireNonNull(singleRequest);

        if (!singleRequest.isValid()) {
            metrics.failedCnt.incrementAndGet();
            return;
        }
        boolean wasAdded =
                processedRequests.putIfAbsent(singleRequest.requestId(), singleRequest.requestId())
                        == null;

        if (wasAdded) {
            notificationSender.accept(singleRequest);
            metrics.processedCnt.incrementAndGet();
        } else {
            metrics.duplicatesSkippedCnt.incrementAndGet();
        }
    }

    /*
    После обработки вывести:

    Processed: 3
    Duplicates skipped: 2
    Failed: 0
     */
    public void printMetrics() {
        metrics.printToConsole();
    }

    /*
    Sending EMAIL to user1@mail.com
    Sending SMS to +79990001122
    Sending PUSH to device-123
     */
    private static void emulateSendingNotification(NotificationRequest singleRequest) {
        System.out.printf("Sending %s to %s%n", singleRequest.type(), singleRequest.recipient());
    }

    public static final class Metrics {

        private Metrics(
                AtomicLong processedCnt, AtomicLong duplicatesSkippedCnt, AtomicLong failedCnt) {
            this.processedCnt = processedCnt;
            this.duplicatesSkippedCnt = duplicatesSkippedCnt;
            this.failedCnt = failedCnt;
        }

        private final AtomicLong processedCnt;
        private final AtomicLong duplicatesSkippedCnt;
        private final AtomicLong failedCnt;

        public Metrics() {
            this(new AtomicLong(0L), new AtomicLong(0L), new AtomicLong(0L));
        }

        public long processedCnt() {
            return processedCnt.get();
        }

        public long duplicatesSkippedCnt() {
            return duplicatesSkippedCnt.get();
        }

        public long failedCnt() {
            return failedCnt.get();
        }

        public void printToConsole() {
            System.out.printf("Processed: %d%n", processedCnt.get());
            System.out.printf("Duplicates skipped: %d%n", duplicatesSkippedCnt.get());
            System.out.printf("Failed: %d%n", failedCnt.get());
        }
    }
}
