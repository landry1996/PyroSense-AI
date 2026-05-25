package com.pyrosense.device.adapter.in.rest.dto;

import com.pyrosense.device.domain.model.ConnectivityType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record RegisterDeviceRequest(
        @NotBlank @Size(min = 5, max = 50) String serialNumber,
        @NotBlank @Size(max = 20) String firmwareVersion,
        @NotBlank @Size(max = 20) String hardwareRevision,
        @NotNull ConnectivityType connectivityType
) {}
