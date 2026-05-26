package com.pyrosense.notification.domain.event;

import com.pyrosense.notification.domain.model.NotificationChannel;
import com.pyrosense.shared.domain.DomainEvent;
import com.pyrosense.shared.id.TenantId;
import com.pyrosense.shared.id.UserId;

import java.time.Instant;
import java.util.UUID;

public record NotificationFailedEvent(
        UUID eventId,
        Instant occurredAt,
        UUID notificationId,
        TenantId tenantId,
        UserId recipientId,
        NotificationChannel channel,
        String failureReason,
        int retryCount
) implements DomainEvent {
    @Override
    public String eventType() {
        return "notification.notification.failed";
    }
}
