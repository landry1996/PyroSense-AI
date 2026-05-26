package com.pyrosense.notification.domain.model;

import java.time.Instant;
import java.util.UUID;

public record NotificationDeliveryAttempt(
        UUID id,
        UUID notificationId,
        NotificationChannel channel,
        int attemptNumber,
        boolean success,
        String errorMessage,
        Instant attemptedAt
) {
    public static NotificationDeliveryAttempt success(UUID notificationId, NotificationChannel channel, int attemptNumber) {
        return new NotificationDeliveryAttempt(UUID.randomUUID(), notificationId, channel, attemptNumber, true, null, Instant.now());
    }

    public static NotificationDeliveryAttempt failure(UUID notificationId, NotificationChannel channel, int attemptNumber, String error) {
        return new NotificationDeliveryAttempt(UUID.randomUUID(), notificationId, channel, attemptNumber, false, error, Instant.now());
    }
}
