package com.pyrosense.notification.application.usecase;

import com.pyrosense.notification.application.port.in.ManageTenantNotificationPolicyUseCase;
import com.pyrosense.notification.application.port.out.AuditLogPort;
import com.pyrosense.notification.application.port.out.TenantNotificationPolicyRepository;
import com.pyrosense.notification.domain.model.TenantNotificationPolicy;
import com.pyrosense.shared.id.TenantId;

public class ManageTenantNotificationPolicyService implements ManageTenantNotificationPolicyUseCase {

    private final TenantNotificationPolicyRepository repository;
    private final AuditLogPort auditLog;

    public ManageTenantNotificationPolicyService(TenantNotificationPolicyRepository repository,
                                                  AuditLogPort auditLog) {
        this.repository = repository;
        this.auditLog = auditLog;
    }

    @Override
    public TenantNotificationPolicy getPolicy(TenantId tenantId) {
        return repository.findByTenantId(tenantId)
                .orElse(new TenantNotificationPolicy(tenantId));
    }

    @Override
    public TenantNotificationPolicy updatePolicy(TenantId tenantId, UpdatePolicyCommand command) {
        TenantNotificationPolicy policy = repository.findByTenantId(tenantId)
                .orElse(new TenantNotificationPolicy(tenantId));

        policy.update(
                command.emailEnabledByDefault(), command.smsEnabledByDefault(),
                command.pushEnabledByDefault(), command.webhookEnabledByDefault(),
                command.criticalOverrideMandatory(), command.requirePhoneVerificationForSms(),
                command.requirePushTokenForPush(),
                command.defaultQuietHoursStart(), command.defaultQuietHoursEnd(),
                command.defaultLanguage()
        );

        TenantNotificationPolicy saved = repository.save(policy);

        auditLog.log("TENANT_NOTIFICATION_POLICY_UPDATED", tenantId,
                "criticalOverride=%s requirePhone=%s requirePush=%s".formatted(
                        saved.isCriticalOverrideMandatory(),
                        saved.isRequirePhoneVerificationForSms(),
                        saved.isRequirePushTokenForPush()));

        return saved;
    }
}
