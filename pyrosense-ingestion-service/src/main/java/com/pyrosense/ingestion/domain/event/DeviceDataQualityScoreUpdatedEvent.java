package com.pyrosense.ingestion.domain.event;

import com.pyrosense.ingestion.domain.model.quality.DataQualityAssessment;
import com.pyrosense.shared.domain.DomainEvent;

import java.time.Instant;
import java.util.UUID;

public record DeviceDataQualityScoreUpdatedEvent(
        UUID eventId,
        Instant occurredAt,
        String deviceId,
        String tenantId,
        int overallScore,
        DataQualityAssessment.QualityGrade grade,
        int issueCount,
        boolean trustworthy
) implements DomainEvent {

    @Override
    public String eventType() {
        return "ingestion.data_quality.score_updated";
    }
}
