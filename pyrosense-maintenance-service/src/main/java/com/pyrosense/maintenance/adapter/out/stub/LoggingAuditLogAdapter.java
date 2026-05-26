package com.pyrosense.maintenance.adapter.out.stub;

import com.pyrosense.maintenance.application.port.out.AuditLogPort;
import com.pyrosense.shared.id.TenantId;
import com.pyrosense.shared.id.UserId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class LoggingAuditLogAdapter implements AuditLogPort {

    private static final Logger log = LoggerFactory.getLogger(LoggingAuditLogAdapter.class);

    @Override
    public void log(TenantId tenantId, UserId actor, UUID interventionId, String action, String details) {
        log.info("AUDIT [tenant={}, actor={}, intervention={}, action={}] {}",
                tenantId.value(), actor.value(), interventionId, action, details);
    }
}
