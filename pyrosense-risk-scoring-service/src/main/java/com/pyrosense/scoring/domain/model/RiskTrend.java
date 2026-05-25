package com.pyrosense.scoring.domain.model;

import java.util.List;

public enum RiskTrend {

    IMPROVING("Score en baisse"),
    STABLE("Score stable"),
    DEGRADING("Score en hausse"),
    CRITICAL("Dégradation rapide");

    private final String description;

    RiskTrend(String description) {
        this.description = description;
    }

    public String description() { return description; }

    public static RiskTrend compute(List<Integer> recentScores) {
        if (recentScores == null || recentScores.size() < 2) return STABLE;

        int last = recentScores.getLast();
        int previous = recentScores.get(recentScores.size() - 2);
        int delta = last - previous;

        if (recentScores.size() >= 3) {
            int secondPrev = recentScores.get(recentScores.size() - 3);
            int prevDelta = previous - secondPrev;
            if (delta > 10 && prevDelta > 10) return CRITICAL;
        }

        if (delta > 5) return DEGRADING;
        if (delta < -5) return IMPROVING;
        return STABLE;
    }
}
