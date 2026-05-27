package com.pyrosense.ingestion.domain.model.dataset;

import java.time.Instant;
import java.util.UUID;

public record TechnicianFeedback(
        UUID id,
        UUID interventionId,
        String technicianId,
        String pseudonymizedDeviceId,
        DataLabel.LabelValue defectObserved,
        double confidenceLevel,
        String visualInspection,
        String measurementMethod,
        String measurementResult,
        boolean defectConfirmed,
        boolean falsePositive,
        String additionalNotes,
        Instant submittedAt
) {
    public static TechnicianFeedback create(UUID interventionId, String technicianId,
                                             String pseudonymizedDeviceId,
                                             DataLabel.LabelValue defectObserved,
                                             double confidenceLevel,
                                             String visualInspection,
                                             String measurementMethod,
                                             String measurementResult,
                                             boolean defectConfirmed,
                                             boolean falsePositive,
                                             String additionalNotes) {
        return new TechnicianFeedback(
                UUID.randomUUID(), interventionId, technicianId, pseudonymizedDeviceId,
                defectObserved, confidenceLevel, visualInspection, measurementMethod,
                measurementResult, defectConfirmed, falsePositive, additionalNotes,
                Instant.now());
    }

    public DataLabel toLabel() {
        if (falsePositive) {
            return DataLabel.fromTechnician(DataLabel.LabelValue.FALSE_POSITIVE,
                    technicianId, confidenceLevel, additionalNotes);
        }
        return DataLabel.fromTechnician(defectObserved, technicianId, confidenceLevel,
                visualInspection != null ? visualInspection : additionalNotes);
    }

    public boolean isActionable() {
        return defectConfirmed || falsePositive;
    }
}
