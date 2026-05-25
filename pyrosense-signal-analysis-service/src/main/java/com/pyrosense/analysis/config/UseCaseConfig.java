package com.pyrosense.analysis.config;

import com.pyrosense.analysis.application.port.out.*;
import com.pyrosense.analysis.application.usecase.*;
import com.pyrosense.analysis.domain.detection.SignalAnalysisEngine;
import com.pyrosense.analysis.domain.model.DetectionThresholds;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(AnalysisProperties.class)
public class UseCaseConfig {

    @Bean
    public DetectionThresholds detectionThresholds(AnalysisProperties props) {
        return new DetectionThresholds(
                props.zScoreThreshold(),
                props.thdMaxPercent(),
                props.temperatureMaxCelsius(),
                props.temperatureRiseRatePerHour(),
                props.microArcRecurrenceThreshold(),
                props.transientCountThreshold(),
                props.hfNoiseMaxDb(),
                props.powerFactorMin(),
                props.driftPercentThreshold(),
                props.exponentialSmoothingAlpha(),
                props.baselineMinimumSamples()
        );
    }

    @Bean
    public SignalAnalysisEngine signalAnalysisEngine(DetectionThresholds thresholds) {
        return new SignalAnalysisEngine(thresholds);
    }

    @Bean
    public AnalyzeSignalService analyzeSignalService(BaselineProfileRepositoryPort baselineRepo,
                                                      AnomalyRepositoryPort anomalyRepo,
                                                      AnalysisEventPublisherPort eventPublisher,
                                                      MachineLearningInferencePort mlPort,
                                                      SignalAnalysisEngine engine) {
        return new AnalyzeSignalService(baselineRepo, anomalyRepo, eventPublisher, mlPort, engine);
    }

    @Bean
    public BuildBaselineService buildBaselineService(BaselineProfileRepositoryPort baselineRepo,
                                                     AnalysisEventPublisherPort eventPublisher,
                                                     DetectionThresholds thresholds) {
        return new BuildBaselineService(baselineRepo, eventPublisher, thresholds);
    }

    @Bean
    public DetectSignalDriftService detectSignalDriftService(BaselineProfileRepositoryPort baselineRepo,
                                                             AnalysisEventPublisherPort eventPublisher,
                                                             DetectionThresholds thresholds) {
        return new DetectSignalDriftService(baselineRepo, eventPublisher, thresholds);
    }

    @Bean
    public DetectMicroArcPatternService detectMicroArcPatternService(DetectionThresholds thresholds) {
        return new DetectMicroArcPatternService(thresholds);
    }
}
