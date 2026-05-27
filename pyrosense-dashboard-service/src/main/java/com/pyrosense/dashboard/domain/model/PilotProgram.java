package com.pyrosense.dashboard.domain.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class PilotProgram {

    private final UUID id;
    private final String tenantId;
    private String name;
    private String description;
    private PilotStatus status;
    private final Instant createdAt;
    private Instant startedAt;
    private Instant completedAt;
    private final List<PilotDevice> devices;
    private final List<PilotSite> sites;

    public PilotProgram(UUID id, String tenantId, String name, String description) {
        this.id = id;
        this.tenantId = tenantId;
        this.name = name;
        this.description = description;
        this.status = PilotStatus.PREPARING;
        this.createdAt = Instant.now();
        this.devices = new ArrayList<>();
        this.sites = new ArrayList<>();
    }

    public PilotProgram(UUID id, String tenantId, String name, String description,
                        PilotStatus status, Instant createdAt, Instant startedAt, Instant completedAt) {
        this.id = id;
        this.tenantId = tenantId;
        this.name = name;
        this.description = description;
        this.status = status;
        this.createdAt = createdAt;
        this.startedAt = startedAt;
        this.completedAt = completedAt;
        this.devices = new ArrayList<>();
        this.sites = new ArrayList<>();
    }

    public void activate() {
        if (status != PilotStatus.PREPARING && status != PilotStatus.PAUSED) {
            throw new IllegalStateException("Cannot activate pilot in status " + status);
        }
        this.status = PilotStatus.ACTIVE;
        if (this.startedAt == null) {
            this.startedAt = Instant.now();
        }
    }

    public void pause() {
        if (status != PilotStatus.ACTIVE) {
            throw new IllegalStateException("Cannot pause pilot in status " + status);
        }
        this.status = PilotStatus.PAUSED;
    }

    public void complete() {
        if (status != PilotStatus.ACTIVE && status != PilotStatus.PAUSED) {
            throw new IllegalStateException("Cannot complete pilot in status " + status);
        }
        this.status = PilotStatus.COMPLETED;
        this.completedAt = Instant.now();
    }

    public void cancel() {
        if (status == PilotStatus.COMPLETED || status == PilotStatus.CANCELLED) {
            throw new IllegalStateException("Cannot cancel pilot in status " + status);
        }
        this.status = PilotStatus.CANCELLED;
        this.completedAt = Instant.now();
    }

    public void addDevice(PilotDevice device) {
        this.devices.add(device);
    }

    public void addSite(PilotSite site) {
        this.sites.add(site);
    }

    public UUID getId() { return id; }
    public String getTenantId() { return tenantId; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public PilotStatus getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getStartedAt() { return startedAt; }
    public Instant getCompletedAt() { return completedAt; }
    public List<PilotDevice> getDevices() { return devices; }
    public List<PilotSite> getSites() { return sites; }
    public int getDeviceCount() { return devices.size(); }
}
