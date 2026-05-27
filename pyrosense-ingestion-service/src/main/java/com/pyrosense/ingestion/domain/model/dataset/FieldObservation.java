package com.pyrosense.ingestion.domain.model.dataset;

import java.time.Instant;
import java.util.UUID;

public class FieldObservation {

    private final UUID id;
    private final String pseudonymizedDeviceId;
    private final String pseudonymizedTenantId;
    private final UUID interventionId;
    private final Instant observedAt;
    private final Instant windowStart;
    private final Instant windowEnd;
    private final String environmentType;
    private final String installationType;
    private final String circuitType;
    private final String notes;
    private DataLabel label;

    public FieldObservation(UUID id, String pseudonymizedDeviceId, String pseudonymizedTenantId,
                            UUID interventionId, Instant observedAt, Instant windowStart,
                            Instant windowEnd, String environmentType, String installationType,
                            String circuitType, String notes) {
        this.id = id;
        this.pseudonymizedDeviceId = pseudonymizedDeviceId;
        this.pseudonymizedTenantId = pseudonymizedTenantId;
        this.interventionId = interventionId;
        this.observedAt = observedAt;
        this.windowStart = windowStart;
        this.windowEnd = windowEnd;
        this.environmentType = environmentType;
        this.installationType = installationType;
        this.circuitType = circuitType;
        this.notes = notes;
    }

    public void applyLabel(DataLabel label) {
        this.label = label;
    }

    public boolean isLabeled() {
        return label != null;
    }

    public UUID getId() { return id; }
    public String getPseudonymizedDeviceId() { return pseudonymizedDeviceId; }
    public String getPseudonymizedTenantId() { return pseudonymizedTenantId; }
    public UUID getInterventionId() { return interventionId; }
    public Instant getObservedAt() { return observedAt; }
    public Instant getWindowStart() { return windowStart; }
    public Instant getWindowEnd() { return windowEnd; }
    public String getEnvironmentType() { return environmentType; }
    public String getInstallationType() { return installationType; }
    public String getCircuitType() { return circuitType; }
    public String getNotes() { return notes; }
    public DataLabel getLabel() { return label; }
}
