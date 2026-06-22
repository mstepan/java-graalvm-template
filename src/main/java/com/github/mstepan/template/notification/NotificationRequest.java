package com.github.mstepan.template.notification;

public record NotificationRequest(
        String requestId, NotificationType type, String recipient, String message) {

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
