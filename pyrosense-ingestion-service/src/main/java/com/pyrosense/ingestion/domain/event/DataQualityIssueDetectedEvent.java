package com.pyrosense.ingestion.domain.event;

import com.pyrosense.ingestion.domain.model.quality.DataQualityIssue;
import com.pyrosense.shared.domain.DomainEvent;

import java.time.Instant;
import java.util.UUID;

public record DataQualityIssueDetectedEvent(
        UUID eventId,
        Instant occurredAt,
        String deviceId,
        String tenantId,
        DataQualityIssue.IssueType issueType,
        DataQualityIssue.IssueSeverity severity,
        String details
) implements DomainEvent {

    @Override
    public String eventType() {
        return "ingestion.data_quality.issue_detected";
    }
}
