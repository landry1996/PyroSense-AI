package com.pyrosense.ingestion.application.usecase;

import com.pyrosense.ingestion.application.port.in.AssessTelemetryQualityUseCase;
import com.pyrosense.ingestion.application.port.out.DataQualityRepositoryPort;
import com.pyrosense.ingestion.application.port.out.DeviceCapabilityLookupPort;
import com.pyrosense.ingestion.application.port.out.DeviceTelemetryStatsPort;
import com.pyrosense.ingestion.application.port.out.TelemetryEventPublisherPort;
import com.pyrosense.ingestion.domain.event.DataQualityIssueDetectedEvent;
import com.pyrosense.ingestion.domain.event.DeviceDataQualityScoreUpdatedEvent;
import com.pyrosense.ingestion.domain.model.quality.*;
import com.pyrosense.shared.util.ClockProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

public class AssessTelemetryQualityService implements AssessTelemetryQualityUseCase {

    private static final Logger log = LoggerFactory.getLogger(AssessTelemetryQualityService.class);
    private static final String MINIMUM_FIRMWARE_VERSION = "1.0.0";

    private final DeviceTelemetryStatsPort statsPort;
    private final DataQualityRepositoryPort qualityRepository;
    private final DeviceCapabilityLookupPort capabilityLookup;
    private final TelemetryEventPublisherPort eventPublisher;

    public AssessTelemetryQualityService(DeviceTelemetryStatsPort statsPort,
                                          DataQualityRepositoryPort qualityRepository,
                                          DeviceCapabilityLookupPort capabilityLookup,
                                          TelemetryEventPublisherPort eventPublisher) {
        this.statsPort = statsPort;
        this.qualityRepository = qualityRepository;
        this.capabilityLookup = capabilityLookup;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public DataQualityAssessment execute(AssessCommand command) {
        var stats = statsPort.computeStats(command.deviceId(), command.periodStart(), command.periodEnd());
        int expectedMessages = statsPort.countExpectedMessages(command.deviceId(), command.periodStart(), command.periodEnd());
        int receivedMessages = statsPort.countReceivedMessages(command.deviceId(), command.periodStart(), command.periodEnd());
        int expectedHeartbeats = statsPort.countExpectedHeartbeats(command.deviceId(), command.periodStart(), command.periodEnd());
        int receivedHeartbeats = statsPort.countReceivedHeartbeats(command.deviceId(), command.periodStart(), command.periodEnd());

        var capability = capabilityLookup.getCapability(command.deviceId());
        boolean firmwareObsolete = isFirmwareObsolete(stats.latestFirmwareVersion());

        var calibrationStatus = capability
                .map(c -> DeviceCalibrationStatus.calibrated(command.periodStart(), "factory"))
                .orElse(DeviceCalibrationStatus.unknown());

        var assessment = DataQualityAssessment.builder()
                .deviceId(command.deviceId())
                .tenantId(command.tenantId())
                .periodStart(command.periodStart())
                .periodEnd(command.periodEnd())
                .missingDataRatio(MissingDataRatio.compute(expectedMessages, receivedMessages))
                .rangeStatus(MeasurementRangeStatus.compute(
                        stats.totalReadings(), stats.outOfRangeReadings(), stats.outOfRangeFields()))
                .noiseLevel(new SensorNoiseLevel(
                        stats.hfNoiseAverage(), stats.hfNoiseMax(),
                        stats.currentStdDev(), stats.voltageStdDev()))
                .completeness(new DataCompleteness(
                        Duration.between(command.periodStart(), command.periodEnd()),
                        expectedHeartbeats, receivedHeartbeats,
                        expectedMessages, receivedMessages,
                        stats.sequenceGaps(), stats.duplicateSequences()))
                .calibrationStatus(calibrationStatus)
                .averageSignalQuality(stats.averageSignalQuality())
                .clockDriftSeconds(stats.maxClockDriftSeconds())
                .offlineCount(stats.offlineTransitions())
                .firmwareVersion(stats.latestFirmwareVersion())
                .firmwareObsolete(firmwareObsolete)
                .build();

        qualityRepository.saveAssessment(assessment);
        qualityRepository.saveIssues(assessment.getIssues());

        publishEvents(assessment);

        log.info("Data quality assessed: device={} score={} grade={} issues={}",
                command.deviceId(), assessment.getOverallScore(),
                assessment.getGrade(), assessment.getIssues().size());

        return assessment;
    }

    private void publishEvents(DataQualityAssessment assessment) {
        eventPublisher.publish(new DeviceDataQualityScoreUpdatedEvent(
                UUID.randomUUID(), ClockProvider.now(),
                assessment.getDeviceId(), assessment.getTenantId(),
                assessment.getOverallScore(), assessment.getGrade(),
                assessment.getIssues().size(), assessment.isTrustworthy()));

        for (var issue : assessment.getIssues()) {
            if (issue.getSeverity() == DataQualityIssue.IssueSeverity.HIGH
                    || issue.getSeverity() == DataQualityIssue.IssueSeverity.CRITICAL) {
                eventPublisher.publish(new DataQualityIssueDetectedEvent(
                        UUID.randomUUID(), ClockProvider.now(),
                        assessment.getDeviceId(), assessment.getTenantId(),
                        issue.getType(), issue.getSeverity(), issue.getDetails()));
            }
        }
    }

    private boolean isFirmwareObsolete(String version) {
        if (version == null || "unknown".equals(version)) return true;
        return version.compareTo(MINIMUM_FIRMWARE_VERSION) < 0;
    }
}
