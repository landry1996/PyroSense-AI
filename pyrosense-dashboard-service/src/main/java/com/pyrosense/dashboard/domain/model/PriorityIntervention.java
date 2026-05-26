package com.pyrosense.dashboard.domain.model;

import java.time.Instant;
import java.time.LocalDate;

public record PriorityIntervention(
        String interventionId,
        String type,
        String priority,
        String status,
        String buildingId,
        String buildingName,
        String assignedTo,
        LocalDate scheduledDate,
        Instant createdAt,
        boolean overdue) {}
