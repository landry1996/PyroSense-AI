package com.pyrosense.notification.domain.event;

import com.pyrosense.notification.domain.model.NotificationChannel;
import com.pyrosense.shared.domain.DomainEvent;
import com.pyrosense.shared.id.TenantId;
import com.pyrosense.shared.id.UserId;

import java.time.Instant;
import java.util.UUID;

public record NotificationSentEvent(
        UUID eventId,
        Instant occurredAt,
        UUID notificationId,
        TenantId tenantId,
        UserId recipientId,
        NotificationChannel channel,
        String subject
) implements DomainEvent {
    @Override
    public String eventType() {
        return "notification.notification.sent";
    }
}
