package com.pyrosense.identity.domain.model;

import com.pyrosense.shared.id.TenantId;
import com.pyrosense.shared.util.ClockProvider;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public class ServiceAccount {

    private final UUID id;
    private final String name;
    private final TenantId tenantId;
    private final String clientId;
    private String clientSecretHash;
    private boolean active;
    private final Instant createdAt;
    private Instant lastUsedAt;

    public ServiceAccount(UUID id, String name, TenantId tenantId, String clientId, String clientSecretHash) {
        this.id = Objects.requireNonNull(id);
        this.name = Objects.requireNonNull(name);
        this.tenantId = Objects.requireNonNull(tenantId);
        this.clientId = Objects.requireNonNull(clientId);
        this.clientSecretHash = Objects.requireNonNull(clientSecretHash);
        this.active = true;
        this.createdAt = ClockProvider.now();
    }

    public void recordUsage() {
        this.lastUsedAt = ClockProvider.now();
    }

    public void rotateSecret(String newSecretHash) {
        this.clientSecretHash = Objects.requireNonNull(newSecretHash);
    }

    public void deactivate() {
        this.active = false;
    }

    public UUID getId() { return id; }
    public String getName() { return name; }
    public TenantId getTenantId() { return tenantId; }
    public String getClientId() { return clientId; }
    public String getClientSecretHash() { return clientSecretHash; }
    public boolean isActive() { return active; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getLastUsedAt() { return lastUsedAt; }
}
