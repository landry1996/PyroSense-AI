package com.pyrosense.notification.application.port.in;

import com.pyrosense.shared.id.TenantId;
import com.pyrosense.shared.security.AuditEntry;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface GetAuditLogQuery {

    AuditLogPage findByTenant(TenantId tenantId, AuditLogFilter filter);

    Optional<AuditEntry> findById(UUID id, TenantId tenantId);

    record AuditLogFilter(
            Instant from,
            Instant to,
            String action,
            String resourceType,
            int page,
            int size
    ) {
        public AuditLogFilter {
            if (size <= 0) size = 50;
            if (size > 200) size = 200;
            if (page < 0) page = 0;
        }

        public int offset() {
            return page * size;
        }
    }

    record AuditLogPage(
            List<AuditEntry> entries,
            long totalCount,
            int page,
            int size
    ) {
        public int totalPages() {
            return (int) Math.ceil((double) totalCount / size);
        }
    }
}
