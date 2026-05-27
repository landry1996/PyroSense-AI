package com.pyrosense.ingestion.domain.model.dataset;

import java.time.Instant;
import java.util.UUID;

public record DefectConfirmation(
        UUID id,
        UUID interventionId,
        UUID alertId,
        String pseudonymizedDeviceId,
        DataLabel.LabelValue confirmedDefect,
        String defectLocation,
        String severityObserved,
        String correctionApplied,
        double riskScoreAtDetection,
        double riskScoreAfterCorrection,
        Instant confirmedAt,
        String confirmedBy
) {
    public static DefectConfirmation create(UUID interventionId, UUID alertId,
                                             String pseudonymizedDeviceId,
                                             DataLabel.LabelValue confirmedDefect,
                                             String defectLocation,
                                             String severityObserved,
                                             String correctionApplied,
                                             double riskScoreAtDetection,
                                             double riskScoreAfterCorrection,
                                             String confirmedBy) {
        return new DefectConfirmation(UUID.randomUUID(), interventionId, alertId,
                pseudonymizedDeviceId, confirmedDefect, defectLocation, severityObserved,
                correctionApplied, riskScoreAtDetection, riskScoreAfterCorrection,
                Instant.now(), confirmedBy);
    }

    public DataLabel toLabel() {
        return DataLabel.fromTechnician(confirmedDefect, confirmedBy, 1.0,
                "Confirmed via intervention: " + correctionApplied);
    }

    public double riskReduction() {
        return riskScoreAtDetection - riskScoreAfterCorrection;
    }

    public boolean isSignificantCorrection() {
        return riskReduction() >= 20.0;
    }
}
