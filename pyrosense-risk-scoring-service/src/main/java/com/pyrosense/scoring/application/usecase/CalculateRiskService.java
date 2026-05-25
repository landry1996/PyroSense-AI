package com.pyrosense.scoring.application.usecase;

import com.pyrosense.scoring.application.port.in.CalculateRiskUseCase;
import com.pyrosense.scoring.application.port.out.RiskAssessmentRepositoryPort;
import com.pyrosense.scoring.application.port.out.RiskModelPort;
import com.pyrosense.scoring.application.port.out.ScoringEventPublisherPort;
import com.pyrosense.scoring.domain.event.CriticalRiskDetectedEvent;
import com.pyrosense.scoring.domain.event.RiskLevelChangedEvent;
import com.pyrosense.scoring.domain.event.RiskScoreUpdatedEvent;
import com.pyrosense.scoring.domain.model.*;
import com.pyrosense.scoring.domain.scoring.RiskScoringEngine;
import com.pyrosense.shared.util.ClockProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public class CalculateRiskService implements CalculateRiskUseCase {

    private static final Logger log = LoggerFactory.getLogger(CalculateRiskService.class);

    private final RiskAssessmentRepositoryPort repository;
    private final ScoringEventPublisherPort eventPublisher;
    private final RiskModelPort riskModel;
    private final RiskScoringEngine engine;

    public CalculateRiskService(RiskAssessmentRepositoryPort repository,
                                 ScoringEventPublisherPort eventPublisher,
                                 RiskModelPort riskModel,
                                 RiskScoringEngine engine) {
        this.repository = repository;
        this.eventPublisher = eventPublisher;
        this.riskModel = riskModel;
        this.engine = engine;
    }

    @Override
    public RiskAssessment calculate(CalculateRiskCommand command) {
        Instant now = ClockProvider.now();
        List<Integer> previousScores = repository.findRecentScores(command.deviceId(), 10);

        RiskAssessment assessment = engine.computeScore(
                command.deviceId(), command.panelId(), command.circuitId(),
                command.anomalies(), previousScores,
                command.deviceOnline(), command.baselineAvailable(), now);

        tryMlAugmentation(command, assessment);

        repository.save(assessment);

        publishEvents(assessment, previousScores);

        log.info("Risk scored: device={}, score={}, level={}",
                command.deviceId(), assessment.score().value(), assessment.level());

        return assessment;
    }

    private void tryMlAugmentation(CalculateRiskCommand command, RiskAssessment assessment) {
        if (!riskModel.isAvailable()) return;
        try {
            riskModel.predict(command.deviceId(), command.anomalies())
                    .ifPresent(mlScore -> log.debug("ML prediction: {} (engine: {})",
                            mlScore.value(), assessment.score().value()));
        } catch (Exception e) {
            log.warn("ML model failed: {}", e.getMessage());
        }
    }

    private void publishEvents(RiskAssessment assessment, List<Integer> previousScores) {
        Instant now = assessment.computedAt();

        eventPublisher.publish(new RiskScoreUpdatedEvent(
                UUID.randomUUID(), now, assessment.deviceId(),
                assessment.score().value(), assessment.level(), assessment.trend()));

        if (assessment.isCritical()) {
            eventPublisher.publish(new CriticalRiskDetectedEvent(
                    UUID.randomUUID(), now, assessment.deviceId(), assessment.panelId(),
                    assessment.score().value(), assessment.predictedIncidentWindow(),
                    assessment.factors().stream().limit(3).toList(),
                    assessment.recommendation()));
        }

        if (!previousScores.isEmpty()) {
            RiskLevel previousLevel = RiskLevel.fromScore(previousScores.getLast());
            if (assessment.isLevelChange(previousLevel)) {
                eventPublisher.publish(new RiskLevelChangedEvent(
                        UUID.randomUUID(), now, assessment.deviceId(),
                        previousLevel, assessment.level(), assessment.score().value()));
            }
        }
    }
}
