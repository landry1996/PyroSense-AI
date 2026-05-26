package com.pyrosense.notification.domain.model;

import com.pyrosense.shared.id.TenantId;
import com.pyrosense.shared.id.UserId;

import java.time.Instant;
import java.time.LocalTime;

public class NotificationPreferences {
    private final UserId userId;
    private final TenantId tenantId;
    private boolean emailEnabled;
    private boolean smsEnabled;
    private boolean pushEnabled;
    private boolean webhookEnabled;
    private LocalTime quietHoursStart;
    private LocalTime quietHoursEnd;
    private final Instant createdAt;
    private Instant updatedAt;

    public NotificationPreferences(UserId userId, TenantId tenantId) {
        this.userId = userId;
        this.tenantId = tenantId;
        this.emailEnabled = true;
        this.smsEnabled = true;
        this.pushEnabled = true;
        this.webhookEnabled = true;
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
    }

    private NotificationPreferences(UserId userId, TenantId tenantId, boolean emailEnabled,
                                     boolean smsEnabled, boolean pushEnabled, boolean webhookEnabled,
                                     LocalTime quietHoursStart, LocalTime quietHoursEnd,
                                     Instant createdAt, Instant updatedAt) {
        this.userId = userId;
        this.tenantId = tenantId;
        this.emailEnabled = emailEnabled;
        this.smsEnabled = smsEnabled;
        this.pushEnabled = pushEnabled;
        this.webhookEnabled = webhookEnabled;
        this.quietHoursStart = quietHoursStart;
        this.quietHoursEnd = quietHoursEnd;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static NotificationPreferences reconstitute(UserId userId, TenantId tenantId,
                                                        boolean emailEnabled, boolean smsEnabled,
                                                        boolean pushEnabled, boolean webhookEnabled,
                                                        LocalTime quietHoursStart, LocalTime quietHoursEnd,
                                                        Instant createdAt, Instant updatedAt) {
        return new NotificationPreferences(userId, tenantId, emailEnabled, smsEnabled, pushEnabled,
                webhookEnabled, quietHoursStart, quietHoursEnd, createdAt, updatedAt);
    }

    public void update(boolean emailEnabled, boolean smsEnabled, boolean pushEnabled,
                       boolean webhookEnabled, LocalTime quietHoursStart, LocalTime quietHoursEnd) {
        this.emailEnabled = emailEnabled;
        this.smsEnabled = smsEnabled;
        this.pushEnabled = pushEnabled;
        this.webhookEnabled = webhookEnabled;
        this.quietHoursStart = quietHoursStart;
        this.quietHoursEnd = quietHoursEnd;
        this.updatedAt = Instant.now();
    }

    public UserId getUserId() { return userId; }
    public TenantId getTenantId() { return tenantId; }
    public boolean isEmailEnabled() { return emailEnabled; }
    public boolean isSmsEnabled() { return smsEnabled; }
    public boolean isPushEnabled() { return pushEnabled; }
    public boolean isWebhookEnabled() { return webhookEnabled; }
    public LocalTime getQuietHoursStart() { return quietHoursStart; }
    public LocalTime getQuietHoursEnd() { return quietHoursEnd; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
