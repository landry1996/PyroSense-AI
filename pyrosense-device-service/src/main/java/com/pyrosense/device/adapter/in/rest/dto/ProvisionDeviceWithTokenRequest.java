package com.pyrosense.device.adapter.in.rest.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ProvisionDeviceWithTokenRequest(
        @NotBlank @Size(max = 64) String claimToken,
        @NotBlank @Size(max = 50) String deviceSerial,
        @Size(max = 50) String deviceModel,
        @NotBlank @Size(max = 20) String firmwareVersion
) {}
