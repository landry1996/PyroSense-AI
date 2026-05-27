package com.pyrosense.ingestion.application.port.in;

import com.pyrosense.ingestion.domain.model.quality.DataQualityIssue;

import java.util.UUID;

public interface MarkDataQualityIssueReviewedUseCase {

    record ReviewCommand(
            UUID issueId,
            String tenantId,
            String reviewer,
            String comment,
            boolean dismiss
    ) {}

    DataQualityIssue execute(ReviewCommand command);
}
