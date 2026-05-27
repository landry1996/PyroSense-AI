package com.pyrosense.device.domain.model;

import com.pyrosense.shared.id.DeviceId;
import com.pyrosense.shared.id.TenantId;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public class DeviceProvisioningSession {

    public enum SessionStatus { PENDING, COMPLETED, FAILED }

    private final UUID id;
    private final DeviceId deviceId;
    private final TenantId tenantId;
    private final String deviceSerial;
    private final String deviceModel;
    private final String firmwareVersion;
    private final String sourceIp;
    private SessionStatus status;
    private String failureReason;
    private final Instant createdAt;
    private Instant completedAt;

    private DeviceProvisioningSession(UUID id, DeviceId deviceId, TenantId tenantId,
                                       String deviceSerial, String deviceModel,
                                       String firmwareVersion, String sourceIp,
                                       Instant createdAt) {
        this.id = Objects.requireNonNull(id);
        this.deviceId = Objects.requireNonNull(deviceId);
        this.tenantId = Objects.requireNonNull(tenantId);
        this.deviceSerial = Objects.requireNonNull(deviceSerial);
        this.deviceModel = deviceModel;
        this.firmwareVersion = Objects.requireNonNull(firmwareVersion);
        this.sourceIp = sourceIp;
        this.status = SessionStatus.PENDING;
        this.createdAt = Objects.requireNonNull(createdAt);
    }

    public static DeviceProvisioningSession create(DeviceId deviceId, TenantId tenantId,
                                                    String deviceSerial, String deviceModel,
                                                    String firmwareVersion, String sourceIp) {
        return new DeviceProvisioningSession(UUID.randomUUID(), deviceId, tenantId,
                deviceSerial, deviceModel, firmwareVersion, sourceIp, Instant.now());
    }

    public static DeviceProvisioningSession reconstitute(UUID id, DeviceId deviceId, TenantId tenantId,
                                                          String deviceSerial, String deviceModel,
                                                          String firmwareVersion, String sourceIp,
                                                          SessionStatus status, String failureReason,
                                                          Instant createdAt, Instant completedAt) {
        var session = new DeviceProvisioningSession(id, deviceId, tenantId,
                deviceSerial, deviceModel, firmwareVersion, sourceIp, createdAt);
        session.status = status;
        session.failureReason = failureReason;
        session.completedAt = completedAt;
        return session;
    }

    public void markCompleted() {
        this.status = SessionStatus.COMPLETED;
        this.completedAt = Instant.now();
    }

    public void markFailed(String reason) {
        this.status = SessionStatus.FAILED;
        this.failureReason = reason;
        this.completedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public DeviceId getDeviceId() { return deviceId; }
    public TenantId getTenantId() { return tenantId; }
    public String getDeviceSerial() { return deviceSerial; }
    public String getDeviceModel() { return deviceModel; }
    public String getFirmwareVersion() { return firmwareVersion; }
    public String getSourceIp() { return sourceIp; }
    public SessionStatus getStatus() { return status; }
    public String getFailureReason() { return failureReason; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getCompletedAt() { return completedAt; }
}
