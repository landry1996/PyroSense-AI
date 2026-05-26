package com.pyrosense.notification.domain.model;

import com.pyrosense.shared.id.TenantId;

import java.time.Instant;
import java.time.LocalTime;

public class TenantNotificationPolicy {
    private final TenantId tenantId;
    private boolean emailEnabledByDefault;
    private boolean smsEnabledByDefault;
    private boolean pushEnabledByDefault;
    private boolean webhookEnabledByDefault;
    private boolean criticalOverrideMandatory;
    private boolean requirePhoneVerificationForSms;
    private boolean requirePushTokenForPush;
    private LocalTime defaultQuietHoursStart;
    private LocalTime defaultQuietHoursEnd;
    private String defaultLanguage;
    private final Instant createdAt;
    private Instant updatedAt;

    public TenantNotificationPolicy(TenantId tenantId) {
        this.tenantId = tenantId;
        this.emailEnabledByDefault = true;
        this.smsEnabledByDefault = true;
        this.pushEnabledByDefault = true;
        this.webhookEnabledByDefault = false;
        this.criticalOverrideMandatory = true;
        this.requirePhoneVerificationForSms = true;
        this.requirePushTokenForPush = true;
        this.defaultLanguage = "fr";
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
    }

    private TenantNotificationPolicy(TenantId tenantId, boolean emailEnabledByDefault,
                                      boolean smsEnabledByDefault, boolean pushEnabledByDefault,
                                      boolean webhookEnabledByDefault, boolean criticalOverrideMandatory,
                                      boolean requirePhoneVerificationForSms, boolean requirePushTokenForPush,
                                      LocalTime defaultQuietHoursStart, LocalTime defaultQuietHoursEnd,
                                      String defaultLanguage, Instant createdAt, Instant updatedAt) {
        this.tenantId = tenantId;
        this.emailEnabledByDefault = emailEnabledByDefault;
        this.smsEnabledByDefault = smsEnabledByDefault;
        this.pushEnabledByDefault = pushEnabledByDefault;
        this.webhookEnabledByDefault = webhookEnabledByDefault;
        this.criticalOverrideMandatory = criticalOverrideMandatory;
        this.requirePhoneVerificationForSms = requirePhoneVerificationForSms;
        this.requirePushTokenForPush = requirePushTokenForPush;
        this.defaultQuietHoursStart = defaultQuietHoursStart;
        this.defaultQuietHoursEnd = defaultQuietHoursEnd;
        this.defaultLanguage = defaultLanguage;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static TenantNotificationPolicy reconstitute(TenantId tenantId,
                                                         boolean emailEnabledByDefault, boolean smsEnabledByDefault,
                                                         boolean pushEnabledByDefault, boolean webhookEnabledByDefault,
                                                         boolean criticalOverrideMandatory,
                                                         boolean requirePhoneVerificationForSms,
                                                         boolean requirePushTokenForPush,
                                                         LocalTime defaultQuietHoursStart, LocalTime defaultQuietHoursEnd,
                                                         String defaultLanguage,
                                                         Instant createdAt, Instant updatedAt) {
        return new TenantNotificationPolicy(tenantId, emailEnabledByDefault, smsEnabledByDefault,
                pushEnabledByDefault, webhookEnabledByDefault, criticalOverrideMandatory,
                requirePhoneVerificationForSms, requirePushTokenForPush,
                defaultQuietHoursStart, defaultQuietHoursEnd, defaultLanguage, createdAt, updatedAt);
    }

    public void update(boolean emailEnabledByDefault, boolean smsEnabledByDefault,
                       boolean pushEnabledByDefault, boolean webhookEnabledByDefault,
                       boolean criticalOverrideMandatory, boolean requirePhoneVerificationForSms,
                       boolean requirePushTokenForPush,
                       LocalTime defaultQuietHoursStart, LocalTime defaultQuietHoursEnd,
                       String defaultLanguage) {
        this.emailEnabledByDefault = emailEnabledByDefault;
        this.smsEnabledByDefault = smsEnabledByDefault;
        this.pushEnabledByDefault = pushEnabledByDefault;
        this.webhookEnabledByDefault = webhookEnabledByDefault;
        this.criticalOverrideMandatory = criticalOverrideMandatory;
        this.requirePhoneVerificationForSms = requirePhoneVerificationForSms;
        this.requirePushTokenForPush = requirePushTokenForPush;
        this.defaultQuietHoursStart = defaultQuietHoursStart;
        this.defaultQuietHoursEnd = defaultQuietHoursEnd;
        this.defaultLanguage = defaultLanguage;
        this.updatedAt = Instant.now();
    }

    public TenantId getTenantId() { return tenantId; }
    public boolean isEmailEnabledByDefault() { return emailEnabledByDefault; }
    public boolean isSmsEnabledByDefault() { return smsEnabledByDefault; }
    public boolean isPushEnabledByDefault() { return pushEnabledByDefault; }
    public boolean isWebhookEnabledByDefault() { return webhookEnabledByDefault; }
    public boolean isCriticalOverrideMandatory() { return criticalOverrideMandatory; }
    public boolean isRequirePhoneVerificationForSms() { return requirePhoneVerificationForSms; }
    public boolean isRequirePushTokenForPush() { return requirePushTokenForPush; }
    public LocalTime getDefaultQuietHoursStart() { return defaultQuietHoursStart; }
    public LocalTime getDefaultQuietHoursEnd() { return defaultQuietHoursEnd; }
    public String getDefaultLanguage() { return defaultLanguage; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
