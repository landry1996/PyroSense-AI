package com.pyrosense.analysis.application.usecase;

import com.pyrosense.analysis.application.port.in.DetectSignalDriftUseCase;
import com.pyrosense.analysis.application.port.out.AnalysisEventPublisherPort;
import com.pyrosense.analysis.application.port.out.BaselineProfileRepositoryPort;
import com.pyrosense.analysis.domain.detection.ExponentialSmoothingDetector;
import com.pyrosense.analysis.domain.detection.ThdDriftDetector;
import com.pyrosense.analysis.domain.event.BaselineDriftDetectedEvent;
import com.pyrosense.analysis.domain.model.*;
import com.pyrosense.shared.util.ClockProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class DetectSignalDriftService implements DetectSignalDriftUseCase {

    private static final Logger log = LoggerFactory.getLogger(DetectSignalDriftService.class);

    private final BaselineProfileRepositoryPort baselineRepository;
    private final AnalysisEventPublisherPort eventPublisher;
    private final DetectionThresholds thresholds;

    public DetectSignalDriftService(BaselineProfileRepositoryPort baselineRepository,
                                    AnalysisEventPublisherPort eventPublisher,
                                    DetectionThresholds thresholds) {
        this.baselineRepository = baselineRepository;
        this.eventPublisher = eventPublisher;
        this.thresholds = thresholds;
    }

    @Override
    public List<SignalAnomaly> detectDrift(DetectDriftCommand command) {
        Instant now = ClockProvider.now();

        BaselineProfile baseline = baselineRepository
                .findByDeviceId(command.deviceId())
                .orElse(null);

        if (baseline == null || !baseline.isReady()) {
            return List.of();
        }

        List<SignalAnomaly> drifts = new ArrayList<>();
        drifts.addAll(ThdDriftDetector.detect(command.window(), baseline, thresholds, now));
        drifts.addAll(ExponentialSmoothingDetector.detect(command.window(), baseline, thresholds, now));

        for (SignalAnomaly drift : drifts) {
            if (drift.type() == AnomalyType.BASELINE_DRIFT) {
                StatisticalRange stats = baseline.statsFor(drift.feature());
                double driftPercent = stats.mean() > 0
                        ? ((drift.currentValue() - stats.mean()) / stats.mean()) * 100.0
                        : 0;

                eventPublisher.publish(new BaselineDriftDetectedEvent(
                        UUID.randomUUID(), now, command.deviceId(), drift.feature(),
                        stats.mean(), drift.currentValue(), driftPercent
                ));
                log.info("Drift detected: device={}, feature={}, drift={}%",
                        command.deviceId(), drift.feature(), String.format("%.1f", driftPercent));
            }
        }

        return drifts;
    }
}
