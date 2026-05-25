package com.pyrosense.shared.security;

import com.pyrosense.shared.id.DeviceId;
import com.pyrosense.shared.id.TenantId;
import com.pyrosense.shared.id.UserId;

import java.util.Collections;
import java.util.Objects;
import java.util.Set;

public record SecurityContext(
        UserId userId,
        TenantId tenantId,
        Set<PlatformRole> roles,
        Set<Permission> permissions,
        DeviceId deviceId,
        String scope
) {
    public SecurityContext {
        Objects.requireNonNull(roles);
        Objects.requireNonNull(permissions);
    }

    public boolean hasRole(PlatformRole role) {
        return roles.contains(role);
    }

    public boolean hasPermission(Permission permission) {
        return permissions.contains(permission) || roles.contains(PlatformRole.PLATFORM_ADMIN);
    }

    public boolean isDevice() {
        return deviceId != null && roles.contains(PlatformRole.DEVICE);
    }

    public boolean isPlatformAdmin() {
        return roles.contains(PlatformRole.PLATFORM_ADMIN);
    }

    public boolean belongsToTenant(TenantId other) {
        if (isPlatformAdmin()) return true;
        return tenantId != null && tenantId.equals(other);
    }

    public static SecurityContext system() {
        return new SecurityContext(null, null, Set.of(PlatformRole.PLATFORM_ADMIN),
                Set.of(Permission.SYSTEM_ADMIN), null, "system");
    }

    public static SecurityContext anonymous() {
        return new SecurityContext(null, null, Collections.emptySet(),
                Collections.emptySet(), null, "anonymous");
    }
}
