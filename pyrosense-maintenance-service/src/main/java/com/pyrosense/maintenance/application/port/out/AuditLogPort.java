package com.pyrosense.maintenance.application.port.out;

import com.pyrosense.shared.id.TenantId;
import com.pyrosense.shared.id.UserId;

import java.util.UUID;

public interface AuditLogPort {

    void log(TenantId tenantId, UserId actor, UUID interventionId, String action, String details);

    default void log(String action, TenantId tenantId, String details) {
        log(tenantId, new UserId(new UUID(0, 0)), null, action, details);
    }
}
