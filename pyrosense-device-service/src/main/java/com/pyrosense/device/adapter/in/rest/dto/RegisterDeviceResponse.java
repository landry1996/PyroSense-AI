package com.pyrosense.device.adapter.in.rest.dto;

import java.util.UUID;

public record RegisterDeviceResponse(
        UUID deviceId,
        String enrollmentKey
) {}
