package com.pyrosense.scoring.domain.scoring;

import com.pyrosense.scoring.domain.model.*;
import com.pyrosense.shared.id.DeviceId;
import com.pyrosense.shared.id.ElectricalPanelId;

import java.time.Duration;
import java.time.Instant;
import java.util.*;

public final class RiskScoringEngine {

    private final ScoringWeights weights;

    public RiskScoringEngine(ScoringWeights weights) {
        this.weights = Objects.requireNonNull(weights);
    }

    public RiskAssessment computeScore(DeviceId deviceId, ElectricalPanelId panelId, UUID circuitId,
                                        List<AnomalyInput> anomalies, List<Integer> previousScores,
                                        boolean deviceOnline, boolean baselineAvailable, Instant now) {

        List<RiskFactor> factors = new ArrayList<>();

        double microArcScore = computeMicroArcFactor(anomalies, now);
        factors.add(RiskFactor.of("micro_arc", "Micro-arcs récurrents",
                weights.microArcWeight(), microArcScore));

        double thdScore = computeTypeFactor(anomalies, "THD_ABNORMAL", "BASELINE_DRIFT", now);
        factors.add(RiskFactor.of("thd_drift", "Dérive THD",
                weights.thdDriftWeight(), thdScore));

        double tempScore = computeTypeFactor(anomalies, "TEMPERATURE_RISING", null, now);
        factors.add(RiskFactor.of("temperature_trend", "Tendance température",
                weights.temperatureTrendWeight(), tempScore));

        double transientScore = computeTypeFactor(anomalies, "TRANSIENT_ABNORMAL", null, now);
        factors.add(RiskFactor.of("transient", "Transitoires anormaux",
                weights.transientWeight(), transientScore));

        double hfScore = computeTypeFactor(anomalies, "HF_NOISE_ELEVATED", null, now);
        factors.add(RiskFactor.of("hf_noise", "Bruit haute fréquence",
                weights.hfNoiseWeight(), hfScore));

        double reliabilityPenalty = deviceOnline ? 0.0 : 0.5;
        if (!baselineAvailable) reliabilityPenalty = Math.max(reliabilityPenalty, 0.3);
        factors.add(RiskFactor.of("device_reliability", "Fiabilité capteur",
                weights.deviceReliabilityWeight(), reliabilityPenalty));

        double rawScore = factors.stream().mapToDouble(RiskFactor::contribution).sum();
        RiskScore score = RiskScore.of(rawScore);
        RiskTrend trend = RiskTrend.compute(previousScores);
        Duration incidentWindow = estimateIncidentWindow(score, trend);
        String recommendation = generateRecommendation(score.level(), factors, trend);

        return new RiskAssessment(UUID.randomUUID(), deviceId, panelId, circuitId,
                score, trend, factors, incidentWindow, recommendation, now);
    }

    private double computeMicroArcFactor(List<AnomalyInput> anomalies, Instant now) {
        List<AnomalyInput> microArcs = anomalies.stream()
                .filter(a -> "MICRO_ARC_RECURRENT".equals(a.anomalyType()))
                .toList();
        if (microArcs.isEmpty()) return 0.0;

        double maxConfidence = microArcs.stream().mapToDouble(AnomalyInput::confidence).max().orElse(0);
        long count = microArcs.size();
        double repetitionBoost = Math.min(1.0 + (count - 1) * (weights.repetitionBoostFactor() - 1.0), 1.0);
        double recencyFactor = computeRecencyFactor(microArcs, now);

        return Math.min(maxConfidence * repetitionBoost * recencyFactor, 1.0);
    }

    private double computeTypeFactor(List<AnomalyInput> anomalies, String primaryType, String secondaryType, Instant now) {
        List<AnomalyInput> relevant = anomalies.stream()
                .filter(a -> a.anomalyType().equals(primaryType) ||
                        (secondaryType != null && a.anomalyType().equals(secondaryType)))
                .toList();
        if (relevant.isEmpty()) return 0.0;

        double maxConfidence = relevant.stream().mapToDouble(AnomalyInput::confidence).max().orElse(0);
        double recencyFactor = computeRecencyFactor(relevant, now);
        long count = relevant.size();
        double repetitionBoost = count > 1
                ? Math.min(1.0 + (count - 1) * 0.1, weights.repetitionBoostFactor())
                : 1.0;

        return Math.min(maxConfidence * recencyFactor * repetitionBoost, 1.0);
    }

    private double computeRecencyFactor(List<AnomalyInput> anomalies, Instant now) {
        Optional<Instant> mostRecent = anomalies.stream()
                .map(AnomalyInput::detectedAt)
                .max(Instant::compareTo);
        if (mostRecent.isEmpty()) return 0.0;

        long hoursAgo = Duration.between(mostRecent.get(), now).toHours();
        return Math.pow(weights.recencyDecayFactor(), hoursAgo / (double) weights.recencyWindowHours());
    }

    private Duration estimateIncidentWindow(RiskScore score, RiskTrend trend) {
        if (score.value() < 30) return Duration.ofDays(365);
        if (score.value() < 60) return Duration.ofDays(90);

        int baseDays = switch (score.level()) {
            case CRITICAL -> 7;
            case HIGH -> 30;
            default -> 90;
        };

        double trendMultiplier = switch (trend) {
            case CRITICAL -> 0.5;
            case DEGRADING -> 0.75;
            case STABLE -> 1.0;
            case IMPROVING -> 1.5;
        };

        return Duration.ofDays((long) (baseDays * trendMultiplier));
    }

    private String generateRecommendation(RiskLevel level, List<RiskFactor> factors, RiskTrend trend) {
        RiskFactor topFactor = factors.stream()
                .max(Comparator.comparingDouble(RiskFactor::contribution))
                .orElse(null);

        String factorDesc = topFactor != null ? topFactor.description() : "anomalies détectées";

        return switch (level) {
            case LOW -> "Aucune action requise. Surveillance standard.";
            case MODERATE -> "Vigilance accrue recommandée. Facteur principal : " + factorDesc + ".";
            case HIGH -> "Planifier une inspection. Facteur dominant : " + factorDesc +
                    (trend == RiskTrend.DEGRADING ? " (tendance dégradante)." : ".");
            case CRITICAL -> "INTERVENTION IMMÉDIATE. " + factorDesc +
                    ". Risque d'incident estimé sous 7 jours.";
        };
    }
}
