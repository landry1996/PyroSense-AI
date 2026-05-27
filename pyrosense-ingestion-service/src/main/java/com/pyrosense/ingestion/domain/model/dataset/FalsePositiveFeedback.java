package com.pyrosense.ingestion.domain.model.dataset;

import java.time.Instant;
import java.util.UUID;

public record FalsePositiveFeedback(
        UUID id,
        UUID alertId,
        UUID interventionId,
        String pseudonymizedDeviceId,
        String originalAlertType,
        double riskScoreAtAlert,
        String reason,
        String suggestedThresholdAdjustment,
        Instant reportedAt,
        String reportedBy
) {
    public static FalsePositiveFeedback create(UUID alertId, UUID interventionId,
                                                String pseudonymizedDeviceId,
                                                String originalAlertType,
                                                double riskScoreAtAlert,
                                                String reason,
                                                String suggestedThresholdAdjustment,
                                                String reportedBy) {
        return new FalsePositiveFeedback(UUID.randomUUID(), alertId, interventionId,
                pseudonymizedDeviceId, originalAlertType, riskScoreAtAlert,
                reason, suggestedThresholdAdjustment, Instant.now(), reportedBy);
    }

    public DataLabel toLabel() {
        return DataLabel.fromTechnician(DataLabel.LabelValue.FALSE_POSITIVE,
                reportedBy, 1.0, reason);
    }

    public boolean suggestsThresholdChange() {
        return suggestedThresholdAdjustment != null && !suggestedThresholdAdjustment.isBlank();
    }
}
