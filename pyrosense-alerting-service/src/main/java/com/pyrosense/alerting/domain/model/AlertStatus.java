package com.pyrosense.alerting.domain.model;

public enum AlertStatus {
    OPEN,
    ACKNOWLEDGED,
    IN_PROGRESS,
    RESOLVED,
    FALSE_POSITIVE;

    public boolean canTransitionTo(AlertStatus target) {
        return switch (this) {
            case OPEN -> target == ACKNOWLEDGED || target == IN_PROGRESS || target == RESOLVED || target == FALSE_POSITIVE;
            case ACKNOWLEDGED -> target == IN_PROGRESS || target == RESOLVED || target == FALSE_POSITIVE;
            case IN_PROGRESS -> target == RESOLVED || target == FALSE_POSITIVE;
            case RESOLVED, FALSE_POSITIVE -> false;
        };
    }

    public boolean isTerminal() {
        return this == RESOLVED || this == FALSE_POSITIVE;
    }
}
