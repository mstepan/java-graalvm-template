package com.github.mstepan.template.notification;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.junit.jupiter.api.parallel.Resources;

public class NotificationServiceTest {

    @Test
    @ResourceLock(Resources.SYSTEM_OUT)
    void defaultServiceSendsOnceAndPrintsMetrics() {
        var output = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        try (var capturedOut = new PrintStream(output, true, StandardCharsets.UTF_8)) {
            System.setOut(capturedOut);
            var service = new NotificationService();
            var request = new NotificationRequest("id", NotificationType.PUSH, "device", "hello");
            service.send(request);
            service.send(request);
            service.send(new NotificationRequest("invalid", NotificationType.PUSH, " ", "hello"));
            service.printMetrics();
        } finally {
            System.setOut(originalOut);
        }
        assertEquals(
                String.join(
                        System.lineSeparator(),
                        "Sending PUSH to device",
                        "Processed: 1",
                        "Duplicates skipped: 1",
                        "Failed: 1",
                        ""),
                output.toString(StandardCharsets.UTF_8));
    }

    @Test
    void sendNormalEmail() {
        AtomicLong sendCount = new AtomicLong(0L);

        Consumer<NotificationRequest> consumer = _ -> sendCount.incrementAndGet();

        var metrics = new NotificationService.Metrics();

        var service = new NotificationService(consumer, metrics);

        service.send(
                new NotificationRequest(
                        "id-123", NotificationType.EMAIL, "maksym@oracle.com", "Hello!!!"));

        service.send(
                new NotificationRequest(
                        "id-123", NotificationType.EMAIL, "maksym@oracle.com", "Good day!!!"));

        service.send(
                new NotificationRequest(
                        "id-123", NotificationType.EMAIL, "maksymoracle.com", "Good day!!!"));

        assertEquals(1L, sendCount.get());
        assertEquals(1L, metrics.processedCnt());
        assertEquals(1L, metrics.failedCnt());
        assertEquals(1L, metrics.duplicatesSkippedCnt());
    }
}
