package com.pyrosense.ingestion.domain.model.dataset;

import java.time.Instant;
import java.util.UUID;

public record DataLabel(
        UUID id,
        LabelValue value,
        LabelSource source,
        double confidence,
        String labeledBy,
        Instant labeledAt,
        String justification
) {
    public enum LabelValue {
        NORMAL,
        MICRO_ARC_SUSPECTED,
        MICRO_ARC_CONFIRMED,
        LOOSE_CONNECTION_CONFIRMED,
        INSULATION_DEGRADATION_CONFIRMED,
        OVERLOAD_CONFIRMED,
        SENSOR_NOISE,
        FALSE_POSITIVE,
        INCONCLUSIVE
    }

    public enum LabelSource {
        TECHNICIAN,
        LAB,
        SYSTEM,
        MANUAL_REVIEW
    }

    public static DataLabel fromTechnician(LabelValue value, String technicianId, double confidence, String justification) {
        return new DataLabel(UUID.randomUUID(), value, LabelSource.TECHNICIAN, confidence,
                technicianId, Instant.now(), justification);
    }

    public static DataLabel fromSystem(LabelValue value, double confidence) {
        return new DataLabel(UUID.randomUUID(), value, LabelSource.SYSTEM, confidence,
                "system", Instant.now(), null);
    }

    public static DataLabel fromLab(LabelValue value, String researcher, String justification) {
        return new DataLabel(UUID.randomUUID(), value, LabelSource.LAB, 1.0,
                researcher, Instant.now(), justification);
    }

    public boolean isConfirmed() {
        return value.name().contains("CONFIRMED") || value == LabelValue.FALSE_POSITIVE;
    }

    public boolean isHighConfidence() {
        return confidence >= 0.9;
    }
}
