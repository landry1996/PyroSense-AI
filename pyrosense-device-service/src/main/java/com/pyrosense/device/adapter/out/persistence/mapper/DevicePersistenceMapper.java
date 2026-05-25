package com.pyrosense.device.adapter.out.persistence.mapper;

import com.pyrosense.device.adapter.out.persistence.entity.DeviceJpaEntity;
import com.pyrosense.device.domain.model.Device;
import com.pyrosense.shared.audit.AuditMetadata;
import com.pyrosense.shared.id.BuildingId;
import com.pyrosense.shared.id.DeviceId;
import com.pyrosense.shared.id.ElectricalPanelId;
import com.pyrosense.shared.id.TenantId;

public final class DevicePersistenceMapper {

    private DevicePersistenceMapper() {}

    public static DeviceJpaEntity toEntity(Device device) {
        var entity = new DeviceJpaEntity();
        entity.setId(device.getId().value());
        entity.setSerialNumber(device.getSerialNumber());
        entity.setTenantId(device.getTenantId() != null ? device.getTenantId().value() : null);
        entity.setBuildingId(device.getBuildingId() != null ? device.getBuildingId().value() : null);
        entity.setPanelId(device.getPanelId() != null ? device.getPanelId().value() : null);
        entity.setFirmwareVersion(device.getFirmwareVersion());
        entity.setHardwareRevision(device.getHardwareRevision());
        entity.setConnectivityType(device.getConnectivityType());
        entity.setStatus(device.getStatus());
        entity.setLastSeenAt(device.getLastSeenAt());
        entity.setInstallationDate(device.getInstallationDate());
        entity.setEnrollmentKeyHash(device.getEnrollmentKeyHash());
        entity.setCreatedAt(device.getAudit().createdAt());
        entity.setUpdatedAt(device.getAudit().updatedAt());
        entity.setCreatedBy(device.getAudit().createdBy());
        entity.setUpdatedBy(device.getAudit().updatedBy());
        return entity;
    }

    public static Device toDomain(DeviceJpaEntity entity) {
        return Device.reconstitute(
                new DeviceId(entity.getId()),
                entity.getSerialNumber(),
                entity.getTenantId() != null ? new TenantId(entity.getTenantId()) : null,
                entity.getBuildingId() != null ? new BuildingId(entity.getBuildingId()) : null,
                entity.getPanelId() != null ? new ElectricalPanelId(entity.getPanelId()) : null,
                entity.getFirmwareVersion(),
                entity.getHardwareRevision(),
                entity.getConnectivityType(),
                entity.getStatus(),
                entity.getLastSeenAt(),
                entity.getInstallationDate(),
                entity.getEnrollmentKeyHash(),
                new AuditMetadata(
                        entity.getCreatedAt(),
                        entity.getUpdatedAt(),
                        entity.getCreatedBy(),
                        entity.getUpdatedBy()
                )
        );
    }
}
