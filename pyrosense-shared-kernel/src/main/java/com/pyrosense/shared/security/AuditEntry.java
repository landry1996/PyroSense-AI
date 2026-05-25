package com.pyrosense.shared.security;

import com.pyrosense.shared.id.TenantId;
import com.pyrosense.shared.id.UserId;
import com.pyrosense.shared.util.ClockProvider;

import java.time.Instant;
import java.util.UUID;

public record AuditEntry(
        UUID id,
        String action,
        String resourceType,
        String resourceId,
        UserId userId,
        TenantId tenantId,
        String ipAddress,
        String userAgent,
        String details,
        Instant timestamp
) {
    public static AuditEntry create(String action, String resourceType, String resourceId,
                                     UserId userId, TenantId tenantId, String ipAddress,
                                     String userAgent, String details) {
        return new AuditEntry(UUID.randomUUID(), action, resourceType, resourceId,
                userId, tenantId, ipAddress, userAgent, details, ClockProvider.now());
    }
}
