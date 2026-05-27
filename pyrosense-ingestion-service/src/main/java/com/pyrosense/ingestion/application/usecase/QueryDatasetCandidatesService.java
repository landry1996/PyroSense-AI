package com.pyrosense.ingestion.application.usecase;

import com.pyrosense.ingestion.application.port.in.QueryDatasetCandidatesUseCase;
import com.pyrosense.ingestion.application.port.out.DatasetRepositoryPort;
import com.pyrosense.ingestion.domain.model.dataset.PseudonymizationService;

public class QueryDatasetCandidatesService implements QueryDatasetCandidatesUseCase {

    private final DatasetRepositoryPort repository;
    private final PseudonymizationService pseudonymization;

    public QueryDatasetCandidatesService(DatasetRepositoryPort repository,
                                          PseudonymizationService pseudonymization) {
        this.repository = repository;
        this.pseudonymization = pseudonymization;
    }

    @Override
    public CandidatePageResult query(CandidateFilter filter) {
        String pseudonymizedTenantId = pseudonymization.pseudonymizeTenant(filter.tenantId());
        int limit = Math.min(filter.size(), 100);
        int offset = filter.page() * limit;

        var candidates = repository.findCandidates(
                pseudonymizedTenantId,
                filter.status(),
                filter.minQualityTier(),
                filter.labelFilter(),
                filter.from(),
                filter.to(),
                offset, limit);

        int totalCount = repository.countCandidates(
                pseudonymizedTenantId,
                filter.status(),
                filter.minQualityTier(),
                filter.labelFilter(),
                filter.from(),
                filter.to());

        int totalPages = (int) Math.ceil((double) totalCount / limit);

        return new CandidatePageResult(candidates, totalCount, filter.page(), totalPages);
    }
}
