package com.pyrosense.shared.valueobject;

/**
 * Alert severity levels, ordered by urgency.
 */
public enum AlertSeverity {

    INFO(1, "Informational - no immediate action required"),
    WARNING(2, "Warning - attention needed within 48 hours"),
    CRITICAL(3, "Critical - immediate intervention required");

    private final int level;
    private final String description;

    AlertSeverity(int level, String description) {
        this.level = level;
        this.description = description;
    }

    public int level() {
        return level;
    }

    public String description() {
        return description;
    }

    public boolean isHigherThan(AlertSeverity other) {
        return this.level > other.level;
    }

    public boolean isAtLeast(AlertSeverity minimum) {
        return this.level >= minimum.level;
    }
}
