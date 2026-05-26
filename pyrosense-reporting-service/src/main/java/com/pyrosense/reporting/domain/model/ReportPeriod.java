package com.pyrosense.reporting.domain.model;

import java.time.Instant;
import java.util.Objects;

public record ReportPeriod(Instant start, Instant end) {
    public ReportPeriod {
        Objects.requireNonNull(start, "period start must not be null");
        Objects.requireNonNull(end, "period end must not be null");
        if (end.isBefore(start)) {
            throw new IllegalArgumentException("period end must be after start");
        }
    }
}
