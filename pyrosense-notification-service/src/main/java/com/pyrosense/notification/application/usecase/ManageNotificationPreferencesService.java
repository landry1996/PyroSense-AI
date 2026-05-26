package com.pyrosense.notification.application.usecase;

import com.pyrosense.notification.application.port.in.ManageNotificationPreferencesUseCase;
import com.pyrosense.notification.application.port.out.AuditLogPort;
import com.pyrosense.notification.application.port.out.NotificationPreferencesRepository;
import com.pyrosense.notification.application.port.out.TenantNotificationPolicyRepository;
import com.pyrosense.notification.domain.model.NotificationPreferences;
import com.pyrosense.notification.domain.model.TenantNotificationPolicy;
import com.pyrosense.shared.id.TenantId;
import com.pyrosense.shared.id.UserId;

public class ManageNotificationPreferencesService implements ManageNotificationPreferencesUseCase {

    private final NotificationPreferencesRepository repository;
    private final TenantNotificationPolicyRepository policyRepository;
    private final AuditLogPort auditLog;

    public ManageNotificationPreferencesService(NotificationPreferencesRepository repository,
                                                 TenantNotificationPolicyRepository policyRepository,
                                                 AuditLogPort auditLog) {
        this.repository = repository;
        this.policyRepository = policyRepository;
        this.auditLog = auditLog;
    }

    @Override
    public NotificationPreferences getPreferences(UserId userId, TenantId tenantId) {
        return repository.findByUserId(userId)
                .orElse(new NotificationPreferences(userId, tenantId));
    }

    @Override
    public NotificationPreferences updatePreferences(UserId userId, TenantId tenantId, UpdatePreferencesCommand command) {
        NotificationPreferences prefs = repository.findByUserId(userId)
                .orElse(new NotificationPreferences(userId, tenantId));

        TenantNotificationPolicy policy = policyRepository != null
                ? policyRepository.findByTenantId(tenantId).orElse(new TenantNotificationPolicy(tenantId))
                : new TenantNotificationPolicy(tenantId);

        boolean effectiveCriticalOverride = true;

        boolean effectiveSms = command.smsEnabled()
                && (!policy.isRequirePhoneVerificationForSms() || command.phoneVerified());

        boolean effectivePush = command.pushEnabled()
                && (!policy.isRequirePushTokenForPush() || command.pushTokenRegistered());

        prefs.updateFull(
                command.emailEnabled(), effectiveSms, effectivePush,
                command.webhookEnabled(), command.quietHoursStart(), command.quietHoursEnd(),
                command.language(), effectiveCriticalOverride,
                command.phoneVerified(), command.emailVerified(), command.pushTokenRegistered()
        );

        NotificationPreferences saved = repository.save(prefs);

        auditLog.log("PREFERENCES_UPDATED", tenantId,
                "userId=%s email=%s sms=%s push=%s criticalOverride=%s".formatted(
                        userId.value(), saved.isEmailEnabled(), saved.isSmsEnabled(),
                        saved.isPushEnabled(), saved.isCriticalOverrideEnabled()));

        return saved;
    }
}
