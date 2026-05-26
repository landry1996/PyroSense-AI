package com.pyrosense.notification.domain.model;

import com.pyrosense.shared.id.TenantId;
import com.pyrosense.shared.id.UserId;
import com.pyrosense.shared.util.ClockProvider;
import com.pyrosense.shared.valueobject.AlertSeverity;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class Notification {

    private static final int MAX_RETRIES = 3;
    private static final Duration[] BACKOFF_DELAYS = {
            Duration.ofSeconds(30),
            Duration.ofMinutes(2),
            Duration.ofMinutes(10)
    };

    private final UUID id;
    private final TenantId tenantId;
    private final UserId recipientId;
    private final NotificationChannel channel;
    private final AlertSeverity severity;
    private final String subject;
    private final String body;
    private final String alertFingerprint;
    private NotificationStatus status;
    private int retryCount;
    private Instant nextRetryAt;
    private final Instant createdAt;
    private Instant sentAt;
    private String failureReason;
    private final List<NotificationDeliveryAttempt> deliveryAttempts = new ArrayList<>();

    public Notification(UUID id, TenantId tenantId, UserId recipientId, NotificationChannel channel,
                        AlertSeverity severity, String subject, String body, String alertFingerprint) {
        this.id = Objects.requireNonNull(id);
        this.tenantId = Objects.requireNonNull(tenantId);
        this.recipientId = Objects.requireNonNull(recipientId);
        this.channel = Objects.requireNonNull(channel);
        this.severity = Objects.requireNonNull(severity);
        this.subject = Objects.requireNonNull(subject);
        this.body = Objects.requireNonNull(body);
        this.alertFingerprint = Objects.requireNonNull(alertFingerprint);
        this.status = NotificationStatus.PENDING;
        this.retryCount = 0;
        this.createdAt = ClockProvider.now();
    }

    public void markSent() {
        this.status = NotificationStatus.SENT;
        this.sentAt = ClockProvider.now();
        this.failureReason = null;
        this.deliveryAttempts.add(NotificationDeliveryAttempt.success(id, channel, retryCount + 1));
    }

    public void markFailed(String reason) {
        this.failureReason = reason;
        this.retryCount++;
        this.deliveryAttempts.add(NotificationDeliveryAttempt.failure(id, channel, retryCount, reason));
        if (retryCount < MAX_RETRIES) {
            this.status = NotificationStatus.RETRYING;
            this.nextRetryAt = ClockProvider.now().plus(BACKOFF_DELAYS[retryCount - 1]);
        } else {
            this.status = NotificationStatus.FAILED;
        }
    }

    public void markSuppressed() {
        this.status = NotificationStatus.SUPPRESSED;
    }

    public void markCancelled() {
        this.status = NotificationStatus.CANCELLED;
    }

    public boolean shouldRetryNow() {
        return status == NotificationStatus.RETRYING
                && nextRetryAt != null
                && !ClockProvider.now().isBefore(nextRetryAt);
    }

    public boolean isExhausted() {
        return status == NotificationStatus.FAILED && retryCount >= MAX_RETRIES;
    }

    public DeduplicationKey deduplicationKey() {
        return new DeduplicationKey(recipientId, channel, alertFingerprint);
    }

    // Getters
    public UUID getId() { return id; }
    public TenantId getTenantId() { return tenantId; }
    public UserId getRecipientId() { return recipientId; }
    public NotificationChannel getChannel() { return channel; }
    public AlertSeverity getSeverity() { return severity; }
    public String getSubject() { return subject; }
    public String getBody() { return body; }
    public String getAlertFingerprint() { return alertFingerprint; }
    public NotificationStatus getStatus() { return status; }
    public int getRetryCount() { return retryCount; }
    public Instant getNextRetryAt() { return nextRetryAt; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getSentAt() { return sentAt; }
    public String getFailureReason() { return failureReason; }
    public List<NotificationDeliveryAttempt> getDeliveryAttempts() { return Collections.unmodifiableList(deliveryAttempts); }
}
