package com.pyrosense.identity.domain.model;

import com.pyrosense.shared.id.TenantId;
import com.pyrosense.shared.util.ClockProvider;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public class EmergencyContact {
    private final UUID id;
    private final TenantId tenantId;
    private String name;
    private String phone;
    private String email;
    private String role;
    private int priority;
    private final Instant createdAt;
    private Instant updatedAt;

    public EmergencyContact(TenantId tenantId, String name, String phone, String email, String role, int priority) {
        this.id = UUID.randomUUID();
        this.tenantId = Objects.requireNonNull(tenantId);
        this.name = Objects.requireNonNull(name);
        this.phone = Objects.requireNonNull(phone);
        this.email = email;
        this.role = Objects.requireNonNull(role);
        this.priority = priority;
        this.createdAt = ClockProvider.now();
        this.updatedAt = this.createdAt;
    }

    private EmergencyContact(UUID id, TenantId tenantId, String name, String phone, String email,
                             String role, int priority, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.tenantId = tenantId;
        this.name = name;
        this.phone = phone;
        this.email = email;
        this.role = role;
        this.priority = priority;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static EmergencyContact reconstitute(UUID id, TenantId tenantId, String name, String phone,
                                                  String email, String role, int priority,
                                                  Instant createdAt, Instant updatedAt) {
        return new EmergencyContact(id, tenantId, name, phone, email, role, priority, createdAt, updatedAt);
    }

    public void update(String name, String phone, String email, String role, int priority) {
        this.name = Objects.requireNonNull(name);
        this.phone = Objects.requireNonNull(phone);
        this.email = email;
        this.role = Objects.requireNonNull(role);
        this.priority = priority;
        this.updatedAt = ClockProvider.now();
    }

    public UUID getId() { return id; }
    public TenantId getTenantId() { return tenantId; }
    public String getName() { return name; }
    public String getPhone() { return phone; }
    public String getEmail() { return email; }
    public String getRole() { return role; }
    public int getPriority() { return priority; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
