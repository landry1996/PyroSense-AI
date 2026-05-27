package com.pyrosense.device.adapter.in.rest.dto;

public record ProvisioningResponse(
        String deviceId,
        String tenantId,
        String hmacKey,
        String mqttBrokerUri,
        int mqttPort,
        String topicPrefix
) {}
