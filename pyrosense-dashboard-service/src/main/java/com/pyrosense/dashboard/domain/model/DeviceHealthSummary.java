package com.pyrosense.dashboard.domain.model;

public record DeviceHealthSummary(
        int totalDevices,
        int activeDevices,
        int offlineDevices,
        int provisionedDevices,
        int revokedDevices,
        double avgUptimePercent,
        int devicesWithHighRisk,
        int devicesSilentOver24h) {}
