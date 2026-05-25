package com.pyrosense.reporting.application.usecase;

import com.pyrosense.reporting.application.port.in.GetReportQuery;
import com.pyrosense.reporting.application.port.out.DownloadTokenStorePort;
import com.pyrosense.reporting.application.port.out.ReportRepositoryPort;
import com.pyrosense.reporting.domain.model.DownloadToken;
import com.pyrosense.reporting.domain.model.Report;
import com.pyrosense.reporting.domain.model.ReportType;
import com.pyrosense.shared.exception.NotFoundException;
import com.pyrosense.shared.id.BuildingId;
import com.pyrosense.shared.id.TenantId;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class GetReportService implements GetReportQuery {

    private final ReportRepositoryPort repository;
    private final DownloadTokenStorePort tokenStore;

    public GetReportService(ReportRepositoryPort repository, DownloadTokenStorePort tokenStore) {
        this.repository = repository;
        this.tokenStore = tokenStore;
    }

    @Override
    public Optional<Report> findById(UUID id) {
        return repository.findById(id);
    }

    @Override
    public List<Report> findByTenant(TenantId tenantId) {
        return repository.findByTenantId(tenantId);
    }

    @Override
    public List<Report> findByTenantAndType(TenantId tenantId, ReportType type) {
        return repository.findByTenantIdAndType(tenantId, type);
    }

    @Override
    public List<Report> findByBuilding(BuildingId buildingId) {
        return repository.findByBuildingId(buildingId);
    }

    @Override
    public DownloadToken createDownloadToken(UUID reportId) {
        repository.findById(reportId)
                .orElseThrow(() -> new NotFoundException("Report", reportId.toString()));
        DownloadToken token = DownloadToken.create(reportId);
        tokenStore.store(token);
        return token;
    }

    @Override
    public Optional<Report> findByDownloadToken(String token) {
        return tokenStore.findByToken(token)
                .filter(DownloadToken::isValid)
                .flatMap(dt -> {
                    tokenStore.invalidate(token);
                    return repository.findById(dt.reportId());
                })
                .filter(r -> r.getStatus().isAvailableForDownload());
    }
}
