package com.pyrosense.identity.adapter.out.persistence;

import com.pyrosense.identity.application.port.out.AuditLogRepository;
import com.pyrosense.shared.id.TenantId;
import com.pyrosense.shared.security.AuditEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Repository
public class InMemoryAuditLogRepository implements AuditLogRepository {

    private static final Logger log = LoggerFactory.getLogger(InMemoryAuditLogRepository.class);
    private final List<AuditEntry> entries = new CopyOnWriteArrayList<>();

    @Override
    public void save(AuditEntry entry) {
        entries.add(entry);
        log.info("AUDIT: {} | user={} | tenant={} | resource={}:{}",
                entry.action(),
                entry.userId() != null ? entry.userId().value() : "system",
                entry.tenantId() != null ? entry.tenantId().value() : "none",
                entry.resourceType(), entry.resourceId());
    }

    @Override
    public List<AuditEntry> findByTenant(TenantId tenantId, Instant from, Instant to) {
        return entries.stream()
                .filter(e -> e.tenantId() != null && e.tenantId().equals(tenantId))
                .filter(e -> !e.timestamp().isBefore(from) && !e.timestamp().isAfter(to))
                .toList();
    }
}
