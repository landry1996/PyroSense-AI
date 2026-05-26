package com.pyrosense.shared.security;

import com.pyrosense.shared.id.TenantId;
import com.pyrosense.shared.id.UserId;
import com.pyrosense.shared.util.ClockProvider;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record AuditEntry(
        UUID id,
        String action,
        String resourceType,
        String resourceId,
        UserId userId,
        String actorRole,
        TenantId tenantId,
        String ipAddress,
        String userAgent,
        String correlationId,
        String details,
        Map<String, String> metadata,
        Instant timestamp
) {
    public AuditEntry(UUID id, String action, String resourceType, String resourceId,
                      UserId userId, TenantId tenantId, String ipAddress, String userAgent,
                      String details, Instant timestamp) {
        this(id, action, resourceType, resourceId, userId, null, tenantId,
                ipAddress, userAgent, null, details, Map.of(), timestamp);
    }

    public static AuditEntry create(String action, String resourceType, String resourceId,
                                     UserId userId, TenantId tenantId, String ipAddress,
                                     String userAgent, String details) {
        return new AuditEntry(UUID.randomUUID(), action, resourceType, resourceId,
                userId, null, tenantId, ipAddress, userAgent, null, details, Map.of(), ClockProvider.now());
    }

    public static AuditEntry createFull(String action, String resourceType, String resourceId,
                                         UserId userId, String actorRole, TenantId tenantId,
                                         String ipAddress, String userAgent, String correlationId,
                                         String details, Map<String, String> metadata) {
        return new AuditEntry(UUID.randomUUID(), action, resourceType, resourceId,
                userId, actorRole, tenantId, ipAddress, userAgent, correlationId,
                details, metadata != null ? metadata : Map.of(), ClockProvider.now());
    }
}
