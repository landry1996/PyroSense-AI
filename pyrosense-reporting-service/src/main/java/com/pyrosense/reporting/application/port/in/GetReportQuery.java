package com.pyrosense.reporting.application.port.in;

import com.pyrosense.reporting.domain.model.DownloadToken;
import com.pyrosense.reporting.domain.model.Report;
import com.pyrosense.reporting.domain.model.ReportType;
import com.pyrosense.shared.id.BuildingId;
import com.pyrosense.shared.id.TenantId;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface GetReportQuery {

    Optional<Report> findById(UUID id);

    List<Report> findByTenant(TenantId tenantId);

    List<Report> findByTenantAndType(TenantId tenantId, ReportType type);

    List<Report> findByBuilding(BuildingId buildingId);

    DownloadToken createDownloadToken(UUID reportId);

    Optional<Report> findByDownloadToken(String token);
}
