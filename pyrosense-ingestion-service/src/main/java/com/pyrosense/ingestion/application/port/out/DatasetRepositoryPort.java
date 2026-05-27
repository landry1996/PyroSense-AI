package com.pyrosense.ingestion.application.port.out;

import com.pyrosense.ingestion.domain.model.dataset.*;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DatasetRepositoryPort {

    void saveCandidate(DatasetCandidate candidate);

    Optional<DatasetCandidate> findCandidateById(UUID id);

    List<DatasetCandidate> findCandidates(String pseudonymizedTenantId,
                                           DatasetCandidate.CandidateStatus status,
                                           DatasetCandidate.DataQualityTier minQualityTier,
                                           DataLabel.LabelValue labelFilter,
                                           Instant from, Instant to,
                                           int offset, int limit);

    int countCandidates(String pseudonymizedTenantId,
                        DatasetCandidate.CandidateStatus status,
                        DatasetCandidate.DataQualityTier minQualityTier,
                        DataLabel.LabelValue labelFilter,
                        Instant from, Instant to);

    void updateCandidate(DatasetCandidate candidate);

    void saveFeedback(TechnicianFeedback feedback);

    void saveFieldObservation(FieldObservation observation);

    void saveDefectConfirmation(DefectConfirmation confirmation);

    void saveFalsePositiveFeedback(FalsePositiveFeedback feedback);

    void saveExportJob(DatasetExportJob job);

    Optional<DatasetExportJob> findExportJobById(UUID id);

    void updateExportJob(DatasetExportJob job);

    List<DatasetCandidate> findExportableCandidates(String pseudonymizedTenantId,
                                                     DatasetCandidate.DataQualityTier minQualityTier,
                                                     DataLabel.LabelValue labelFilter,
                                                     boolean includeUnlabeled,
                                                     Instant from, Instant to);
}
