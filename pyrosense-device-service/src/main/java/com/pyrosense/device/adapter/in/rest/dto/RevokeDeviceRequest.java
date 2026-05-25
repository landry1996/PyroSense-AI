package com.pyrosense.device.adapter.in.rest.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RevokeDeviceRequest(
        @NotBlank @Size(max = 500) String reason
) {}
