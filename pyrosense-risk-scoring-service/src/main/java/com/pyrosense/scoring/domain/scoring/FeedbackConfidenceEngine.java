package com.pyrosense.scoring.domain.scoring;

import com.pyrosense.scoring.domain.model.FeedbackOutcome;
import com.pyrosense.scoring.domain.model.ScoringAdjustment;

import java.util.Optional;

public final class FeedbackConfidenceEngine {

    private static final double CONFIRMED_BOOST = 0.05;
    private static final double FALSE_POSITIVE_REDUCTION = 0.03;
    private static final double NO_DEFECT_SUSPICION_REDUCTION = 0.01;
    private static final double MAX_CUMULATIVE_BOOST = 0.20;
    private static final double MAX_CUMULATIVE_REDUCTION = 0.15;

    private FeedbackConfidenceEngine() {}

    public static Optional<ScoringAdjustment> computeAdjustment(
            FeedbackOutcome outcome,
            String anomalyType,
            double currentConfidence,
            int confirmedCount,
            int falsePositiveCount) {

        return switch (outcome) {
            case CONFIRMED_DEFECT -> {
                double boost = CONFIRMED_BOOST * decayFactor(confirmedCount);
                double cumulativeBoost = confirmedCount * CONFIRMED_BOOST;
                if (cumulativeBoost >= MAX_CUMULATIVE_BOOST || currentConfidence >= 0.99) {
                    yield Optional.empty();
                }
                yield Optional.of(ScoringAdjustment.confidenceBoost(
                        anomalyType, currentConfidence, boost,
                        "Defect confirmed by field technician (count: %d)".formatted(confirmedCount + 1)));
            }
            case FALSE_POSITIVE -> {
                double reduction = FALSE_POSITIVE_REDUCTION * decayFactor(falsePositiveCount);
                double cumulativeReduction = falsePositiveCount * FALSE_POSITIVE_REDUCTION;
                if (cumulativeReduction >= MAX_CUMULATIVE_REDUCTION || currentConfidence <= 0.1) {
                    yield Optional.empty();
                }
                yield Optional.of(ScoringAdjustment.confidenceReduction(
                        anomalyType, currentConfidence, reduction,
                        "False positive confirmed by field technician (count: %d)".formatted(falsePositiveCount + 1)));
            }
            case NO_DEFECT_FOUND -> {
                if (currentConfidence <= 0.2) {
                    yield Optional.empty();
                }
                yield Optional.of(ScoringAdjustment.confidenceReduction(
                        anomalyType, currentConfidence, NO_DEFECT_SUSPICION_REDUCTION,
                        "No defect found during inspection - suspected false positive"));
            }
            case INCONCLUSIVE -> Optional.empty();
        };
    }

    private static double decayFactor(int previousAdjustmentCount) {
        return Math.pow(0.8, previousAdjustmentCount);
    }
}
