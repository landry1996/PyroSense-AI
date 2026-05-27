package com.pyrosense.scoring.application.usecase;

import com.pyrosense.scoring.application.port.in.ProcessFieldFeedbackUseCase;
import com.pyrosense.scoring.application.port.out.ScoringEventPublisherPort;
import com.pyrosense.scoring.application.port.out.ScoringFeedbackRepositoryPort;
import com.pyrosense.scoring.domain.model.ScoringAdjustment;
import com.pyrosense.scoring.domain.model.ScoringFeedback;
import com.pyrosense.scoring.domain.scoring.FeedbackConfidenceEngine;
import com.pyrosense.shared.id.DeviceId;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.UUID;

public class ProcessFieldFeedbackService implements ProcessFieldFeedbackUseCase {

    private static final Logger log = LoggerFactory.getLogger(ProcessFieldFeedbackService.class);

    private final ScoringFeedbackRepositoryPort feedbackRepository;
    private final ScoringEventPublisherPort eventPublisher;
    private final Counter confirmedCounter;
    private final Counter falsePositiveCounter;
    private final Counter inconclusiveCounter;
    private final Counter adjustmentsCounter;

    public ProcessFieldFeedbackService(ScoringFeedbackRepositoryPort feedbackRepository,
                                        ScoringEventPublisherPort eventPublisher,
                                        MeterRegistry registry) {
        this.feedbackRepository = feedbackRepository;
        this.eventPublisher = eventPublisher;
        this.confirmedCounter = Counter.builder("pyrosense.scoring.confirmed_defects_total").register(registry);
        this.falsePositiveCounter = Counter.builder("pyrosense.scoring.false_positives_total").register(registry);
        this.inconclusiveCounter = Counter.builder("pyrosense.scoring.inconclusive_feedback_total").register(registry);
        this.adjustmentsCounter = Counter.builder("pyrosense.scoring.scoring_adjustments_total").register(registry);
    }

    @Override
    public ScoringFeedback process(FieldFeedbackCommand command) {
        var feedback = new ScoringFeedback(
                UUID.randomUUID(),
                DeviceId.from(command.deviceId()),
                command.tenantId(),
                command.alertId(),
                command.interventionId(),
                command.outcome(),
                command.anomalyType(),
                command.riskScoreAtAlert(),
                command.feedbackSource(),
                command.comment(),
                Instant.now());

        feedbackRepository.saveFeedback(feedback);
        incrementOutcomeMetric(command);

        int confirmedCount = feedbackRepository.countByOutcome(
                command.deviceId(), command.anomalyType(), command.outcome());
        int falsePositiveCount = feedbackRepository.countByOutcome(
                command.deviceId(), command.anomalyType(),
                com.pyrosense.scoring.domain.model.FeedbackOutcome.FALSE_POSITIVE);

        var adjustmentOpt = FeedbackConfidenceEngine.computeAdjustment(
                command.outcome(),
                command.anomalyType(),
                computeCurrentConfidence(command.deviceId(), command.anomalyType()),
                confirmedCount - (command.outcome().improvesConfidence() ? 1 : 0),
                falsePositiveCount - (command.outcome().reducesConfidence() ? 1 : 0));

        adjustmentOpt.ifPresent(adjustment -> {
            feedback.applyAdjustment(adjustment);
            feedbackRepository.saveAdjustment(adjustment, command.deviceId(), command.tenantId());
            adjustmentsCounter.increment();

            log.info("Scoring adjustment suggested: device={} anomaly={} type={} delta={:.4f} mode={}",
                    command.deviceId(), command.anomalyType(),
                    adjustment.type(), adjustment.delta(), adjustment.mode());
        });

        log.info("Field feedback processed: device={} outcome={} anomaly={} alert={}",
                command.deviceId(), command.outcome(), command.anomalyType(), command.alertId());

        return feedback;
    }

    private double computeCurrentConfidence(String deviceId, String anomalyType) {
        var adjustments = feedbackRepository.findAdjustments(deviceId, anomalyType);
        double base = 0.5;
        for (var adj : adjustments) {
            base = adj.newValue();
        }
        return base;
    }

    private void incrementOutcomeMetric(FieldFeedbackCommand command) {
        switch (command.outcome()) {
            case CONFIRMED_DEFECT -> confirmedCounter.increment();
            case FALSE_POSITIVE -> falsePositiveCounter.increment();
            case INCONCLUSIVE, NO_DEFECT_FOUND -> inconclusiveCounter.increment();
        }
    }
}
