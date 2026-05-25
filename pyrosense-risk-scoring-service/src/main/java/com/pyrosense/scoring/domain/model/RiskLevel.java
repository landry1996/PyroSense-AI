package com.pyrosense.scoring.domain.model;

public enum RiskLevel {

    LOW(0, 29, "Risque faible — surveillance normale"),
    MODERATE(30, 59, "Risque modéré — vigilance accrue recommandée"),
    HIGH(60, 79, "Risque élevé — intervention planifiée nécessaire"),
    CRITICAL(80, 100, "Risque critique — intervention immédiate requise");

    private final int minScore;
    private final int maxScore;
    private final String description;

    RiskLevel(int minScore, int maxScore, String description) {
        this.minScore = minScore;
        this.maxScore = maxScore;
        this.description = description;
    }

    public int minScore() { return minScore; }
    public int maxScore() { return maxScore; }
    public String description() { return description; }

    public static RiskLevel fromScore(int score) {
        for (RiskLevel level : values()) {
            if (score >= level.minScore && score <= level.maxScore) return level;
        }
        throw new IllegalArgumentException("Score must be 0-100, got: " + score);
    }

    public boolean isEscalation(RiskLevel previous) {
        return this.ordinal() > previous.ordinal();
    }
}
