package com.pyrosense.reporting.application.port.out;

import com.pyrosense.reporting.domain.model.Report;
import com.pyrosense.reporting.domain.model.ReportMetadata;
import com.pyrosense.reporting.domain.model.ReportType;
import com.pyrosense.shared.id.TenantId;

import java.time.Instant;

public interface ReportRendererPort {

    byte[] render(ReportRenderRequest request);

    record ReportRenderRequest(
            String reportNumber,
            ReportType type,
            String tenantName,
            Instant periodStart,
            Instant periodEnd,
            ReportMetadata metadata
    ) {}
}
