package com.pyrosense.ingestion.config;

import com.pyrosense.ingestion.application.port.out.DataQualityRepositoryPort;
import com.pyrosense.ingestion.application.port.out.DatasetAuditPort;
import com.pyrosense.ingestion.application.port.out.DatasetRepositoryPort;
import com.pyrosense.ingestion.application.port.out.DeviceAuthorizationPort;
import com.pyrosense.ingestion.application.port.out.DeviceCapabilityLookupPort;
import com.pyrosense.ingestion.application.port.out.DeviceTelemetryStatsPort;
import com.pyrosense.ingestion.application.port.out.HeartbeatRepositoryPort;
import com.pyrosense.ingestion.application.port.out.IdempotencyPort;
import com.pyrosense.ingestion.application.port.out.TelemetryEventPublisherPort;
import com.pyrosense.ingestion.application.port.out.TelemetryRepositoryPort;
import com.pyrosense.ingestion.application.usecase.AssessTelemetryQualityService;
import com.pyrosense.ingestion.application.usecase.ComputeDailyDeviceDataQualityService;
import com.pyrosense.ingestion.application.usecase.IngestHeartbeatService;
import com.pyrosense.ingestion.application.usecase.IngestTelemetryService;
import com.pyrosense.ingestion.application.usecase.ListDataQualityIssuesService;
import com.pyrosense.ingestion.application.usecase.MarkDataQualityIssueReviewedService;
import com.pyrosense.ingestion.application.usecase.QueryDatasetCandidatesService;
import com.pyrosense.ingestion.application.usecase.RequestDatasetExportService;
import com.pyrosense.ingestion.application.usecase.SubmitFieldFeedbackService;
import com.pyrosense.ingestion.domain.model.dataset.PseudonymizationService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class UseCaseConfig {

    @Bean
    public IngestTelemetryService ingestTelemetryService(
            TelemetryRepositoryPort repository,
            TelemetryEventPublisherPort eventPublisher,
            DeviceAuthorizationPort deviceAuth,
            IdempotencyPort idempotency) {
        return new IngestTelemetryService(repository, eventPublisher, deviceAuth, idempotency);
    }

    @Bean
    public IngestHeartbeatService ingestHeartbeatService(
            DeviceAuthorizationPort deviceAuth,
            TelemetryEventPublisherPort eventPublisher,
            HeartbeatRepositoryPort heartbeatRepository) {
        return new IngestHeartbeatService(deviceAuth, eventPublisher, heartbeatRepository);
    }

    @Bean
    public AssessTelemetryQualityService assessTelemetryQualityService(
            DeviceTelemetryStatsPort statsPort,
            DataQualityRepositoryPort qualityRepository,
            DeviceCapabilityLookupPort capabilityLookup,
            TelemetryEventPublisherPort eventPublisher) {
        return new AssessTelemetryQualityService(statsPort, qualityRepository, capabilityLookup, eventPublisher);
    }

    @Bean
    public ComputeDailyDeviceDataQualityService computeDailyDeviceDataQualityService(
            AssessTelemetryQualityService assessService) {
        return new ComputeDailyDeviceDataQualityService(assessService);
    }

    @Bean
    public ListDataQualityIssuesService listDataQualityIssuesService(
            DataQualityRepositoryPort qualityRepository) {
        return new ListDataQualityIssuesService(qualityRepository);
    }

    @Bean
    public MarkDataQualityIssueReviewedService markDataQualityIssueReviewedService(
            DataQualityRepositoryPort qualityRepository) {
        return new MarkDataQualityIssueReviewedService(qualityRepository);
    }

    @Bean
    public PseudonymizationService pseudonymizationService(
            @Value("${pyrosense.dataset.pseudonymization-secret:pyrosense-default-pseudonym-key-change-in-prod}") String secret) {
        return new PseudonymizationService(secret);
    }

    @Bean
    public SubmitFieldFeedbackService submitFieldFeedbackService(
            DatasetRepositoryPort datasetRepository,
            DatasetAuditPort auditPort,
            PseudonymizationService pseudonymizationService) {
        return new SubmitFieldFeedbackService(datasetRepository, auditPort, pseudonymizationService);
    }

    @Bean
    public QueryDatasetCandidatesService queryDatasetCandidatesService(
            DatasetRepositoryPort datasetRepository,
            PseudonymizationService pseudonymizationService) {
        return new QueryDatasetCandidatesService(datasetRepository, pseudonymizationService);
    }

    @Bean
    public RequestDatasetExportService requestDatasetExportService(
            DatasetRepositoryPort datasetRepository,
            DatasetAuditPort auditPort,
            PseudonymizationService pseudonymizationService) {
        return new RequestDatasetExportService(datasetRepository, auditPort, pseudonymizationService);
    }
}
