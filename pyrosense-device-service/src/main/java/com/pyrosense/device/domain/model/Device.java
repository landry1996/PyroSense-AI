package com.pyrosense.device.domain.model;

import com.pyrosense.shared.audit.AuditMetadata;
import com.pyrosense.shared.domain.AggregateRoot;
import com.pyrosense.shared.exception.InvalidStateTransitionException;
import com.pyrosense.shared.id.BuildingId;
import com.pyrosense.shared.id.DeviceId;
import com.pyrosense.shared.id.ElectricalPanelId;
import com.pyrosense.shared.id.TenantId;
import com.pyrosense.shared.util.ClockProvider;
import com.pyrosense.device.domain.event.*;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public class Device extends AggregateRoot<DeviceId> {

    private final DeviceId id;
    private final String serialNumber;
    private TenantId tenantId;
    private BuildingId buildingId;
    private ElectricalPanelId panelId;
    private String firmwareVersion;
    private String hardwareRevision;
    private ConnectivityType connectivityType;
    private DeviceStatus status;
    private Instant lastSeenAt;
    private Instant installationDate;
    private String enrollmentKeyHash;
    private AuditMetadata audit;

    private Device(DeviceId id, String serialNumber, String firmwareVersion,
                   String hardwareRevision, ConnectivityType connectivityType, String actor) {
        this.id = Objects.requireNonNull(id);
        this.serialNumber = Objects.requireNonNull(serialNumber);
        this.firmwareVersion = Objects.requireNonNull(firmwareVersion);
        this.hardwareRevision = Objects.requireNonNull(hardwareRevision);
        this.connectivityType = Objects.requireNonNull(connectivityType);
        this.status = DeviceStatus.REGISTERED;
        this.audit = AuditMetadata.create(actor);
    }

    public static Device register(String serialNumber, String firmwareVersion,
                                   String hardwareRevision, ConnectivityType connectivityType,
                                   String enrollmentKeyHash, String actor) {
        var device = new Device(DeviceId.generate(), serialNumber, firmwareVersion,
                hardwareRevision, connectivityType, actor);
        device.enrollmentKeyHash = enrollmentKeyHash;
        device.registerEvent(new DeviceRegisteredEvent(
                UUID.randomUUID(), ClockProvider.now(), device.id, serialNumber));
        return device;
    }

    public void provision(TenantId tenantId, BuildingId buildingId,
                          ElectricalPanelId panelId, String actor) {
        assertState(DeviceStatus.REGISTERED, DeviceStatus.PROVISIONED);
        this.tenantId = Objects.requireNonNull(tenantId);
        this.buildingId = Objects.requireNonNull(buildingId);
        this.panelId = Objects.requireNonNull(panelId);
        this.installationDate = ClockProvider.now();
        this.status = DeviceStatus.PROVISIONED;
        this.audit = audit.updatedBy(actor);
        registerEvent(new DeviceProvisionedEvent(
                UUID.randomUUID(), ClockProvider.now(), id, tenantId, buildingId, panelId));
    }

    public void activate(String actor) {
        assertState(DeviceStatus.PROVISIONED, DeviceStatus.ACTIVE);
        this.status = DeviceStatus.ACTIVE;
        this.lastSeenAt = ClockProvider.now();
        this.audit = audit.updatedBy(actor);
        registerEvent(new DeviceActivatedEvent(
                UUID.randomUUID(), ClockProvider.now(), id));
    }

    public void markOffline() {
        if (status == DeviceStatus.REVOKED || status == DeviceStatus.REGISTERED) return;
        this.status = DeviceStatus.OFFLINE;
        registerEvent(new DeviceOfflineDetectedEvent(
                UUID.randomUUID(), ClockProvider.now(), id, lastSeenAt));
    }

    public void markMaintenance(String actor) {
        if (status == DeviceStatus.REVOKED) {
            throw new InvalidStateTransitionException("Device", status.name(), "MAINTENANCE");
        }
        this.status = DeviceStatus.MAINTENANCE;
        this.audit = audit.updatedBy(actor);
    }

    public void revoke(String reason, String actor) {
        if (status == DeviceStatus.REVOKED) return;
        this.status = DeviceStatus.REVOKED;
        this.audit = audit.updatedBy(actor);
        registerEvent(new DeviceRevokedEvent(
                UUID.randomUUID(), ClockProvider.now(), id, reason));
    }

    public void recordHeartbeat() {
        if (status == DeviceStatus.REVOKED) return;
        this.lastSeenAt = ClockProvider.now();
        if (status == DeviceStatus.OFFLINE) {
            this.status = DeviceStatus.ACTIVE;
        }
    }

    public boolean isOperational() {
        return status == DeviceStatus.ACTIVE || status == DeviceStatus.PROVISIONED;
    }

    public boolean isRevoked() {
        return status == DeviceStatus.REVOKED;
    }

    private void assertState(DeviceStatus expected, DeviceStatus target) {
        if (this.status != expected) {
            throw new InvalidStateTransitionException("Device", this.status.name(), target.name());
        }
    }

    // Reconstitution factory for persistence adapters
    public static Device reconstitute(DeviceId id, String serialNumber, TenantId tenantId,
                                       BuildingId buildingId, ElectricalPanelId panelId,
                                       String firmwareVersion, String hardwareRevision,
                                       ConnectivityType connectivityType, DeviceStatus status,
                                       Instant lastSeenAt, Instant installationDate,
                                       String enrollmentKeyHash, AuditMetadata audit) {
        var device = new Device(id, serialNumber, firmwareVersion, hardwareRevision,
                connectivityType, audit.createdBy());
        device.tenantId = tenantId;
        device.buildingId = buildingId;
        device.panelId = panelId;
        device.status = status;
        device.lastSeenAt = lastSeenAt;
        device.installationDate = installationDate;
        device.enrollmentKeyHash = enrollmentKeyHash;
        device.audit = audit;
        return device;
    }

    @Override
    public DeviceId getId() { return id; }
    public String getSerialNumber() { return serialNumber; }
    public TenantId getTenantId() { return tenantId; }
    public BuildingId getBuildingId() { return buildingId; }
    public ElectricalPanelId getPanelId() { return panelId; }
    public String getFirmwareVersion() { return firmwareVersion; }
    public String getHardwareRevision() { return hardwareRevision; }
    public ConnectivityType getConnectivityType() { return connectivityType; }
    public DeviceStatus getStatus() { return status; }
    public Instant getLastSeenAt() { return lastSeenAt; }
    public Instant getInstallationDate() { return installationDate; }
    public String getEnrollmentKeyHash() { return enrollmentKeyHash; }
    public AuditMetadata getAudit() { return audit; }
}
