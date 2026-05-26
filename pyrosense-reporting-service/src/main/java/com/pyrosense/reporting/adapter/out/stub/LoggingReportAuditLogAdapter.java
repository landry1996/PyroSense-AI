package com.pyrosense.reporting.adapter.out.stub;

import com.pyrosense.reporting.application.port.out.ReportAuditLogPort;
import com.pyrosense.shared.id.TenantId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class LoggingReportAuditLogAdapter implements ReportAuditLogPort {

    private static final Logger log = LoggerFactory.getLogger(LoggingReportAuditLogAdapter.class);

    @Override
    public void log(TenantId tenantId, UUID userId, UUID reportId, String action, String details) {
        log.info("AUDIT [tenant={}, user={}, report={}, action={}] {}",
                tenantId.value(), userId != null ? userId : "system",
                reportId, action, details);
    }
}
