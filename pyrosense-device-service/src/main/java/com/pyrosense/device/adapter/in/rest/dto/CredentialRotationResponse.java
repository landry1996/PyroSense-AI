package com.pyrosense.device.adapter.in.rest.dto;

public record CredentialRotationResponse(
        String hmacKey,
        int version
) {}
