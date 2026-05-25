package com.pyrosense.alerting.domain.model;

import com.pyrosense.alerting.domain.event.*;
import com.pyrosense.shared.domain.AggregateRoot;
import com.pyrosense.shared.exception.InvalidStateTransitionException;
import com.pyrosense.shared.id.AlertId;
import com.pyrosense.shared.id.DeviceId;
import com.pyrosense.shared.id.TenantId;
import com.pyrosense.shared.id.UserId;
import com.pyrosense.shared.util.ClockProvider;
import com.pyrosense.shared.valueobject.AlertSeverity;

import java.time.Duration;
import java.time.Instant;
import java.util.*;

public class Alert extends AggregateRoot<AlertId> {

    private final AlertId id;
    private final TenantId tenantId;
    private final DeviceId deviceId;
    private final AlertType type;
    private final AlertSeverity severity;
    private final String title;
    private final String description;
    private final DeduplicationKey deduplicationKey;
    private final Instant createdAt;
    private final Instant slaDeadline;

    private AlertStatus status;
    private UserId assignedTo;
    private EscalationLevel escalationLevel;
    private Instant acknowledgedAt;
    private UserId acknowledgedBy;
    private Instant resolvedAt;
    private UserId resolvedBy;
    private String resolutionNote;
    private Instant lastEscalatedAt;
    private int occurrenceCount;
    private Instant lastOccurrenceAt;
    private final List<AlertComment> comments;

    private Alert(Builder builder) {
        this.id = builder.id;
        this.tenantId = builder.tenantId;
        this.deviceId = builder.deviceId;
        this.type = builder.type;
        this.severity = builder.severity;
        this.title = builder.title;
        this.description = builder.description;
        this.deduplicationKey = new DeduplicationKey(builder.deviceId, builder.type);
        this.createdAt = builder.createdAt;
        this.slaDeadline = builder.slaDeadline;
        this.status = builder.status;
        this.assignedTo = builder.assignedTo;
        this.escalationLevel = builder.escalationLevel;
        this.acknowledgedAt = builder.acknowledgedAt;
        this.acknowledgedBy = builder.acknowledgedBy;
        this.resolvedAt = builder.resolvedAt;
        this.resolvedBy = builder.resolvedBy;
        this.resolutionNote = builder.resolutionNote;
        this.lastEscalatedAt = builder.lastEscalatedAt;
        this.occurrenceCount = builder.occurrenceCount;
        this.lastOccurrenceAt = builder.lastOccurrenceAt;
        this.comments = new ArrayList<>(builder.comments);
    }

    public static Alert create(TenantId tenantId, DeviceId deviceId, AlertType type,
                                AlertSeverity severity, String title, String description,
                                SlaPolicy slaPolicy) {
        Instant now = ClockProvider.now();
        Alert alert = new Builder()
                .id(AlertId.generate())
                .tenantId(tenantId)
                .deviceId(deviceId)
                .type(type)
                .severity(severity)
                .title(title)
                .description(description)
                .createdAt(now)
                .slaDeadline(now.plus(slaPolicy.deadlineFor(severity)))
                .status(AlertStatus.OPEN)
                .escalationLevel(EscalationLevel.NONE)
                .occurrenceCount(1)
                .lastOccurrenceAt(now)
                .build();

        alert.registerEvent(new AlertCreatedEvent(
                UUID.randomUUID(), now, alert.id, tenantId, deviceId, severity, type));
        return alert;
    }

    public void acknowledge(UserId userId) {
        assertTransition(AlertStatus.ACKNOWLEDGED);
        this.status = AlertStatus.ACKNOWLEDGED;
        this.acknowledgedAt = ClockProvider.now();
        this.acknowledgedBy = userId;
        registerEvent(new AlertAcknowledgedEvent(
                UUID.randomUUID(), acknowledgedAt, id, userId));
    }

    public void assign(UserId assignee, UserId assignedByUser) {
        if (status.isTerminal()) {
            throw new InvalidStateTransitionException("Alert", status.name(), "ASSIGNED");
        }
        this.assignedTo = assignee;
        if (status == AlertStatus.OPEN || status == AlertStatus.ACKNOWLEDGED) {
            this.status = AlertStatus.IN_PROGRESS;
        }
        registerEvent(new AlertAssignedEvent(
                UUID.randomUUID(), ClockProvider.now(), id, assignee, assignedByUser));
    }

    public void resolve(UserId userId, String note) {
        assertTransition(AlertStatus.RESOLVED);
        this.status = AlertStatus.RESOLVED;
        this.resolvedAt = ClockProvider.now();
        this.resolvedBy = userId;
        this.resolutionNote = note;
        registerEvent(new AlertResolvedEvent(
                UUID.randomUUID(), resolvedAt, id, userId, false));
    }

    public void markFalsePositive(UserId userId, String reason) {
        assertTransition(AlertStatus.FALSE_POSITIVE);
        this.status = AlertStatus.FALSE_POSITIVE;
        this.resolvedAt = ClockProvider.now();
        this.resolvedBy = userId;
        this.resolutionNote = reason;
        registerEvent(new AlertResolvedEvent(
                UUID.randomUUID(), resolvedAt, id, userId, true));
    }

    public void escalate() {
        if (status.isTerminal()) return;
        EscalationLevel previous = this.escalationLevel;
        this.escalationLevel = this.escalationLevel.next();
        this.lastEscalatedAt = ClockProvider.now();
        registerEvent(new AlertEscalatedEvent(
                UUID.randomUUID(), lastEscalatedAt, id, previous, this.escalationLevel, severity));
    }

    public void recordOccurrence() {
        this.occurrenceCount++;
        this.lastOccurrenceAt = ClockProvider.now();
    }

    public void addComment(UserId author, String content) {
        if (status.isTerminal()) {
            throw new InvalidStateTransitionException("Alert", status.name(), "ADD_COMMENT");
        }
        this.comments.add(AlertComment.create(author, content));
    }

    public boolean isSlaBreached() {
        if (status.isTerminal()) return false;
        return ClockProvider.now().isAfter(slaDeadline);
    }

    public boolean shouldEscalate(Duration escalationInterval) {
        if (status.isTerminal() || status == AlertStatus.IN_PROGRESS) return false;
        if (severity != AlertSeverity.CRITICAL) return false;
        Instant reference = lastEscalatedAt != null ? lastEscalatedAt : createdAt;
        return ClockProvider.now().isAfter(reference.plus(escalationInterval));
    }

    private void assertTransition(AlertStatus target) {
        if (!status.canTransitionTo(target)) {
            throw new InvalidStateTransitionException("Alert", status.name(), target.name());
        }
    }

    @Override
    public AlertId getId() { return id; }
    public TenantId tenantId() { return tenantId; }
    public DeviceId deviceId() { return deviceId; }
    public AlertType type() { return type; }
    public AlertSeverity severity() { return severity; }
    public String title() { return title; }
    public String description() { return description; }
    public DeduplicationKey deduplicationKey() { return deduplicationKey; }
    public AlertStatus status() { return status; }
    public UserId assignedTo() { return assignedTo; }
    public EscalationLevel escalationLevel() { return escalationLevel; }
    public Instant createdAt() { return createdAt; }
    public Instant slaDeadline() { return slaDeadline; }
    public Instant acknowledgedAt() { return acknowledgedAt; }
    public UserId acknowledgedBy() { return acknowledgedBy; }
    public Instant resolvedAt() { return resolvedAt; }
    public UserId resolvedBy() { return resolvedBy; }
    public String resolutionNote() { return resolutionNote; }
    public Instant lastEscalatedAt() { return lastEscalatedAt; }
    public int occurrenceCount() { return occurrenceCount; }
    public Instant lastOccurrenceAt() { return lastOccurrenceAt; }
    public List<AlertComment> comments() { return Collections.unmodifiableList(comments); }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private AlertId id;
        private TenantId tenantId;
        private DeviceId deviceId;
        private AlertType type;
        private AlertSeverity severity;
        private String title;
        private String description;
        private Instant createdAt;
        private Instant slaDeadline;
        private AlertStatus status = AlertStatus.OPEN;
        private UserId assignedTo;
        private EscalationLevel escalationLevel = EscalationLevel.NONE;
        private Instant acknowledgedAt;
        private UserId acknowledgedBy;
        private Instant resolvedAt;
        private UserId resolvedBy;
        private String resolutionNote;
        private Instant lastEscalatedAt;
        private int occurrenceCount = 1;
        private Instant lastOccurrenceAt;
        private List<AlertComment> comments = new ArrayList<>();

        public Builder id(AlertId id) { this.id = id; return this; }
        public Builder tenantId(TenantId tenantId) { this.tenantId = tenantId; return this; }
        public Builder deviceId(DeviceId deviceId) { this.deviceId = deviceId; return this; }
        public Builder type(AlertType type) { this.type = type; return this; }
        public Builder severity(AlertSeverity severity) { this.severity = severity; return this; }
        public Builder title(String title) { this.title = title; return this; }
        public Builder description(String description) { this.description = description; return this; }
        public Builder createdAt(Instant createdAt) { this.createdAt = createdAt; return this; }
        public Builder slaDeadline(Instant slaDeadline) { this.slaDeadline = slaDeadline; return this; }
        public Builder status(AlertStatus status) { this.status = status; return this; }
        public Builder assignedTo(UserId assignedTo) { this.assignedTo = assignedTo; return this; }
        public Builder escalationLevel(EscalationLevel level) { this.escalationLevel = level; return this; }
        public Builder acknowledgedAt(Instant at) { this.acknowledgedAt = at; return this; }
        public Builder acknowledgedBy(UserId by) { this.acknowledgedBy = by; return this; }
        public Builder resolvedAt(Instant at) { this.resolvedAt = at; return this; }
        public Builder resolvedBy(UserId by) { this.resolvedBy = by; return this; }
        public Builder resolutionNote(String note) { this.resolutionNote = note; return this; }
        public Builder lastEscalatedAt(Instant at) { this.lastEscalatedAt = at; return this; }
        public Builder occurrenceCount(int count) { this.occurrenceCount = count; return this; }
        public Builder lastOccurrenceAt(Instant at) { this.lastOccurrenceAt = at; return this; }
        public Builder comments(List<AlertComment> comments) { this.comments = comments; return this; }

        public Alert build() {
            Objects.requireNonNull(id, "id is required");
            Objects.requireNonNull(tenantId, "tenantId is required");
            Objects.requireNonNull(deviceId, "deviceId is required");
            Objects.requireNonNull(type, "type is required");
            Objects.requireNonNull(severity, "severity is required");
            Objects.requireNonNull(title, "title is required");
            Objects.requireNonNull(description, "description is required");
            Objects.requireNonNull(createdAt, "createdAt is required");
            Objects.requireNonNull(slaDeadline, "slaDeadline is required");
            return new Alert(this);
        }
    }
}
