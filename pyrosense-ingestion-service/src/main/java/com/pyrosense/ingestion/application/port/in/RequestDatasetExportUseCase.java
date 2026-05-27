package com.pyrosense.ingestion.application.port.in;

import com.pyrosense.ingestion.domain.model.dataset.DataLabel;
import com.pyrosense.ingestion.domain.model.dataset.DatasetCandidate;
import com.pyrosense.ingestion.domain.model.dataset.DatasetExportJob;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface RequestDatasetExportUseCase {

    record ExportCommand(
            String requestedBy,
            String tenantId,
            DatasetExportJob.ExportFormat format,
            Instant windowStart,
            Instant windowEnd,
            DataLabel.LabelValue labelFilter,
            DatasetCandidate.DataQualityTier minQualityTier,
            boolean includeUnlabeled
    ) {}

    DatasetExportJob requestExport(ExportCommand command);

    Optional<DatasetExportJob> getExportJob(UUID jobId, String tenantId);
}
