package com.pyrosense.notification.application.port.out;

import com.pyrosense.notification.domain.model.TenantNotificationPolicy;
import com.pyrosense.shared.id.TenantId;

import java.util.Optional;

public interface TenantNotificationPolicyRepository {
    Optional<TenantNotificationPolicy> findByTenantId(TenantId tenantId);
    TenantNotificationPolicy save(TenantNotificationPolicy policy);
}
