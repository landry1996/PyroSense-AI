package com.pyrosense.notification.application.port.out;

import com.pyrosense.shared.id.TenantId;
import com.pyrosense.shared.security.AuditEntry;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AuditLogRepository {

    void save(AuditEntry entry);

    Optional<AuditEntry> findById(UUID id);

    List<AuditEntry> findByTenant(TenantId tenantId, Instant from, Instant to, int offset, int limit);

    List<AuditEntry> findByTenantAndAction(TenantId tenantId, String action, Instant from, Instant to, int offset, int limit);

    List<AuditEntry> findByTenantAndResource(TenantId tenantId, String resourceType, Instant from, Instant to, int offset, int limit);

    long countByTenant(TenantId tenantId, Instant from, Instant to);
}
