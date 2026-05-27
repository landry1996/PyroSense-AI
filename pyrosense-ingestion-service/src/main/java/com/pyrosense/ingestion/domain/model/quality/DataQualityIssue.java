package com.pyrosense.ingestion.domain.model.quality;

import java.time.Instant;
import java.util.UUID;

public class DataQualityIssue {

    public enum IssueSeverity { LOW, MEDIUM, HIGH, CRITICAL }
    public enum IssueStatus { OPEN, REVIEWED, DISMISSED }

    public enum IssueType {
        MISSING_DATA("Données manquantes détectées"),
        OUT_OF_RANGE("Valeurs hors plage physique"),
        TIMESTAMP_INCONSISTENT("Horodatage incohérent"),
        SEQUENCE_ANOMALY("Numéro de séquence manquant ou doublon"),
        EXCESSIVE_NOISE("Bruit capteur excessif"),
        LOW_SIGNAL_QUALITY("Qualité signal faible"),
        FREQUENT_OFFLINE("Capteur offline fréquent"),
        OBSOLETE_FIRMWARE("Firmware obsolète"),
        SENSOR_DRIFT("Dérive suspecte du capteur"),
        UNKNOWN_CALIBRATION("Calibration inconnue");

        private final String description;

        IssueType(String description) {
            this.description = description;
        }

        public String getDescription() { return description; }
    }

    private final UUID id;
    private final String deviceId;
    private final String tenantId;
    private final IssueType type;
    private final IssueSeverity severity;
    private final String details;
    private final Instant detectedAt;
    private IssueStatus status;
    private String reviewedBy;
    private Instant reviewedAt;
    private String reviewComment;

    public DataQualityIssue(UUID id, String deviceId, String tenantId,
                             IssueType type, IssueSeverity severity,
                             String details, Instant detectedAt) {
        this.id = id;
        this.deviceId = deviceId;
        this.tenantId = tenantId;
        this.type = type;
        this.severity = severity;
        this.details = details;
        this.detectedAt = detectedAt;
        this.status = IssueStatus.OPEN;
    }

    public static DataQualityIssue create(String deviceId, String tenantId,
                                           IssueType type, IssueSeverity severity,
                                           String details) {
        return new DataQualityIssue(UUID.randomUUID(), deviceId, tenantId,
                type, severity, details, Instant.now());
    }

    public void markReviewed(String reviewer, String comment) {
        this.status = IssueStatus.REVIEWED;
        this.reviewedBy = reviewer;
        this.reviewedAt = Instant.now();
        this.reviewComment = comment;
    }

    public void dismiss(String reviewer, String reason) {
        this.status = IssueStatus.DISMISSED;
        this.reviewedBy = reviewer;
        this.reviewedAt = Instant.now();
        this.reviewComment = reason;
    }

    public boolean isOpen() { return status == IssueStatus.OPEN; }
    public boolean blocksAlertEscalation() { return severity == IssueSeverity.HIGH || severity == IssueSeverity.CRITICAL; }
    public boolean excludeFromMlDataset() { return severity == IssueSeverity.CRITICAL; }

    public UUID getId() { return id; }
    public String getDeviceId() { return deviceId; }
    public String getTenantId() { return tenantId; }
    public IssueType getType() { return type; }
    public IssueSeverity getSeverity() { return severity; }
    public String getDetails() { return details; }
    public Instant getDetectedAt() { return detectedAt; }
    public IssueStatus getStatus() { return status; }
    public String getReviewedBy() { return reviewedBy; }
    public Instant getReviewedAt() { return reviewedAt; }
    public String getReviewComment() { return reviewComment; }
}
