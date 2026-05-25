package com.pyrosense.device.adapter.out.persistence.entity;

import com.pyrosense.device.domain.model.ConnectivityType;
import com.pyrosense.device.domain.model.DeviceStatus;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "devices")
public class DeviceJpaEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "serial_number", nullable = false, unique = true, length = 50)
    private String serialNumber;

    @Column(name = "tenant_id")
    private UUID tenantId;

    @Column(name = "building_id")
    private UUID buildingId;

    @Column(name = "panel_id")
    private UUID panelId;

    @Column(name = "firmware_version", nullable = false, length = 20)
    private String firmwareVersion;

    @Column(name = "hardware_revision", nullable = false, length = 20)
    private String hardwareRevision;

    @Enumerated(EnumType.STRING)
    @Column(name = "connectivity_type", nullable = false, length = 20)
    private ConnectivityType connectivityType;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private DeviceStatus status;

    @Column(name = "last_seen_at")
    private Instant lastSeenAt;

    @Column(name = "installation_date")
    private Instant installationDate;

    @Column(name = "enrollment_key_hash", length = 128)
    private String enrollmentKeyHash;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "created_by", nullable = false, updatable = false, length = 100)
    private String createdBy;

    @Column(name = "updated_by", nullable = false, length = 100)
    private String updatedBy;

    public DeviceJpaEntity() {}

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getSerialNumber() { return serialNumber; }
    public void setSerialNumber(String serialNumber) { this.serialNumber = serialNumber; }
    public UUID getTenantId() { return tenantId; }
    public void setTenantId(UUID tenantId) { this.tenantId = tenantId; }
    public UUID getBuildingId() { return buildingId; }
    public void setBuildingId(UUID buildingId) { this.buildingId = buildingId; }
    public UUID getPanelId() { return panelId; }
    public void setPanelId(UUID panelId) { this.panelId = panelId; }
    public String getFirmwareVersion() { return firmwareVersion; }
    public void setFirmwareVersion(String firmwareVersion) { this.firmwareVersion = firmwareVersion; }
    public String getHardwareRevision() { return hardwareRevision; }
    public void setHardwareRevision(String hardwareRevision) { this.hardwareRevision = hardwareRevision; }
    public ConnectivityType getConnectivityType() { return connectivityType; }
    public void setConnectivityType(ConnectivityType connectivityType) { this.connectivityType = connectivityType; }
    public DeviceStatus getStatus() { return status; }
    public void setStatus(DeviceStatus status) { this.status = status; }
    public Instant getLastSeenAt() { return lastSeenAt; }
    public void setLastSeenAt(Instant lastSeenAt) { this.lastSeenAt = lastSeenAt; }
    public Instant getInstallationDate() { return installationDate; }
    public void setInstallationDate(Instant installationDate) { this.installationDate = installationDate; }
    public String getEnrollmentKeyHash() { return enrollmentKeyHash; }
    public void setEnrollmentKeyHash(String enrollmentKeyHash) { this.enrollmentKeyHash = enrollmentKeyHash; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }
    public String getUpdatedBy() { return updatedBy; }
    public void setUpdatedBy(String updatedBy) { this.updatedBy = updatedBy; }
}
