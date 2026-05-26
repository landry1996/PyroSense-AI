package com.pyrosense.identity.domain.model;

import com.pyrosense.shared.id.TenantId;
import java.time.Instant;
import java.util.Map;

public class TenantSettings {
    private final TenantId tenantId;
    private Map<String, Object> settings;
    private Instant updatedAt;

    public TenantSettings(TenantId tenantId, Map<String, Object> settings, Instant updatedAt) {
        this.tenantId = tenantId;
        this.settings = settings;
        this.updatedAt = updatedAt;
    }

    public TenantId getTenantId() { return tenantId; }
    public Map<String, Object> getSettings() { return settings; }
    public Instant getUpdatedAt() { return updatedAt; }

    public void update(Map<String, Object> newSettings) {
        this.settings = newSettings;
        this.updatedAt = Instant.now();
    }
}
