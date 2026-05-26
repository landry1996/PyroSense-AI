package com.pyrosense.dashboard.domain.model;

import java.time.LocalDate;

public record RiskTrendPoint(
        LocalDate date,
        double averageScore,
        double maxScore,
        int alertCount) {}
