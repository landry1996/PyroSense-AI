package com.pyrosense.scoring.domain.scoring;

import com.pyrosense.scoring.domain.model.FeedbackOutcome;
import com.pyrosense.scoring.domain.model.ScoringAdjustment;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

class FeedbackConfidenceEngineTest {

    @Nested
    class ConfirmedDefect {

        @Test
        void shouldBoostConfidenceOnFirstConfirmation() {
            Optional<ScoringAdjustment> result = FeedbackConfidenceEngine.computeAdjustment(
                    FeedbackOutcome.CONFIRMED_DEFECT, "MICRO_ARC", 0.5, 0, 0);

            assertThat(result).isPresent();
            assertThat(result.get().type()).isEqualTo(ScoringAdjustment.AdjustmentType.CONFIDENCE_BOOST);
            assertThat(result.get().delta()).isCloseTo(0.05, within(0.001));
            assertThat(result.get().newValue()).isCloseTo(0.55, within(0.001));
            assertThat(result.get().mode()).isEqualTo(ScoringAdjustment.AdjustmentMode.SUGGESTION_ONLY);
        }

        @Test
        void shouldApplyDecayOnSubsequentConfirmations() {
            Optional<ScoringAdjustment> result = FeedbackConfidenceEngine.computeAdjustment(
                    FeedbackOutcome.CONFIRMED_DEFECT, "MICRO_ARC", 0.55, 1, 0);

            assertThat(result).isPresent();
            assertThat(result.get().delta()).isCloseTo(0.05 * 0.8, within(0.001));
        }

        @Test
        void shouldStopBoostingWhenMaxCumulativeReached() {
            Optional<ScoringAdjustment> result = FeedbackConfidenceEngine.computeAdjustment(
                    FeedbackOutcome.CONFIRMED_DEFECT, "MICRO_ARC", 0.7, 4, 0);

            assertThat(result).isEmpty();
        }

        @Test
        void shouldStopBoostingWhenConfidenceNearMax() {
            Optional<ScoringAdjustment> result = FeedbackConfidenceEngine.computeAdjustment(
                    FeedbackOutcome.CONFIRMED_DEFECT, "MICRO_ARC", 0.99, 0, 0);

            assertThat(result).isEmpty();
        }
    }

    @Nested
    class FalsePositive {

        @Test
        void shouldReduceConfidenceOnFirstFalsePositive() {
            Optional<ScoringAdjustment> result = FeedbackConfidenceEngine.computeAdjustment(
                    FeedbackOutcome.FALSE_POSITIVE, "THD_DRIFT", 0.6, 0, 0);

            assertThat(result).isPresent();
            assertThat(result.get().type()).isEqualTo(ScoringAdjustment.AdjustmentType.CONFIDENCE_REDUCTION);
            assertThat(result.get().delta()).isCloseTo(-0.03, within(0.001));
            assertThat(result.get().newValue()).isCloseTo(0.57, within(0.001));
        }

        @Test
        void shouldApplyDecayOnSubsequentFalsePositives() {
            Optional<ScoringAdjustment> result = FeedbackConfidenceEngine.computeAdjustment(
                    FeedbackOutcome.FALSE_POSITIVE, "THD_DRIFT", 0.57, 0, 1);

            assertThat(result).isPresent();
            assertThat(result.get().delta()).isCloseTo(-0.03 * 0.8, within(0.001));
        }

        @Test
        void shouldStopReducingWhenMaxCumulativeReached() {
            Optional<ScoringAdjustment> result = FeedbackConfidenceEngine.computeAdjustment(
                    FeedbackOutcome.FALSE_POSITIVE, "THD_DRIFT", 0.4, 0, 5);

            assertThat(result).isEmpty();
        }

        @Test
        void shouldStopReducingWhenConfidenceTooLow() {
            Optional<ScoringAdjustment> result = FeedbackConfidenceEngine.computeAdjustment(
                    FeedbackOutcome.FALSE_POSITIVE, "THD_DRIFT", 0.1, 0, 0);

            assertThat(result).isEmpty();
        }
    }

    @Nested
    class NoDefectFound {

        @Test
        void shouldApplySmallSuspicionReduction() {
            Optional<ScoringAdjustment> result = FeedbackConfidenceEngine.computeAdjustment(
                    FeedbackOutcome.NO_DEFECT_FOUND, "TEMPERATURE", 0.5, 0, 0);

            assertThat(result).isPresent();
            assertThat(result.get().delta()).isCloseTo(-0.01, within(0.001));
        }

        @Test
        void shouldNotReduceWhenConfidenceAlreadyLow() {
            Optional<ScoringAdjustment> result = FeedbackConfidenceEngine.computeAdjustment(
                    FeedbackOutcome.NO_DEFECT_FOUND, "TEMPERATURE", 0.2, 0, 0);

            assertThat(result).isEmpty();
        }
    }

    @Nested
    class Inconclusive {

        @Test
        void shouldReturnEmptyForInconclusive() {
            Optional<ScoringAdjustment> result = FeedbackConfidenceEngine.computeAdjustment(
                    FeedbackOutcome.INCONCLUSIVE, "MICRO_ARC", 0.5, 0, 0);

            assertThat(result).isEmpty();
        }
    }

    @Nested
    class AdjustmentProperties {

        @Test
        void shouldAlwaysUseSuggestionOnlyMode() {
            Optional<ScoringAdjustment> boost = FeedbackConfidenceEngine.computeAdjustment(
                    FeedbackOutcome.CONFIRMED_DEFECT, "MICRO_ARC", 0.5, 0, 0);
            Optional<ScoringAdjustment> reduction = FeedbackConfidenceEngine.computeAdjustment(
                    FeedbackOutcome.FALSE_POSITIVE, "MICRO_ARC", 0.5, 0, 0);

            assertThat(boost.get().isSuggestionOnly()).isTrue();
            assertThat(reduction.get().isSuggestionOnly()).isTrue();
        }

        @Test
        void shouldIncludeAnomalyTypeInAdjustment() {
            Optional<ScoringAdjustment> result = FeedbackConfidenceEngine.computeAdjustment(
                    FeedbackOutcome.CONFIRMED_DEFECT, "HF_NOISE", 0.5, 0, 0);

            assertThat(result.get().anomalyType()).isEqualTo("HF_NOISE");
        }

        @Test
        void shouldNeverExceedBounds() {
            Optional<ScoringAdjustment> boost = FeedbackConfidenceEngine.computeAdjustment(
                    FeedbackOutcome.CONFIRMED_DEFECT, "MICRO_ARC", 0.98, 0, 0);
            Optional<ScoringAdjustment> reduction = FeedbackConfidenceEngine.computeAdjustment(
                    FeedbackOutcome.FALSE_POSITIVE, "MICRO_ARC", 0.02, 0, 0);

            if (boost.isPresent()) {
                assertThat(boost.get().newValue()).isLessThanOrEqualTo(1.0);
            }
            if (reduction.isPresent()) {
                assertThat(reduction.get().newValue()).isGreaterThanOrEqualTo(0.0);
            }
        }
    }
}
