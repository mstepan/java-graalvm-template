package com.github.mstepan.template.notification;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class NotificationRequestTest {

    @Test
    void correctEmail() {
        assertTrue(
                new NotificationRequest(
                                "id-123", NotificationType.EMAIL, "maksym@oracle.com", "Hello!!!")
                        .isValid());

        assertTrue(
                new NotificationRequest("id-123", NotificationType.EMAIL, "@oracle.com", "Hello!!!")
                        .isValid());

        assertTrue(
                new NotificationRequest("id-123", NotificationType.EMAIL, "@", "Hello!!!")
                        .isValid());
    }

    @Test
    void incorrectEmail() {
        assertFalse(
                new NotificationRequest(
                                null, NotificationType.EMAIL, "maksym@oracle.com", "Hello!!!")
                        .isValid());

        assertFalse(
                new NotificationRequest("id-123", null, "maksym@oracle.com", "Hello!!!").isValid());

        assertFalse(
                new NotificationRequest("id-123", NotificationType.EMAIL, null, "Hello!!!")
                        .isValid());

        assertFalse(
                new NotificationRequest(
                                "id-123", NotificationType.EMAIL, "maksymoracle.com", "Hello!!!")
                        .isValid());
    }

    @Test
    void notificationRequestWithNullTypeShouldFail() {
        assertFalse(
                new NotificationRequest("id-123", null, "maksym@oracle.com", "Hello!!!").isValid());
    }
}
