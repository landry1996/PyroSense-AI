package com.pyrosense.reporting.application.port.out;

import com.pyrosense.shared.id.TenantId;

import java.util.UUID;

public interface ReportAuditLogPort {

    void log(TenantId tenantId, UUID userId, UUID reportId, String action, String details);
}
