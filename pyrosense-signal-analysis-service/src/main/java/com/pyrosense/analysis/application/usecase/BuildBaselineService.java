package com.pyrosense.analysis.application.usecase;

import com.pyrosense.analysis.application.port.in.BuildBaselineUseCase;
import com.pyrosense.analysis.application.port.out.AnalysisEventPublisherPort;
import com.pyrosense.analysis.application.port.out.BaselineProfileRepositoryPort;
import com.pyrosense.analysis.domain.detection.BaselineBuilder;
import com.pyrosense.analysis.domain.event.BaselineBuiltEvent;
import com.pyrosense.analysis.domain.model.BaselineProfile;
import com.pyrosense.analysis.domain.model.DetectionThresholds;
import com.pyrosense.shared.util.ClockProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class BuildBaselineService implements BuildBaselineUseCase {

    private static final Logger log = LoggerFactory.getLogger(BuildBaselineService.class);

    private final BaselineProfileRepositoryPort baselineRepository;
    private final AnalysisEventPublisherPort eventPublisher;
    private final DetectionThresholds thresholds;
    private final ConcurrentHashMap<String, BaselineBuilder> builders = new ConcurrentHashMap<>();

    public BuildBaselineService(BaselineProfileRepositoryPort baselineRepository,
                                AnalysisEventPublisherPort eventPublisher,
                                DetectionThresholds thresholds) {
        this.baselineRepository = baselineRepository;
        this.eventPublisher = eventPublisher;
        this.thresholds = thresholds;
    }

    @Override
    public BaselineProfile buildOrUpdate(BuildBaselineCommand command) {
        String key = command.deviceId().value().toString();
        Instant now = ClockProvider.now();

        BaselineBuilder builder = builders.computeIfAbsent(key,
                k -> new BaselineBuilder(command.deviceId(), thresholds.baselineMinimumSamples()));

        builder.addWindow(command.window());

        if (builder.isReady()) {
            BaselineProfile profile = builder.build(now);
            baselineRepository.save(profile);
            builders.remove(key);

            eventPublisher.publish(new BaselineBuiltEvent(
                    UUID.randomUUID(), now, command.deviceId(), profile.sampleCount()
            ));
            log.info("Baseline built: device={}, samples={}", command.deviceId(), profile.sampleCount());
            return profile;
        }

        log.debug("Baseline learning: device={}, progress={}/{}",
                command.deviceId(), builder.sampleCount(), thresholds.baselineMinimumSamples());
        return builder.build(now);
    }
}
