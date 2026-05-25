package com.pyrosense.shared.security;

import com.pyrosense.shared.id.TenantId;

import java.util.Optional;

public final class TenantContext {

    private static final ThreadLocal<TenantId> currentTenant = new ThreadLocal<>();

    private TenantContext() {}

    public static void set(TenantId tenantId) {
        currentTenant.set(tenantId);
    }

    public static Optional<TenantId> get() {
        return Optional.ofNullable(currentTenant.get());
    }

    public static TenantId require() {
        TenantId tenantId = currentTenant.get();
        if (tenantId == null) {
            throw new IllegalStateException("No tenant context set for current request");
        }
        return tenantId;
    }

    public static void clear() {
        currentTenant.remove();
    }
}
