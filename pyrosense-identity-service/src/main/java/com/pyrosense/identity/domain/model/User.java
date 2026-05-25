package com.pyrosense.identity.domain.model;

import com.pyrosense.shared.id.TenantId;
import com.pyrosense.shared.id.UserId;
import com.pyrosense.shared.util.ClockProvider;

import java.time.Instant;
import java.util.*;

public class User {

    private final UserId id;
    private String email;
    private String fullName;
    private UserStatus status;
    private final List<Membership> memberships;
    private int failedLoginAttempts;
    private Instant lastLoginAt;
    private Instant lockedUntil;
    private final Instant createdAt;
    private Instant updatedAt;

    public User(UserId id, String email, String fullName) {
        this.id = Objects.requireNonNull(id);
        this.email = Objects.requireNonNull(email);
        this.fullName = Objects.requireNonNull(fullName);
        this.status = UserStatus.ACTIVE;
        this.memberships = new ArrayList<>();
        this.failedLoginAttempts = 0;
        this.createdAt = ClockProvider.now();
        this.updatedAt = this.createdAt;
    }

    private User(UserId id, String email, String fullName, UserStatus status,
                 List<Membership> memberships, int failedLoginAttempts,
                 Instant lastLoginAt, Instant lockedUntil,
                 Instant createdAt, Instant updatedAt) {
        this.id = Objects.requireNonNull(id);
        this.email = email;
        this.fullName = fullName;
        this.status = status;
        this.memberships = new ArrayList<>(memberships);
        this.failedLoginAttempts = failedLoginAttempts;
        this.lastLoginAt = lastLoginAt;
        this.lockedUntil = lockedUntil;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static User reconstitute(UserId id, String email, String fullName, UserStatus status,
                                     List<Membership> memberships, int failedLoginAttempts,
                                     Instant lastLoginAt, Instant lockedUntil,
                                     Instant createdAt, Instant updatedAt) {
        return new User(id, email, fullName, status, memberships, failedLoginAttempts,
                lastLoginAt, lockedUntil, createdAt, updatedAt);
    }

    public void addMembership(TenantId tenantId, Set<Role> roles) {
        Membership membership = new Membership(UUID.randomUUID(), id, tenantId, roles);
        memberships.add(membership);
        this.updatedAt = ClockProvider.now();
    }

    public Optional<Membership> getMembershipForTenant(TenantId tenantId) {
        return memberships.stream()
                .filter(m -> m.getTenantId().equals(tenantId) && m.isActive())
                .findFirst();
    }

    public Set<Role> getRolesForTenant(TenantId tenantId) {
        return getMembershipForTenant(tenantId)
                .map(Membership::getRoles)
                .orElse(Collections.emptySet());
    }

    public void suspend() {
        this.status = UserStatus.SUSPENDED;
        this.updatedAt = ClockProvider.now();
    }

    public void activate() {
        this.status = UserStatus.ACTIVE;
        this.failedLoginAttempts = 0;
        this.lockedUntil = null;
        this.updatedAt = ClockProvider.now();
    }

    public void deactivate() {
        this.status = UserStatus.INACTIVE;
        this.updatedAt = ClockProvider.now();
    }

    public void recordLoginSuccess() {
        this.failedLoginAttempts = 0;
        this.lastLoginAt = ClockProvider.now();
    }

    public void recordLoginFailure(int maxAttempts) {
        this.failedLoginAttempts++;
        if (this.failedLoginAttempts >= maxAttempts) {
            this.status = UserStatus.LOCKED;
            this.lockedUntil = ClockProvider.now().plusSeconds(1800); // 30 min lockout
        }
    }

    public boolean isLocked() {
        if (status != UserStatus.LOCKED) return false;
        if (lockedUntil != null && ClockProvider.now().isAfter(lockedUntil)) {
            this.status = UserStatus.ACTIVE;
            this.failedLoginAttempts = 0;
            this.lockedUntil = null;
            return false;
        }
        return true;
    }

    public UserId getId() { return id; }
    public String getEmail() { return email; }
    public String getFullName() { return fullName; }
    public UserStatus getStatus() { return status; }
    public List<Membership> getMemberships() { return Collections.unmodifiableList(memberships); }
    public int getFailedLoginAttempts() { return failedLoginAttempts; }
    public Instant getLastLoginAt() { return lastLoginAt; }
    public Instant getLockedUntil() { return lockedUntil; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
