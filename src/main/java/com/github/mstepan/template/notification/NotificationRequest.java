package com.github.mstepan.template.notification;

import org.jspecify.annotations.Nullable;

public record NotificationRequest(
        @Nullable String requestId,
        @Nullable NotificationType type,
        @Nullable String recipient,
        @Nullable String message) {

    public boolean isValid() {
        if (requestId == null || recipient == null || message == null || type == null) {
            return false;
        }

        final String recipientNormalized = recipient.trim();

        return switch (type) {
            case PUSH -> !recipientNormalized.isEmpty();
            case SMS -> recipientNormalized.startsWith("+") && recipientNormalized.length() == 12;
            case EMAIL -> recipientNormalized.contains("@");
        };
    }
}
