package com.pyrosense.identity.application.port.out;

import com.pyrosense.shared.id.TenantId;
import com.pyrosense.shared.security.AuditEntry;

import java.time.Instant;
import java.util.List;

public interface AuditLogRepository {

    void save(AuditEntry entry);

    List<AuditEntry> findByTenant(TenantId tenantId, Instant from, Instant to);
}
