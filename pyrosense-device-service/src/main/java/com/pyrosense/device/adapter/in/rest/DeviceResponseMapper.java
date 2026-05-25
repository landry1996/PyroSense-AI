package com.pyrosense.device.adapter.in.rest;

import com.pyrosense.device.adapter.in.rest.dto.DevicePageResponse;
import com.pyrosense.device.adapter.in.rest.dto.DeviceResponse;
import com.pyrosense.device.domain.model.Device;
import com.pyrosense.shared.pagination.Page;

import java.util.List;

public final class DeviceResponseMapper {

    private DeviceResponseMapper() {}

    public static DeviceResponse toResponse(Device device) {
        return new DeviceResponse(
                device.getId().value(),
                device.getSerialNumber(),
                device.getTenantId() != null ? device.getTenantId().value() : null,
                device.getBuildingId() != null ? device.getBuildingId().value() : null,
                device.getPanelId() != null ? device.getPanelId().value() : null,
                device.getFirmwareVersion(),
                device.getHardwareRevision(),
                device.getConnectivityType(),
                device.getStatus(),
                device.getLastSeenAt(),
                device.getInstallationDate(),
                device.getAudit().createdAt(),
                device.getAudit().createdBy()
        );
    }

    public static DevicePageResponse toPageResponse(Page<Device> page) {
        List<DeviceResponse> content = page.content().stream()
                .map(DeviceResponseMapper::toResponse)
                .toList();
        return new DevicePageResponse(
                content,
                page.pageNumber(),
                page.pageSize(),
                page.totalElements(),
                page.totalPages(),
                page.hasNext()
        );
    }
}
