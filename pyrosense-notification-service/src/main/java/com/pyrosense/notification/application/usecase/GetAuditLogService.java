package com.pyrosense.notification.application.usecase;

import com.pyrosense.notification.application.port.in.GetAuditLogQuery;
import com.pyrosense.notification.application.port.out.AuditLogRepository;
import com.pyrosense.shared.id.TenantId;
import com.pyrosense.shared.security.AuditEntry;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class GetAuditLogService implements GetAuditLogQuery {

    private final AuditLogRepository repository;

    public GetAuditLogService(AuditLogRepository repository) {
        this.repository = repository;
    }

    @Override
    public AuditLogPage findByTenant(TenantId tenantId, AuditLogFilter filter) {
        List<AuditEntry> entries;

        if (filter.action() != null && !filter.action().isBlank()) {
            entries = repository.findByTenantAndAction(
                    tenantId, filter.action(), filter.from(), filter.to(),
                    filter.offset(), filter.size());
        } else if (filter.resourceType() != null && !filter.resourceType().isBlank()) {
            entries = repository.findByTenantAndResource(
                    tenantId, filter.resourceType(), filter.from(), filter.to(),
                    filter.offset(), filter.size());
        } else {
            entries = repository.findByTenant(
                    tenantId, filter.from(), filter.to(),
                    filter.offset(), filter.size());
        }

        long total = repository.countByTenant(tenantId, filter.from(), filter.to());

        return new AuditLogPage(entries, total, filter.page(), filter.size());
    }

    @Override
    public Optional<AuditEntry> findById(UUID id, TenantId tenantId) {
        return repository.findById(id)
                .filter(entry -> entry.tenantId() != null && entry.tenantId().equals(tenantId));
    }
}
