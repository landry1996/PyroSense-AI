package com.pyrosense.notification.domain.model;

import com.pyrosense.shared.id.TenantId;
import com.pyrosense.shared.id.UserId;
import com.pyrosense.shared.valueobject.AlertSeverity;

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
    private String language;
    private boolean criticalOverrideEnabled;
    private boolean phoneVerified;
    private boolean emailVerified;
    private boolean pushTokenRegistered;
    private final Instant createdAt;
    private Instant updatedAt;

    public NotificationPreferences(UserId userId, TenantId tenantId) {
        this.userId = userId;
        this.tenantId = tenantId;
        this.emailEnabled = true;
        this.smsEnabled = true;
        this.pushEnabled = true;
        this.webhookEnabled = true;
        this.language = "fr";
        this.criticalOverrideEnabled = true;
        this.phoneVerified = false;
        this.emailVerified = false;
        this.pushTokenRegistered = false;
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
    }

    private NotificationPreferences(UserId userId, TenantId tenantId, boolean emailEnabled,
                                     boolean smsEnabled, boolean pushEnabled, boolean webhookEnabled,
                                     LocalTime quietHoursStart, LocalTime quietHoursEnd,
                                     String language, boolean criticalOverrideEnabled,
                                     boolean phoneVerified, boolean emailVerified,
                                     boolean pushTokenRegistered,
                                     Instant createdAt, Instant updatedAt) {
        this.userId = userId;
        this.tenantId = tenantId;
        this.emailEnabled = emailEnabled;
        this.smsEnabled = smsEnabled;
        this.pushEnabled = pushEnabled;
        this.webhookEnabled = webhookEnabled;
        this.quietHoursStart = quietHoursStart;
        this.quietHoursEnd = quietHoursEnd;
        this.language = language;
        this.criticalOverrideEnabled = criticalOverrideEnabled;
        this.phoneVerified = phoneVerified;
        this.emailVerified = emailVerified;
        this.pushTokenRegistered = pushTokenRegistered;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static NotificationPreferences reconstitute(UserId userId, TenantId tenantId,
                                                        boolean emailEnabled, boolean smsEnabled,
                                                        boolean pushEnabled, boolean webhookEnabled,
                                                        LocalTime quietHoursStart, LocalTime quietHoursEnd,
                                                        Instant createdAt, Instant updatedAt) {
        return new NotificationPreferences(userId, tenantId, emailEnabled, smsEnabled, pushEnabled,
                webhookEnabled, quietHoursStart, quietHoursEnd, "fr", true,
                false, false, false, createdAt, updatedAt);
    }

    public static NotificationPreferences reconstituteFull(UserId userId, TenantId tenantId,
                                                            boolean emailEnabled, boolean smsEnabled,
                                                            boolean pushEnabled, boolean webhookEnabled,
                                                            LocalTime quietHoursStart, LocalTime quietHoursEnd,
                                                            String language, boolean criticalOverrideEnabled,
                                                            boolean phoneVerified, boolean emailVerified,
                                                            boolean pushTokenRegistered,
                                                            Instant createdAt, Instant updatedAt) {
        return new NotificationPreferences(userId, tenantId, emailEnabled, smsEnabled, pushEnabled,
                webhookEnabled, quietHoursStart, quietHoursEnd, language, criticalOverrideEnabled,
                phoneVerified, emailVerified, pushTokenRegistered, createdAt, updatedAt);
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

    public void updateFull(boolean emailEnabled, boolean smsEnabled, boolean pushEnabled,
                           boolean webhookEnabled, LocalTime quietHoursStart, LocalTime quietHoursEnd,
                           String language, boolean criticalOverrideEnabled,
                           boolean phoneVerified, boolean emailVerified, boolean pushTokenRegistered) {
        this.emailEnabled = emailEnabled;
        this.smsEnabled = smsEnabled;
        this.pushEnabled = pushEnabled;
        this.webhookEnabled = webhookEnabled;
        this.quietHoursStart = quietHoursStart;
        this.quietHoursEnd = quietHoursEnd;
        this.language = language;
        this.criticalOverrideEnabled = criticalOverrideEnabled;
        this.phoneVerified = phoneVerified;
        this.emailVerified = emailVerified;
        this.pushTokenRegistered = pushTokenRegistered;
        this.updatedAt = Instant.now();
    }

    public boolean isChannelAllowed(NotificationChannel channel, AlertSeverity severity) {
        if (severity == AlertSeverity.CRITICAL && criticalOverrideEnabled) {
            return isChannelTechnicallyReachable(channel);
        }

        if (isInQuietHours() && severity != AlertSeverity.CRITICAL) {
            return channel == NotificationChannel.DASHBOARD;
        }

        if (!isChannelEnabledByUser(channel)) return false;
        return isChannelTechnicallyReachable(channel);
    }

    public boolean isChannelEnabledByUser(NotificationChannel channel) {
        return switch (channel) {
            case EMAIL -> emailEnabled;
            case SMS -> smsEnabled;
            case PUSH -> pushEnabled;
            case WEBHOOK -> webhookEnabled;
            case DASHBOARD -> true;
        };
    }

    public boolean isChannelTechnicallyReachable(NotificationChannel channel) {
        return switch (channel) {
            case SMS -> phoneVerified;
            case PUSH -> pushTokenRegistered;
            case EMAIL -> emailVerified;
            case DASHBOARD, WEBHOOK -> true;
        };
    }

    public boolean isInQuietHours() {
        if (quietHoursStart == null || quietHoursEnd == null) return false;
        LocalTime now = LocalTime.now();
        if (quietHoursStart.isBefore(quietHoursEnd)) {
            return !now.isBefore(quietHoursStart) && now.isBefore(quietHoursEnd);
        }
        return !now.isBefore(quietHoursStart) || now.isBefore(quietHoursEnd);
    }

    public boolean isInQuietHours(LocalTime time) {
        if (quietHoursStart == null || quietHoursEnd == null) return false;
        if (quietHoursStart.isBefore(quietHoursEnd)) {
            return !time.isBefore(quietHoursStart) && time.isBefore(quietHoursEnd);
        }
        return !time.isBefore(quietHoursStart) || time.isBefore(quietHoursEnd);
    }

    public UserId getUserId() { return userId; }
    public TenantId getTenantId() { return tenantId; }
    public boolean isEmailEnabled() { return emailEnabled; }
    public boolean isSmsEnabled() { return smsEnabled; }
    public boolean isPushEnabled() { return pushEnabled; }
    public boolean isWebhookEnabled() { return webhookEnabled; }
    public LocalTime getQuietHoursStart() { return quietHoursStart; }
    public LocalTime getQuietHoursEnd() { return quietHoursEnd; }
    public String getLanguage() { return language; }
    public boolean isCriticalOverrideEnabled() { return criticalOverrideEnabled; }
    public boolean isPhoneVerified() { return phoneVerified; }
    public boolean isEmailVerified() { return emailVerified; }
    public boolean isPushTokenRegistered() { return pushTokenRegistered; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
