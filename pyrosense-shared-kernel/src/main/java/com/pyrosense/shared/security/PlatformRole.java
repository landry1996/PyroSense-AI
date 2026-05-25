package com.pyrosense.shared.security;

public enum PlatformRole {
    PLATFORM_ADMIN,
    TENANT_ADMIN,
    PROPERTY_MANAGER,
    OCCUPANT,
    ELECTRICIAN,
    INSURANCE_PARTNER,
    DEVICE,
    SUPPORT_READONLY;

    public String authority() {
        return "ROLE_" + name();
    }

    public boolean isAdminLevel() {
        return this == PLATFORM_ADMIN || this == TENANT_ADMIN;
    }

    public boolean canManageAlerts() {
        return this == PLATFORM_ADMIN || this == TENANT_ADMIN || this == PROPERTY_MANAGER || this == ELECTRICIAN;
    }

    public boolean canViewTenantData() {
        return this != DEVICE && this != SUPPORT_READONLY;
    }
}
