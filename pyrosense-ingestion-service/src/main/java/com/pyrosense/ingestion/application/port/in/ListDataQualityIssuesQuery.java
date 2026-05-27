package com.pyrosense.ingestion.application.port.in;

import com.pyrosense.ingestion.domain.model.quality.DataQualityIssue;

import java.util.List;

public interface ListDataQualityIssuesQuery {

    record IssueFilter(
            String tenantId,
            String deviceId,
            DataQualityIssue.IssueStatus status,
            DataQualityIssue.IssueType type,
            int page,
            int size
    ) {}

    record IssuePageResult(
            List<DataQualityIssue> issues,
            int totalCount,
            int page,
            int totalPages
    ) {}

    IssuePageResult list(IssueFilter filter);

    DataQualityIssue getById(String tenantId, java.util.UUID issueId);
}
