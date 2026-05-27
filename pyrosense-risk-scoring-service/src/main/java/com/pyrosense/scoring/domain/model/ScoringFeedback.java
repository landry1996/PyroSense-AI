package com.pyrosense.scoring.domain.model;

import com.pyrosense.shared.id.DeviceId;

import java.time.Instant;
import java.util.UUID;

public class ScoringFeedback {

    private final UUID id;
    private final DeviceId deviceId;
    private final String tenantId;
    private final UUID alertId;
    private final UUID interventionId;
    private final FeedbackOutcome outcome;
    private final String anomalyType;
    private final double riskScoreAtAlert;
    private final String feedbackSource;
    private final String comment;
    private final Instant receivedAt;
    private ScoringAdjustment adjustment;

    public ScoringFeedback(UUID id, DeviceId deviceId, String tenantId,
                            UUID alertId, UUID interventionId,
                            FeedbackOutcome outcome, String anomalyType,
                            double riskScoreAtAlert, String feedbackSource,
                            String comment, Instant receivedAt) {
        this.id = id;
        this.deviceId = deviceId;
        this.tenantId = tenantId;
        this.alertId = alertId;
        this.interventionId = interventionId;
        this.outcome = outcome;
        this.anomalyType = anomalyType;
        this.riskScoreAtAlert = riskScoreAtAlert;
        this.feedbackSource = feedbackSource;
        this.comment = comment;
        this.receivedAt = receivedAt;
    }

    public void applyAdjustment(ScoringAdjustment adjustment) {
        this.adjustment = adjustment;
    }

    public UUID getId() { return id; }
    public DeviceId getDeviceId() { return deviceId; }
    public String getTenantId() { return tenantId; }
    public UUID getAlertId() { return alertId; }
    public UUID getInterventionId() { return interventionId; }
    public FeedbackOutcome getOutcome() { return outcome; }
    public String getAnomalyType() { return anomalyType; }
    public double getRiskScoreAtAlert() { return riskScoreAtAlert; }
    public String getFeedbackSource() { return feedbackSource; }
    public String getComment() { return comment; }
    public Instant getReceivedAt() { return receivedAt; }
    public ScoringAdjustment getAdjustment() { return adjustment; }
}
