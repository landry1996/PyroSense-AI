package com.pyrosense.analysis.domain.model;

public enum AnomalyType {

    THD_ABNORMAL("THD anormalement élevé", 0.7),
    MICRO_ARC_RECURRENT("Micro-arcs récurrents détectés", 0.9),
    TEMPERATURE_RISING("Température en hausse progressive", 0.6),
    TRANSIENT_ABNORMAL("Transitoires anormaux", 0.5),
    HF_NOISE_ELEVATED("Bruit haute fréquence élevé", 0.6),
    POWER_FACTOR_DEGRADED("Facteur de puissance dégradé", 0.4),
    BASELINE_DRIFT("Dérive par rapport à la baseline", 0.5);

    private final String description;
    private final double baseWeight;

    AnomalyType(String description, double baseWeight) {
        this.description = description;
        this.baseWeight = baseWeight;
    }

    public String description() {
        return description;
    }

    public double baseWeight() {
        return baseWeight;
    }
}
