package com.pyrosense.ingestion.application.port.in;

import com.pyrosense.ingestion.domain.model.dataset.DataLabel;
import com.pyrosense.ingestion.domain.model.dataset.DatasetCandidate;

import java.time.Instant;
import java.util.List;

public interface QueryDatasetCandidatesUseCase {

    record CandidateFilter(
            String tenantId,
            DatasetCandidate.CandidateStatus status,
            DatasetCandidate.DataQualityTier minQualityTier,
            DataLabel.LabelValue labelFilter,
            Instant from,
            Instant to,
            int page,
            int size
    ) {}

    record CandidatePageResult(
            List<DatasetCandidate> candidates,
            int totalCount,
            int page,
            int totalPages
    ) {}

    CandidatePageResult query(CandidateFilter filter);
}
