package com.pyrosense.reporting.application.port.out;

import com.pyrosense.reporting.domain.model.Report;
import com.pyrosense.reporting.domain.model.ReportType;
import com.pyrosense.shared.id.BuildingId;
import com.pyrosense.shared.id.TenantId;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ReportRepositoryPort {

    Report save(Report report);

    Optional<Report> findById(UUID id);

    List<Report> findByTenantId(TenantId tenantId);

    List<Report> findByTenantIdAndType(TenantId tenantId, ReportType type);

    List<Report> findByBuildingId(BuildingId buildingId);
}
