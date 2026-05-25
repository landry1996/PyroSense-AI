package com.pyrosense.analysis.application.usecase;

import com.pyrosense.analysis.application.port.in.AnalyzeSignalUseCase;
import com.pyrosense.analysis.application.port.out.*;
import com.pyrosense.analysis.domain.detection.SignalAnalysisEngine;
import com.pyrosense.analysis.domain.event.SignalAnomalyDetectedEvent;
import com.pyrosense.analysis.domain.model.*;
import com.pyrosense.shared.util.ClockProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class AnalyzeSignalService implements AnalyzeSignalUseCase {

    private static final Logger log = LoggerFactory.getLogger(AnalyzeSignalService.class);

    private final BaselineProfileRepositoryPort baselineRepository;
    private final AnomalyRepositoryPort anomalyRepository;
    private final AnalysisEventPublisherPort eventPublisher;
    private final MachineLearningInferencePort mlPort;
    private final SignalAnalysisEngine engine;

    public AnalyzeSignalService(BaselineProfileRepositoryPort baselineRepository,
                                 AnomalyRepositoryPort anomalyRepository,
                                 AnalysisEventPublisherPort eventPublisher,
                                 MachineLearningInferencePort mlPort,
                                 SignalAnalysisEngine engine) {
        this.baselineRepository = baselineRepository;
        this.anomalyRepository = anomalyRepository;
        this.eventPublisher = eventPublisher;
        this.mlPort = mlPort;
        this.engine = engine;
    }

    @Override
    public AnalysisResult analyze(AnalyzeTelemetryWindowCommand command) {
        SignalWindow window = command.window();
        Instant now = ClockProvider.now();

        BaselineProfile baseline = baselineRepository
                .findByDeviceId(window.deviceId())
                .orElse(null);

        AnalysisResult result = engine.analyze(window, baseline, now);

        List<SignalAnomaly> mlAnomalies = tryMlInference(window);
        if (!mlAnomalies.isEmpty()) {
            List<SignalAnomaly> merged = new ArrayList<>(result.anomalies());
            merged.addAll(mlAnomalies);
            double riskScore = Math.min(result.aggregateRiskScore() + 10.0, 100.0);
            result = new AnalysisResult(result.id(), result.deviceId(), result.analyzedAt(),
                    merged, riskScore, result.baselineAvailable());
        }

        anomalyRepository.saveResult(result);

        if (result.hasAnomalies()) {
            eventPublisher.publish(new SignalAnomalyDetectedEvent(
                    UUID.randomUUID(), now, window.deviceId(), window.tenantId(),
                    result.anomalies(), result.aggregateRiskScore()
            ));
            log.info("Anomalies detected: device={}, count={}, risk={}",
                    window.deviceId(), result.anomalyCount(), result.aggregateRiskScore());
        }

        return result;
    }

    private List<SignalAnomaly> tryMlInference(SignalWindow window) {
        if (!mlPort.isAvailable()) return List.of();
        try {
            return mlPort.infer(window).orElse(List.of());
        } catch (Exception e) {
            log.warn("ML inference failed, continuing with rule-based analysis: {}", e.getMessage());
            return List.of();
        }
    }
}
