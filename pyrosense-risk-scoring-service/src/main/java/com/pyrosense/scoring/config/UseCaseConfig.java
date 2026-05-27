package com.pyrosense.scoring.config;

import com.pyrosense.scoring.application.port.out.RiskAssessmentRepositoryPort;
import com.pyrosense.scoring.application.port.out.RiskModelPort;
import com.pyrosense.scoring.application.port.out.ScoringEventPublisherPort;
import com.pyrosense.scoring.application.port.out.ScoringFeedbackRepositoryPort;
import com.pyrosense.scoring.application.usecase.CalculateRiskService;
import com.pyrosense.scoring.application.usecase.GetFeedbackHistoryService;
import com.pyrosense.scoring.application.usecase.GetRiskScoreService;
import com.pyrosense.scoring.application.usecase.GetTenantRiskService;
import com.pyrosense.scoring.application.usecase.ProcessFieldFeedbackService;
import com.pyrosense.scoring.domain.model.ScoringWeights;
import com.pyrosense.scoring.domain.scoring.RiskScoringEngine;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(ScoringProperties.class)
public class UseCaseConfig {

    @Bean
    public ScoringWeights scoringWeights(ScoringProperties props) {
        return new ScoringWeights(
                props.microArcWeight(),
                props.thdDriftWeight(),
                props.temperatureTrendWeight(),
                props.transientWeight(),
                props.hfNoiseWeight(),
                props.recencyDecayFactor(),
                props.repetitionBoostFactor(),
                props.deviceReliabilityWeight(),
                props.historyWindowDays(),
                props.recencyWindowHours()
        );
    }

    @Bean
    public RiskScoringEngine riskScoringEngine(ScoringWeights weights) {
        return new RiskScoringEngine(weights);
    }

    @Bean
    public CalculateRiskService calculateRiskService(RiskAssessmentRepositoryPort repository,
                                                      ScoringEventPublisherPort eventPublisher,
                                                      RiskModelPort riskModel,
                                                      RiskScoringEngine engine) {
        return new CalculateRiskService(repository, eventPublisher, riskModel, engine);
    }

    @Bean
    public GetRiskScoreService getRiskScoreService(RiskAssessmentRepositoryPort repository) {
        return new GetRiskScoreService(repository);
    }

    @Bean
    public GetTenantRiskService getTenantRiskService(RiskAssessmentRepositoryPort repository) {
        return new GetTenantRiskService(repository);
    }

    @Bean
    public ProcessFieldFeedbackService processFieldFeedbackService(ScoringFeedbackRepositoryPort feedbackRepository,
                                                                     ScoringEventPublisherPort eventPublisher,
                                                                     MeterRegistry registry) {
        return new ProcessFieldFeedbackService(feedbackRepository, eventPublisher, registry);
    }

    @Bean
    public GetFeedbackHistoryService getFeedbackHistoryService(ScoringFeedbackRepositoryPort feedbackRepository) {
        return new GetFeedbackHistoryService(feedbackRepository);
    }
}
