package com.pyrosense.shared.audit;

import com.pyrosense.shared.util.ClockProvider;
import java.time.Instant;
import java.util.Objects;

/**
 * Audit trail metadata attached to entities that need traceability.
 * Immutable: a new instance is created on each update.
 */
public record AuditMetadata(
        Instant createdAt,
        Instant updatedAt,
        String createdBy,
        String updatedBy
) {

    public AuditMetadata {
        Objects.requireNonNull(createdAt, "createdAt must not be null");
        Objects.requireNonNull(createdBy, "createdBy must not be null");
    }

    /**
     * Factory for initial creation.
     */
    public static AuditMetadata create(String actor) {
        var now = ClockProvider.now();
        return new AuditMetadata(now, now, actor, actor);
    }

    /**
     * Returns a new instance reflecting an update.
     */
    public AuditMetadata updatedBy(String actor) {
        return new AuditMetadata(this.createdAt, ClockProvider.now(), this.createdBy, actor);
    }
}
