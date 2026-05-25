package com.pyrosense.shared.security;

public enum Permission {
    // User management
    USER_CREATE,
    USER_READ,
    USER_UPDATE,
    USER_DELETE,

    // Tenant management
    TENANT_CREATE,
    TENANT_READ,
    TENANT_UPDATE,

    // Device management
    DEVICE_REGISTER,
    DEVICE_PROVISION,
    DEVICE_ACTIVATE,
    DEVICE_REVOKE,
    DEVICE_READ,

    // Telemetry
    TELEMETRY_INGEST,
    TELEMETRY_READ,

    // Alerts
    ALERT_READ,
    ALERT_ACKNOWLEDGE,
    ALERT_ASSIGN,
    ALERT_RESOLVE,

    // Risk
    RISK_READ,
    RISK_EXPORT,

    // Reports
    REPORT_GENERATE,
    REPORT_READ,

    // Maintenance
    MAINTENANCE_SCHEDULE,
    MAINTENANCE_READ,
    MAINTENANCE_UPDATE,

    // System
    SYSTEM_ADMIN,
    AUDIT_READ;

    public String authority() {
        return "PERM_" + name();
    }
}
