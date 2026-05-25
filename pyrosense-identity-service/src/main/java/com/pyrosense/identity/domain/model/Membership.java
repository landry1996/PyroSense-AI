package com.pyrosense.identity.domain.model;

import com.pyrosense.shared.id.TenantId;
import com.pyrosense.shared.id.UserId;
import com.pyrosense.shared.util.ClockProvider;

import java.time.Instant;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public class Membership {

    private final UUID id;
    private final UserId userId;
    private final TenantId tenantId;
    private final Set<Role> roles;
    private boolean active;
    private final Instant createdAt;

    public Membership(UUID id, UserId userId, TenantId tenantId, Set<Role> roles) {
        this.id = Objects.requireNonNull(id);
        this.userId = Objects.requireNonNull(userId);
        this.tenantId = Objects.requireNonNull(tenantId);
        this.roles = EnumSet.copyOf(Objects.requireNonNull(roles));
        this.active = true;
        this.createdAt = ClockProvider.now();
    }

    private Membership(UUID id, UserId userId, TenantId tenantId, Set<Role> roles,
                       boolean active, Instant createdAt) {
        this.id = Objects.requireNonNull(id);
        this.userId = Objects.requireNonNull(userId);
        this.tenantId = Objects.requireNonNull(tenantId);
        this.roles = EnumSet.copyOf(Objects.requireNonNull(roles));
        this.active = active;
        this.createdAt = createdAt;
    }

    public static Membership reconstitute(UUID id, UserId userId, TenantId tenantId,
                                           Set<Role> roles, boolean active, Instant createdAt) {
        return new Membership(id, userId, tenantId, roles, active, createdAt);
    }

    public void addRole(Role role) {
        this.roles.add(role);
    }

    public void removeRole(Role role) {
        this.roles.remove(role);
    }

    public void deactivate() {
        this.active = false;
    }

    public boolean hasRole(Role role) {
        return roles.contains(role);
    }

    public UUID getId() { return id; }
    public UserId getUserId() { return userId; }
    public TenantId getTenantId() { return tenantId; }
    public Set<Role> getRoles() { return roles; }
    public boolean isActive() { return active; }
    public Instant getCreatedAt() { return createdAt; }
}
