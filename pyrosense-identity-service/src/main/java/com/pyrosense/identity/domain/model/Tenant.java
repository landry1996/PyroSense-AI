package com.pyrosense.identity.domain.model;

import com.pyrosense.shared.id.TenantId;
import com.pyrosense.shared.util.ClockProvider;

import java.time.Instant;
import java.util.Objects;

public class Tenant {

    private final TenantId id;
    private String name;
    private String slug;
    private boolean active;
    private final Instant createdAt;
    private Instant updatedAt;

    public Tenant(TenantId id, String name, String slug) {
        this.id = Objects.requireNonNull(id);
        this.name = Objects.requireNonNull(name);
        this.slug = Objects.requireNonNull(slug);
        this.active = true;
        this.createdAt = ClockProvider.now();
        this.updatedAt = this.createdAt;
    }

    private Tenant(TenantId id, String name, String slug, boolean active,
                   Instant createdAt, Instant updatedAt) {
        this.id = Objects.requireNonNull(id);
        this.name = name;
        this.slug = slug;
        this.active = active;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static Tenant reconstitute(TenantId id, String name, String slug, boolean active,
                                       Instant createdAt, Instant updatedAt) {
        return new Tenant(id, name, slug, active, createdAt, updatedAt);
    }

    public void deactivate() {
        this.active = false;
        this.updatedAt = ClockProvider.now();
    }

    public void rename(String name) {
        this.name = Objects.requireNonNull(name);
        this.updatedAt = ClockProvider.now();
    }

    public TenantId getId() { return id; }
    public String getName() { return name; }
    public String getSlug() { return slug; }
    public boolean isActive() { return active; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
