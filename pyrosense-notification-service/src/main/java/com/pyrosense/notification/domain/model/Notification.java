package com.pyrosense.notification.domain.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public class Notification {

    private final UUID id;
    private final UUID recipientId;
    private final NotificationChannel channel;
    private final String subject;
    private final String body;
    private NotificationStatus status;
    private final Instant createdAt;
    private Instant sentAt;

    public Notification(UUID id, UUID recipientId, NotificationChannel channel,
                        String subject, String body) {
        this.id = Objects.requireNonNull(id);
        this.recipientId = Objects.requireNonNull(recipientId);
        this.channel = Objects.requireNonNull(channel);
        this.subject = Objects.requireNonNull(subject);
        this.body = Objects.requireNonNull(body);
        this.status = NotificationStatus.PENDING;
        this.createdAt = Instant.now();
    }

    public void markSent() {
        this.status = NotificationStatus.SENT;
        this.sentAt = Instant.now();
    }

    public void markFailed() {
        this.status = NotificationStatus.FAILED;
    }

    public UUID getId() { return id; }
    public UUID getRecipientId() { return recipientId; }
    public NotificationChannel getChannel() { return channel; }
    public String getSubject() { return subject; }
    public String getBody() { return body; }
    public NotificationStatus getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getSentAt() { return sentAt; }
}
