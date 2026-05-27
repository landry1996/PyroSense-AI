package com.pyrosense.ingestion.application.usecase;

import com.pyrosense.ingestion.application.port.in.MarkDataQualityIssueReviewedUseCase;
import com.pyrosense.ingestion.application.port.out.DataQualityRepositoryPort;
import com.pyrosense.ingestion.domain.model.quality.DataQualityIssue;
import com.pyrosense.shared.exception.NotFoundException;

public class MarkDataQualityIssueReviewedService implements MarkDataQualityIssueReviewedUseCase {

    private final DataQualityRepositoryPort repository;

    public MarkDataQualityIssueReviewedService(DataQualityRepositoryPort repository) {
        this.repository = repository;
    }

    @Override
    public DataQualityIssue execute(ReviewCommand command) {
        var issue = repository.findIssueById(command.issueId())
                .filter(i -> command.tenantId().equals(i.getTenantId()))
                .orElseThrow(() -> new NotFoundException("DataQualityIssue", command.issueId()));

        if (command.dismiss()) {
            issue.dismiss(command.reviewer(), command.comment());
        } else {
            issue.markReviewed(command.reviewer(), command.comment());
        }

        repository.updateIssue(issue);
        return issue;
    }
}
