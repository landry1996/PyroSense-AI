package com.pyrosense.identity.domain.model;

import com.pyrosense.shared.security.Permission;
import com.pyrosense.shared.security.PlatformRole;

import java.util.EnumSet;
import java.util.Set;

public enum Role {
    PLATFORM_ADMIN(PlatformRole.PLATFORM_ADMIN, EnumSet.allOf(Permission.class)),

    TENANT_ADMIN(PlatformRole.TENANT_ADMIN, EnumSet.of(
            Permission.USER_CREATE, Permission.USER_READ, Permission.USER_UPDATE, Permission.USER_DELETE,
            Permission.TENANT_READ, Permission.TENANT_UPDATE,
            Permission.DEVICE_REGISTER, Permission.DEVICE_PROVISION, Permission.DEVICE_ACTIVATE, Permission.DEVICE_REVOKE, Permission.DEVICE_READ,
            Permission.TELEMETRY_READ,
            Permission.ALERT_READ, Permission.ALERT_ACKNOWLEDGE, Permission.ALERT_ASSIGN, Permission.ALERT_RESOLVE,
            Permission.RISK_READ, Permission.RISK_EXPORT,
            Permission.REPORT_GENERATE, Permission.REPORT_READ,
            Permission.MAINTENANCE_SCHEDULE, Permission.MAINTENANCE_READ, Permission.MAINTENANCE_UPDATE,
            Permission.AUDIT_READ
    )),

    PROPERTY_MANAGER(PlatformRole.PROPERTY_MANAGER, EnumSet.of(
            Permission.USER_READ,
            Permission.DEVICE_READ, Permission.DEVICE_REGISTER,
            Permission.TELEMETRY_READ,
            Permission.ALERT_READ, Permission.ALERT_ACKNOWLEDGE, Permission.ALERT_ASSIGN,
            Permission.RISK_READ,
            Permission.REPORT_GENERATE, Permission.REPORT_READ,
            Permission.MAINTENANCE_SCHEDULE, Permission.MAINTENANCE_READ, Permission.MAINTENANCE_UPDATE
    )),

    OCCUPANT(PlatformRole.OCCUPANT, EnumSet.of(
            Permission.ALERT_READ,
            Permission.RISK_READ,
            Permission.REPORT_READ,
            Permission.MAINTENANCE_READ
    )),

    ELECTRICIAN(PlatformRole.ELECTRICIAN, EnumSet.of(
            Permission.DEVICE_READ,
            Permission.TELEMETRY_READ,
            Permission.ALERT_READ, Permission.ALERT_ACKNOWLEDGE, Permission.ALERT_RESOLVE,
            Permission.RISK_READ,
            Permission.MAINTENANCE_READ, Permission.MAINTENANCE_UPDATE
    )),

    INSURANCE_PARTNER(PlatformRole.INSURANCE_PARTNER, EnumSet.of(
            Permission.RISK_READ, Permission.RISK_EXPORT,
            Permission.REPORT_READ,
            Permission.ALERT_READ
    )),

    DEVICE(PlatformRole.DEVICE, EnumSet.of(
            Permission.TELEMETRY_INGEST,
            Permission.DEVICE_READ
    )),

    SUPPORT_READONLY(PlatformRole.SUPPORT_READONLY, EnumSet.of(
            Permission.USER_READ,
            Permission.DEVICE_READ,
            Permission.TELEMETRY_READ,
            Permission.ALERT_READ,
            Permission.RISK_READ,
            Permission.REPORT_READ,
            Permission.MAINTENANCE_READ,
            Permission.AUDIT_READ
    ));

    private final PlatformRole platformRole;
    private final Set<Permission> permissions;

    Role(PlatformRole platformRole, Set<Permission> permissions) {
        this.platformRole = platformRole;
        this.permissions = permissions;
    }

    public PlatformRole platformRole() {
        return platformRole;
    }

    public Set<Permission> permissions() {
        return permissions;
    }

    public boolean hasPermission(Permission permission) {
        return permissions.contains(permission);
    }
}
