package com.pyrosense.reporting.domain.model;

public enum ReportType {

    MONTHLY_HEALTH,
    CONTINUOUS_MONITORING_CERTIFICATE,
    CRITICAL_ALERT_REPORT,
    INTERVENTION_REPORT,
    ROI_AVOIDED_INCIDENTS,
    INSURER_EXPORT;

    public boolean isComplianceCertificate() {
        return this == CONTINUOUS_MONITORING_CERTIFICATE;
    }

    public boolean isInsurerAccessible() {
        return this == INSURER_EXPORT || this == ROI_AVOIDED_INCIDENTS
                || this == CONTINUOUS_MONITORING_CERTIFICATE;
    }
}
