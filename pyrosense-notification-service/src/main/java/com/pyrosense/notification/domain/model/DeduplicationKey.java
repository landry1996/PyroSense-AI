package com.pyrosense.notification.domain.model;

import com.pyrosense.shared.id.UserId;

import java.util.Objects;

public record DeduplicationKey(
        UserId recipientId,
        NotificationChannel channel,
        String alertFingerprint
) {
    public DeduplicationKey {
        Objects.requireNonNull(recipientId);
        Objects.requireNonNull(channel);
        Objects.requireNonNull(alertFingerprint);
    }

    public String toKeyString() {
        return "%s:%s:%s".formatted(recipientId.value(), channel.name(), alertFingerprint);
    }
}
