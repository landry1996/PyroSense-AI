package com.pyrosense.shared.id;

import com.pyrosense.shared.domain.ValueObject;
import java.util.Objects;
import java.util.UUID;

public record RiskAssessmentId(UUID value) implements ValueObject {

    public RiskAssessmentId {
        Objects.requireNonNull(value, "RiskAssessmentId value must not be null");
    }

    public static RiskAssessmentId generate() {
        return new RiskAssessmentId(UUID.randomUUID());
    }

    public static RiskAssessmentId from(String uuid) {
        return new RiskAssessmentId(UUID.fromString(uuid));
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
