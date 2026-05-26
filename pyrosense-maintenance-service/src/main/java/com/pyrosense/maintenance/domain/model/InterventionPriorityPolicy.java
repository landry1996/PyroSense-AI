package com.pyrosense.maintenance.domain.model;

import java.time.Duration;

public class InterventionPriorityPolicy {

    public InterventionPriority determineFromAlert(String severity, Integer riskScore) {
        return switch (severity.toUpperCase()) {
            case "CRITICAL" -> InterventionPriority.URGENT;
            case "WARNING" -> determineWarningPriority(riskScore);
            default -> InterventionPriority.MEDIUM;
        };
    }

    public InterventionType determineTypeFromAlert(String severity) {
        return switch (severity.toUpperCase()) {
            case "CRITICAL" -> InterventionType.EMERGENCY;
            case "WARNING" -> InterventionType.PREVENTIVE;
            default -> InterventionType.PREDICTIVE;
        };
    }

    public Duration determineSlaDeadline(InterventionPriority priority) {
        return SlaPolicy.forPriority(priority).responseDeadline();
    }

    private InterventionPriority determineWarningPriority(Integer riskScore) {
        if (riskScore == null) {
            return InterventionPriority.HIGH;
        }
        if (riskScore >= 70) {
            return InterventionPriority.HIGH;
        }
        return InterventionPriority.MEDIUM;
    }
}
