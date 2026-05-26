package com.pyrosense.maintenance.domain.model;

import com.pyrosense.shared.id.AlertId;
import com.pyrosense.shared.id.DeviceId;
import com.pyrosense.shared.id.TenantId;
import com.pyrosense.shared.util.ClockProvider;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public class InterventionRecommendation {

    private final UUID id;
    private final TenantId tenantId;
    private final AlertId alertId;
    private final DeviceId deviceId;
    private final InterventionType suggestedType;
    private final InterventionPriority suggestedPriority;
    private final String reason;
    private final Duration slaDeadline;
    private final Instant createdAt;
    private final Instant slaExpiresAt;

    private RecommendationStatus status;
    private String rejectionReason;
    private UUID acceptedInterventionId;
    private Instant decidedAt;

    public InterventionRecommendation(UUID id, TenantId tenantId, AlertId alertId, DeviceId deviceId,
                                      InterventionType suggestedType, InterventionPriority suggestedPriority,
                                      String reason, Duration slaDeadline) {
        this.id = Objects.requireNonNull(id);
        this.tenantId = Objects.requireNonNull(tenantId);
        this.alertId = Objects.requireNonNull(alertId);
        this.deviceId = Objects.requireNonNull(deviceId);
        this.suggestedType = Objects.requireNonNull(suggestedType);
        this.suggestedPriority = Objects.requireNonNull(suggestedPriority);
        this.reason = Objects.requireNonNull(reason);
        this.slaDeadline = Objects.requireNonNull(slaDeadline);
        this.status = RecommendationStatus.PENDING;
        this.createdAt = ClockProvider.now();
        this.slaExpiresAt = this.createdAt.plus(slaDeadline);
    }

    public void accept(UUID interventionId) {
        if (this.status != RecommendationStatus.PENDING) {
            throw new IllegalStateException("Recommendation already decided: " + this.status);
        }
        this.status = RecommendationStatus.ACCEPTED;
        this.acceptedInterventionId = Objects.requireNonNull(interventionId);
        this.decidedAt = ClockProvider.now();
    }

    public void reject(String reason) {
        if (this.status != RecommendationStatus.PENDING) {
            throw new IllegalStateException("Recommendation already decided: " + this.status);
        }
        Objects.requireNonNull(reason);
        if (reason.isBlank()) {
            throw new IllegalArgumentException("Rejection reason must not be blank");
        }
        this.status = RecommendationStatus.REJECTED;
        this.rejectionReason = reason;
        this.decidedAt = ClockProvider.now();
    }

    public void expire() {
        if (this.status != RecommendationStatus.PENDING) {
            throw new IllegalStateException("Recommendation already decided: " + this.status);
        }
        this.status = RecommendationStatus.EXPIRED;
        this.decidedAt = ClockProvider.now();
    }

    public boolean isSlaBreached() {
        return status == RecommendationStatus.PENDING && ClockProvider.now().isAfter(slaExpiresAt);
    }

    public UUID getId() { return id; }
    public TenantId getTenantId() { return tenantId; }
    public AlertId getAlertId() { return alertId; }
    public DeviceId getDeviceId() { return deviceId; }
    public InterventionType getSuggestedType() { return suggestedType; }
    public InterventionPriority getSuggestedPriority() { return suggestedPriority; }
    public String getReason() { return reason; }
    public Duration getSlaDeadline() { return slaDeadline; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getSlaExpiresAt() { return slaExpiresAt; }
    public RecommendationStatus getStatus() { return status; }
    public String getRejectionReason() { return rejectionReason; }
    public UUID getAcceptedInterventionId() { return acceptedInterventionId; }
    public Instant getDecidedAt() { return decidedAt; }
}
