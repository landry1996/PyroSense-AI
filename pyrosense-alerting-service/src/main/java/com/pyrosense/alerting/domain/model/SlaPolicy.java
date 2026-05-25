package com.pyrosense.alerting.domain.model;

import com.pyrosense.shared.valueobject.AlertSeverity;

import java.time.Duration;

public record SlaPolicy(
        Duration criticalDeadline,
        Duration warningDeadline,
        Duration infoDeadline,
        Duration escalationInterval
) {
    public static SlaPolicy defaults() {
        return new SlaPolicy(
                Duration.ofHours(24),
                Duration.ofDays(7),
                Duration.ofDays(30),
                Duration.ofHours(4)
        );
    }

    public Duration deadlineFor(AlertSeverity severity) {
        return switch (severity) {
            case CRITICAL -> criticalDeadline;
            case WARNING -> warningDeadline;
            case INFO -> infoDeadline;
        };
    }
}
