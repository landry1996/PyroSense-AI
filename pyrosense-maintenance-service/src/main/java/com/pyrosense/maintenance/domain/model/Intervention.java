package com.pyrosense.maintenance.domain.model;

import com.pyrosense.shared.domain.AggregateRoot;
import com.pyrosense.shared.exception.InvalidStateTransitionException;
import com.pyrosense.shared.id.AlertId;
import com.pyrosense.shared.id.DeviceId;
import com.pyrosense.shared.id.TenantId;
import com.pyrosense.shared.id.UserId;
import com.pyrosense.shared.util.ClockProvider;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public class Intervention extends AggregateRoot {

    private final UUID id;
    private final TenantId tenantId;
    private final AlertId sourceAlertId;
    private final DeviceId deviceId;
    private final InterventionType type;
    private final InterventionPriority priority;
    private final String description;

    private InterventionStatus status;
    private UserId assignedElectricianId;
    private Instant scheduledAt;
    private Instant startedAt;
    private Instant completedAt;
    private FieldDiagnostic diagnostic;
    private InterventionResult result;
    private RiskImpact riskImpact;
    private final Instant createdAt;
    private Instant updatedAt;

    public Intervention(UUID id, TenantId tenantId, AlertId sourceAlertId, DeviceId deviceId,
                        InterventionType type, InterventionPriority priority, String description) {
        this.id = Objects.requireNonNull(id);
        this.tenantId = Objects.requireNonNull(tenantId);
        this.sourceAlertId = Objects.requireNonNull(sourceAlertId);
        this.deviceId = Objects.requireNonNull(deviceId);
        this.type = Objects.requireNonNull(type);
        this.priority = Objects.requireNonNull(priority);
        this.description = Objects.requireNonNull(description);
        this.status = InterventionStatus.CREATED;
        this.createdAt = ClockProvider.now();
        this.updatedAt = this.createdAt;
    }

    public void schedule(Instant scheduledAt) {
        Objects.requireNonNull(scheduledAt, "scheduledAt must not be null");
        if (scheduledAt.isBefore(ClockProvider.now())) {
            throw new IllegalArgumentException("Cannot schedule in the past");
        }
        transitionTo(InterventionStatus.PLANNED);
        this.scheduledAt = scheduledAt;
        this.updatedAt = ClockProvider.now();
    }

    public void assign(UserId electricianId, Instant scheduledAt) {
        Objects.requireNonNull(electricianId, "electricianId must not be null");
        Objects.requireNonNull(scheduledAt, "scheduledAt must not be null");
        if (this.status == InterventionStatus.CREATED || this.status == InterventionStatus.PLANNED) {
            this.status = InterventionStatus.ASSIGNED;
        } else {
            transitionTo(InterventionStatus.ASSIGNED);
        }
        this.assignedElectricianId = electricianId;
        this.scheduledAt = scheduledAt;
        this.updatedAt = ClockProvider.now();
    }

    public void start() {
        transitionTo(InterventionStatus.IN_PROGRESS);
        this.startedAt = ClockProvider.now();
        this.updatedAt = ClockProvider.now();
    }

    public void addDiagnostic(FieldDiagnostic diagnostic) {
        if (this.status != InterventionStatus.IN_PROGRESS) {
            throw new InvalidStateTransitionException(
                    "Intervention", this.status.name(), "Cannot add diagnostic unless IN_PROGRESS");
        }
        this.diagnostic = Objects.requireNonNull(diagnostic);
        this.updatedAt = ClockProvider.now();
    }

    public void complete(InterventionResult result) {
        Objects.requireNonNull(result, "result must not be null");
        transitionTo(InterventionStatus.COMPLETED);
        this.result = result;
        this.completedAt = ClockProvider.now();
        this.updatedAt = ClockProvider.now();
    }

    public void recordRiskImpact(RiskImpact riskImpact) {
        if (this.status != InterventionStatus.COMPLETED) {
            throw new InvalidStateTransitionException(
                    "Intervention", this.status.name(), "Cannot record risk impact unless COMPLETED");
        }
        this.riskImpact = Objects.requireNonNull(riskImpact);
        this.updatedAt = ClockProvider.now();
    }

    public void cancel() {
        transitionTo(InterventionStatus.CANCELLED);
        this.updatedAt = ClockProvider.now();
    }

    private void transitionTo(InterventionStatus target) {
        if (!this.status.canTransitionTo(target)) {
            throw new InvalidStateTransitionException(
                    "Intervention", this.status.name(), target.name());
        }
        this.status = target;
    }

    public boolean isFalsePositive() {
        return result != null && result.isFalsePositive();
    }

    public boolean isDefectConfirmed() {
        return result != null && result.isDefectConfirmed();
    }

    public boolean requiresFollowUp() {
        return result == InterventionResult.NEEDS_FOLLOW_UP;
    }

    // Getters
    public UUID getId() { return id; }
    public TenantId getTenantId() { return tenantId; }
    public AlertId getSourceAlertId() { return sourceAlertId; }
    public DeviceId getDeviceId() { return deviceId; }
    public InterventionType getType() { return type; }
    public InterventionPriority getPriority() { return priority; }
    public String getDescription() { return description; }
    public InterventionStatus getStatus() { return status; }
    public UserId getAssignedElectricianId() { return assignedElectricianId; }
    public Instant getScheduledAt() { return scheduledAt; }
    public Instant getStartedAt() { return startedAt; }
    public Instant getCompletedAt() { return completedAt; }
    public FieldDiagnostic getDiagnostic() { return diagnostic; }
    public InterventionResult getResult() { return result; }
    public RiskImpact getRiskImpact() { return riskImpact; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
