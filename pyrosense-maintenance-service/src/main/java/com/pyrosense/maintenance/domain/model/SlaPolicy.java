package com.pyrosense.maintenance.domain.model;

import java.time.Duration;

public record SlaPolicy(InterventionPriority priority, Duration responseDeadline, Duration resolutionDeadline) {

    public static SlaPolicy forPriority(InterventionPriority priority) {
        return switch (priority) {
            case URGENT -> new SlaPolicy(priority, Duration.ofHours(4), Duration.ofHours(24));
            case HIGH -> new SlaPolicy(priority, Duration.ofHours(24), Duration.ofDays(3));
            case MEDIUM -> new SlaPolicy(priority, Duration.ofDays(3), Duration.ofDays(7));
            case LOW -> new SlaPolicy(priority, Duration.ofDays(7), Duration.ofDays(30));
        };
    }

    public boolean isResponseBreached(Duration elapsed) {
        return elapsed.compareTo(responseDeadline) > 0;
    }

    public boolean isResolutionBreached(Duration elapsed) {
        return elapsed.compareTo(resolutionDeadline) > 0;
    }
}
