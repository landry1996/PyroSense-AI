package com.pyrosense.ingestion.application.usecase;

import com.pyrosense.ingestion.application.port.in.ListDataQualityIssuesQuery;
import com.pyrosense.ingestion.application.port.out.DataQualityRepositoryPort;
import com.pyrosense.ingestion.domain.model.quality.DataQualityIssue;
import com.pyrosense.shared.exception.NotFoundException;

import java.util.UUID;

public class ListDataQualityIssuesService implements ListDataQualityIssuesQuery {

    private final DataQualityRepositoryPort repository;

    public ListDataQualityIssuesService(DataQualityRepositoryPort repository) {
        this.repository = repository;
    }

    @Override
    public IssuePageResult list(IssueFilter filter) {
        int limit = Math.min(filter.size(), 100);
        int offset = filter.page() * limit;

        var issues = repository.findIssues(
                filter.tenantId(), filter.deviceId(),
                filter.status(), filter.type(),
                offset, limit);

        int totalCount = repository.countIssues(
                filter.tenantId(), filter.deviceId(),
                filter.status(), filter.type());

        int totalPages = (int) Math.ceil((double) totalCount / limit);

        return new IssuePageResult(issues, totalCount, filter.page(), totalPages);
    }

    @Override
    public DataQualityIssue getById(String tenantId, UUID issueId) {
        return repository.findIssueById(issueId)
                .filter(issue -> tenantId.equals(issue.getTenantId()))
                .orElseThrow(() -> new NotFoundException("DataQualityIssue", issueId));
    }
}
