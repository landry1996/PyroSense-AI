package com.pyrosense.notification.application.port.in;

import com.pyrosense.notification.domain.model.TenantNotificationPolicy;
import com.pyrosense.shared.id.TenantId;

import java.time.LocalTime;

public interface ManageTenantNotificationPolicyUseCase {

    TenantNotificationPolicy getPolicy(TenantId tenantId);

    TenantNotificationPolicy updatePolicy(TenantId tenantId, UpdatePolicyCommand command);

    record UpdatePolicyCommand(
            boolean emailEnabledByDefault,
            boolean smsEnabledByDefault,
            boolean pushEnabledByDefault,
            boolean webhookEnabledByDefault,
            boolean criticalOverrideMandatory,
            boolean requirePhoneVerificationForSms,
            boolean requirePushTokenForPush,
            LocalTime defaultQuietHoursStart,
            LocalTime defaultQuietHoursEnd,
            String defaultLanguage
    ) {}
}
