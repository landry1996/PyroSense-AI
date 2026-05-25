package com.pyrosense.alerting.domain.model;

import com.pyrosense.shared.valueobject.AlertSeverity;

public enum AlertType {
    MICRO_ARC_DETECTED(AlertSeverity.CRITICAL),
    INSULATION_DEGRADATION(AlertSeverity.WARNING),
    LOOSE_CONNECTION(AlertSeverity.WARNING),
    OVERHEATING(AlertSeverity.CRITICAL),
    ABNORMAL_TRANSIENT(AlertSeverity.WARNING),
    HARMONIC_DISTORTION(AlertSeverity.WARNING),
    LOAD_IMBALANCE(AlertSeverity.INFO),
    SENSOR_OFFLINE(AlertSeverity.INFO),
    CRITICAL_RISK_SCORE(AlertSeverity.CRITICAL),
    HIGH_RISK_SCORE(AlertSeverity.WARNING),
    BASELINE_DEVIATION(AlertSeverity.INFO);

    private final AlertSeverity defaultSeverity;

    AlertType(AlertSeverity defaultSeverity) {
        this.defaultSeverity = defaultSeverity;
    }

    public AlertSeverity defaultSeverity() {
        return defaultSeverity;
    }
}
