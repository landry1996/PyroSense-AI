package com.pyrosense.ingestion.domain.model.dataset;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class DatasetCandidate {

    public enum CandidateStatus {
        PENDING_LABEL,
        LABELED,
        VALIDATED,
        EXPORTED,
        REJECTED
    }

    public enum DataQualityTier {
        HIGH,
        MEDIUM,
        LOW,
        INSUFFICIENT
    }

    private final UUID id;
    private final String pseudonymizedDeviceId;
    private final String pseudonymizedTenantId;
    private final Instant windowStart;
    private final Instant windowEnd;
    private CandidateStatus status;
    private DataQualityTier qualityTier;
    private final int dataQualityScore;
    private final double riskScoreAtTime;
    private final String anomalyType;
    private final UUID sourceAlertId;
    private final UUID sourceInterventionId;
    private final List<DataLabel> labels;
    private final FeatureSummary featureSummary;
    private final Instant createdAt;
    private Instant exportedAt;

    public DatasetCandidate(UUID id, String pseudonymizedDeviceId, String pseudonymizedTenantId,
                            Instant windowStart, Instant windowEnd,
                            int dataQualityScore, double riskScoreAtTime,
                            String anomalyType, UUID sourceAlertId,
                            UUID sourceInterventionId, FeatureSummary featureSummary) {
        this.id = id;
        this.pseudonymizedDeviceId = pseudonymizedDeviceId;
        this.pseudonymizedTenantId = pseudonymizedTenantId;
        this.windowStart = windowStart;
        this.windowEnd = windowEnd;
        this.status = CandidateStatus.PENDING_LABEL;
        this.dataQualityScore = dataQualityScore;
        this.qualityTier = computeQualityTier(dataQualityScore);
        this.riskScoreAtTime = riskScoreAtTime;
        this.anomalyType = anomalyType;
        this.sourceAlertId = sourceAlertId;
        this.sourceInterventionId = sourceInterventionId;
        this.labels = new ArrayList<>();
        this.featureSummary = featureSummary;
        this.createdAt = Instant.now();
    }

    public void addLabel(DataLabel label) {
        this.labels.add(label);
        if (label.isConfirmed() && label.isHighConfidence()) {
            this.status = CandidateStatus.VALIDATED;
        } else if (this.status == CandidateStatus.PENDING_LABEL) {
            this.status = CandidateStatus.LABELED;
        }
    }

    public void markExported() {
        this.status = CandidateStatus.EXPORTED;
        this.exportedAt = Instant.now();
    }

    public void reject(String reason) {
        this.status = CandidateStatus.REJECTED;
    }

    public boolean isExportable() {
        return (status == CandidateStatus.LABELED || status == CandidateStatus.VALIDATED)
                && qualityTier != DataQualityTier.INSUFFICIENT
                && !labels.isEmpty();
    }

    public DataLabel getPrimaryLabel() {
        return labels.stream()
                .filter(DataLabel::isHighConfidence)
                .findFirst()
                .orElse(labels.isEmpty() ? null : labels.get(0));
    }

    private static DataQualityTier computeQualityTier(int score) {
        if (score >= 90) return DataQualityTier.HIGH;
        if (score >= 75) return DataQualityTier.MEDIUM;
        if (score >= 50) return DataQualityTier.LOW;
        return DataQualityTier.INSUFFICIENT;
    }

    public UUID getId() { return id; }
    public String getPseudonymizedDeviceId() { return pseudonymizedDeviceId; }
    public String getPseudonymizedTenantId() { return pseudonymizedTenantId; }
    public Instant getWindowStart() { return windowStart; }
    public Instant getWindowEnd() { return windowEnd; }
    public CandidateStatus getStatus() { return status; }
    public DataQualityTier getQualityTier() { return qualityTier; }
    public int getDataQualityScore() { return dataQualityScore; }
    public double getRiskScoreAtTime() { return riskScoreAtTime; }
    public String getAnomalyType() { return anomalyType; }
    public UUID getSourceAlertId() { return sourceAlertId; }
    public UUID getSourceInterventionId() { return sourceInterventionId; }
    public List<DataLabel> getLabels() { return List.copyOf(labels); }
    public FeatureSummary getFeatureSummary() { return featureSummary; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getExportedAt() { return exportedAt; }
}
