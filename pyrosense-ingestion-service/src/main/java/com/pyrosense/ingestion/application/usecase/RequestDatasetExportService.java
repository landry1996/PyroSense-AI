package com.pyrosense.ingestion.application.usecase;

import com.pyrosense.ingestion.application.port.in.RequestDatasetExportUseCase;
import com.pyrosense.ingestion.application.port.out.DatasetAuditPort;
import com.pyrosense.ingestion.application.port.out.DatasetRepositoryPort;
import com.pyrosense.ingestion.domain.model.dataset.DatasetExportJob;
import com.pyrosense.ingestion.domain.model.dataset.PseudonymizationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;
import java.util.UUID;

public class RequestDatasetExportService implements RequestDatasetExportUseCase {

    private static final Logger log = LoggerFactory.getLogger(RequestDatasetExportService.class);

    private final DatasetRepositoryPort repository;
    private final DatasetAuditPort auditPort;
    private final PseudonymizationService pseudonymization;

    public RequestDatasetExportService(DatasetRepositoryPort repository,
                                        DatasetAuditPort auditPort,
                                        PseudonymizationService pseudonymization) {
        this.repository = repository;
        this.auditPort = auditPort;
        this.pseudonymization = pseudonymization;
    }

    @Override
    public DatasetExportJob requestExport(ExportCommand command) {
        String pseudonymizedTenantId = pseudonymization.pseudonymizeTenant(command.tenantId());

        var job = new DatasetExportJob(
                command.requestedBy(),
                pseudonymizedTenantId,
                command.format(),
                command.windowStart(),
                command.windowEnd(),
                command.labelFilter(),
                command.minQualityTier(),
                command.includeUnlabeled());

        repository.saveExportJob(job);

        auditPort.logExportRequested(
                command.tenantId(),
                command.requestedBy(),
                job.getId(),
                command.format().name());

        log.info("Dataset export requested: job={} tenant={} format={} window=[{}, {}]",
                job.getId(), pseudonymizedTenantId, command.format(),
                command.windowStart(), command.windowEnd());

        return job;
    }

    @Override
    public Optional<DatasetExportJob> getExportJob(UUID jobId, String tenantId) {
        String pseudonymizedTenantId = pseudonymization.pseudonymizeTenant(tenantId);
        return repository.findExportJobById(jobId)
                .filter(job -> pseudonymizedTenantId.equals(job.getPseudonymizedTenantId()));
    }
}
