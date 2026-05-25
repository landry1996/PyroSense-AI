package com.pyrosense.maintenance.domain.model;

import java.time.Instant;
import java.util.Objects;

public record FieldDiagnostic(
        String observations,
        String measurementsTaken,
        String recommendations,
        String diagnosticBy,
        Instant recordedAt
) {
    public FieldDiagnostic {
        Objects.requireNonNull(observations, "observations must not be null");
        Objects.requireNonNull(diagnosticBy, "diagnosticBy must not be null");
        Objects.requireNonNull(recordedAt, "recordedAt must not be null");
    }
}
