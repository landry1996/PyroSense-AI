package com.pyrosense.notification.adapter.out.audit;

import com.pyrosense.notification.application.port.out.AuditLogPort;
import com.pyrosense.notification.application.port.out.AuditLogRepository;
import com.pyrosense.shared.id.TenantId;
import com.pyrosense.shared.security.AuditEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class LoggingAuditLogAdapter implements AuditLogPort {

    private static final Logger log = LoggerFactory.getLogger(LoggingAuditLogAdapter.class);

    private final AuditLogRepository repository;

    public LoggingAuditLogAdapter(AuditLogRepository repository) {
        this.repository = repository;
    }

    @Override
    public void log(String action, TenantId tenantId, String details) {
        log.info("[AUDIT] action={} tenant={} details={}", action,
                tenantId != null ? tenantId.value() : "system", details);

        AuditEntry entry = AuditEntry.createFull(
                action, "NOTIFICATION", null, null, null,
                tenantId, null, null, null, details, Map.of());
        try {
            repository.save(entry);
        } catch (Exception e) {
            log.warn("Failed to persist audit entry: {}", e.getMessage());
        }
    }
}
