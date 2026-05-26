package com.pyrosense.notification.application.port.out;

import com.pyrosense.shared.id.TenantId;

public interface AuditLogPort {

    void log(String action, TenantId tenantId, String details);

    default void log(String action, String details) {
        log(action, null, details);
    }
}
