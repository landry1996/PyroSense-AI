package com.pyrosense.device.adapter.in.rest.dto;

import java.time.Instant;

public record ClaimTokenResponse(
        String token,
        Instant expiresAt
) {}
