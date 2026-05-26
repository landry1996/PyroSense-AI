package com.pyrosense.notification.adapter.out.audit;

import com.pyrosense.notification.application.port.out.AuditLogPort;
import com.pyrosense.shared.id.TenantId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class LoggingAuditLogAdapter implements AuditLogPort {

    private static final Logger log = LoggerFactory.getLogger(LoggingAuditLogAdapter.class);

    @Override
    public void log(String action, TenantId tenantId, String details) {
        log.info("[AUDIT] action={} tenant={} details={}", action,
                tenantId != null ? tenantId.value() : "system", details);
    }
}
