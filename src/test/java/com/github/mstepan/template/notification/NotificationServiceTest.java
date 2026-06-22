package com.github.mstepan.template.notification;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import org.junit.jupiter.api.Test;

public class NotificationServiceTest {

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
