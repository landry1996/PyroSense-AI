package com.pyrosense.dashboard.domain.model;

import java.time.Instant;

public record RecentAlert(
        String alertId,
        String title,
        String severity,
        String status,
        String type,
        String buildingId,
        String buildingName,
        String deviceId,
        Instant createdAt,
        boolean slaBreached) {}
