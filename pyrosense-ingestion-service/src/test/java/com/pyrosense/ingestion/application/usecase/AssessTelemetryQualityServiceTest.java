package com.pyrosense.ingestion.application.usecase;

import com.pyrosense.ingestion.application.port.in.AssessTelemetryQualityUseCase;
import com.pyrosense.ingestion.application.port.out.DataQualityRepositoryPort;
import com.pyrosense.ingestion.application.port.out.DeviceCapabilityLookupPort;
import com.pyrosense.ingestion.application.port.out.DeviceTelemetryStatsPort;
import com.pyrosense.ingestion.application.port.out.TelemetryEventPublisherPort;
import com.pyrosense.ingestion.domain.model.quality.DataQualityAssessment;
import com.pyrosense.ingestion.domain.model.quality.DataQualityIssue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class AssessTelemetryQualityServiceTest {

    private DeviceTelemetryStatsPort statsPort;
    private DataQualityRepositoryPort qualityRepository;
    private DeviceCapabilityLookupPort capabilityLookup;
    private TelemetryEventPublisherPort eventPublisher;
    private AssessTelemetryQualityService service;

    private static final String DEVICE_ID = "device-001";
    private static final String TENANT_ID = "tenant-001";
    private static final Instant FROM = Instant.now().minus(24, ChronoUnit.HOURS);
    private static final Instant TO = Instant.now();

    @BeforeEach
    void setUp() {
        statsPort = mock(DeviceTelemetryStatsPort.class);
        qualityRepository = mock(DataQualityRepositoryPort.class);
        capabilityLookup = mock(DeviceCapabilityLookupPort.class);
        eventPublisher = mock(TelemetryEventPublisherPort.class);
        service = new AssessTelemetryQualityService(statsPort, qualityRepository, capabilityLookup, eventPublisher);
    }

    @Test
    @DisplayName("healthy device produces Grade A assessment")
    void healthyDevice() {
        setupGoodStats();

        var result = execute();

        assertThat(result.getGrade()).isEqualTo(DataQualityAssessment.QualityGrade.A);
        assertThat(result.isTrustworthy()).isTrue();
        assertThat(result.getIssues()).isEmpty();
        verify(qualityRepository).saveAssessment(any());
        verify(eventPublisher).publish(any());
    }

    @Test
    @DisplayName("out of range values generate HIGH issue")
    void outOfRangeValues() {
        setupStats(new DeviceTelemetryStatsPort.TelemetryStats(
                1000, 150, List.of("voltage", "current"),
                0.9, 5.0, 0.2, 0.3, 0.1, 0.5,
                0, 0, 0, "1.2.0"));

        var result = execute();

        assertThat(result.getIssues()).anyMatch(i ->
                i.getType() == DataQualityIssue.IssueType.OUT_OF_RANGE
                        && i.getSeverity() == DataQualityIssue.IssueSeverity.HIGH);
        assertThat(result.getOverallScore()).isLessThan(100);
    }

    @Test
    @DisplayName("future timestamp (large clock drift) generates TIMESTAMP_INCONSISTENT")
    void futureTimestamp() {
        setupStats(new DeviceTelemetryStatsPort.TelemetryStats(
                1000, 0, List.of(),
                0.9, 500.0, 0.2, 0.3, 0.1, 0.5,
                0, 0, 0, "1.2.0"));

        var result = execute();

        assertThat(result.getIssues()).anyMatch(i ->
                i.getType() == DataQualityIssue.IssueType.TIMESTAMP_INCONSISTENT
                        && i.getSeverity() == DataQualityIssue.IssueSeverity.HIGH);
    }

    @Test
    @DisplayName("old timestamp (moderate drift) generates low severity issue")
    void oldTimestamp() {
        setupStats(new DeviceTelemetryStatsPort.TelemetryStats(
                1000, 0, List.of(),
                0.9, 60.0, 0.2, 0.3, 0.1, 0.5,
                0, 0, 0, "1.2.0"));

        var result = execute();

        assertThat(result.getIssues()).anyMatch(i ->
                i.getType() == DataQualityIssue.IssueType.TIMESTAMP_INCONSISTENT
                        && i.getSeverity() == DataQualityIssue.IssueSeverity.LOW);
    }

    @Test
    @DisplayName("duplicate sequence numbers generate SEQUENCE_ANOMALY")
    void duplicateSequenceNumbers() {
        setupStats(new DeviceTelemetryStatsPort.TelemetryStats(
                1000, 0, List.of(),
                0.9, 5.0, 0.2, 0.3, 0.1, 0.5,
                3, 5, 0, "1.2.0"));

        var result = execute();

        assertThat(result.getIssues()).anyMatch(i ->
                i.getType() == DataQualityIssue.IssueType.SEQUENCE_ANOMALY
                        && i.getDetails().contains("Duplicates: 5"));
    }

    @Test
    @DisplayName("missing data (low received count) generates MISSING_DATA")
    void missingData() {
        when(statsPort.computeStats(DEVICE_ID, FROM, TO))
                .thenReturn(new DeviceTelemetryStatsPort.TelemetryStats(
                        600, 0, List.of(), 0.9, 5.0, 0.2, 0.3, 0.1, 0.5,
                        0, 0, 0, "1.2.0"));
        when(statsPort.countExpectedMessages(DEVICE_ID, FROM, TO)).thenReturn(1000);
        when(statsPort.countReceivedMessages(DEVICE_ID, FROM, TO)).thenReturn(600);
        when(statsPort.countExpectedHeartbeats(DEVICE_ID, FROM, TO)).thenReturn(100);
        when(statsPort.countReceivedHeartbeats(DEVICE_ID, FROM, TO)).thenReturn(100);
        when(capabilityLookup.getCapability(DEVICE_ID)).thenReturn(Optional.of(
                new DeviceCapabilityLookupPort.DeviceCapability(DEVICE_ID, TENANT_ID, "PSM-1", "1.2.0", true, true, 10000)));

        var result = execute();

        assertThat(result.getIssues()).anyMatch(i ->
                i.getType() == DataQualityIssue.IssueType.MISSING_DATA
                        && i.getSeverity() == DataQualityIssue.IssueSeverity.CRITICAL);
    }

    @Test
    @DisplayName("low signal quality generates LOW_SIGNAL_QUALITY issue")
    void lowSignalQuality() {
        setupStats(new DeviceTelemetryStatsPort.TelemetryStats(
                1000, 0, List.of(),
                0.15, 5.0, 0.2, 0.3, 0.1, 0.5,
                0, 0, 0, "1.2.0"));

        var result = execute();

        assertThat(result.getIssues()).anyMatch(i ->
                i.getType() == DataQualityIssue.IssueType.LOW_SIGNAL_QUALITY
                        && i.getSeverity() == DataQualityIssue.IssueSeverity.HIGH);
    }

    @Test
    @DisplayName("device frequently offline generates FREQUENT_OFFLINE")
    void frequentOffline() {
        setupStats(new DeviceTelemetryStatsPort.TelemetryStats(
                1000, 0, List.of(),
                0.9, 5.0, 0.2, 0.3, 0.1, 0.5,
                0, 0, 8, "1.2.0"));

        var result = execute();

        assertThat(result.getIssues()).anyMatch(i ->
                i.getType() == DataQualityIssue.IssueType.FREQUENT_OFFLINE
                        && i.getDetails().contains("8 times"));
    }

    @Test
    @DisplayName("critical issues trigger event publication")
    void criticalIssuesPublishEvents() {
        setupStats(new DeviceTelemetryStatsPort.TelemetryStats(
                1000, 150, List.of("voltage"),
                0.15, 500.0, 0.2, 0.3, 0.1, 0.5,
                0, 0, 8, "1.2.0"));

        execute();

        verify(eventPublisher, atLeast(2)).publish(any());
    }

    @Test
    @DisplayName("assessment is persisted along with issues")
    void assessmentPersisted() {
        setupStats(new DeviceTelemetryStatsPort.TelemetryStats(
                1000, 150, List.of("voltage"),
                0.9, 5.0, 0.2, 0.3, 0.1, 0.5,
                0, 0, 0, "1.2.0"));

        execute();

        verify(qualityRepository).saveAssessment(any(DataQualityAssessment.class));
        verify(qualityRepository).saveIssues(anyList());
    }

    private DataQualityAssessment execute() {
        var command = new AssessTelemetryQualityUseCase.AssessCommand(DEVICE_ID, TENANT_ID, FROM, TO);
        return service.execute(command);
    }

    private void setupGoodStats() {
        setupStats(new DeviceTelemetryStatsPort.TelemetryStats(
                1000, 0, List.of(),
                0.95, 2.0, 0.2, 0.3, 0.1, 0.5,
                0, 0, 0, "1.2.0"));
    }

    private void setupStats(DeviceTelemetryStatsPort.TelemetryStats stats) {
        when(statsPort.computeStats(DEVICE_ID, FROM, TO)).thenReturn(stats);
        when(statsPort.countExpectedMessages(DEVICE_ID, FROM, TO)).thenReturn(1000);
        when(statsPort.countReceivedMessages(DEVICE_ID, FROM, TO)).thenReturn(1000);
        when(statsPort.countExpectedHeartbeats(DEVICE_ID, FROM, TO)).thenReturn(100);
        when(statsPort.countReceivedHeartbeats(DEVICE_ID, FROM, TO)).thenReturn(100);
        when(capabilityLookup.getCapability(DEVICE_ID)).thenReturn(Optional.of(
                new DeviceCapabilityLookupPort.DeviceCapability(DEVICE_ID, TENANT_ID, "PSM-1", "1.2.0", true, true, 10000)));
    }
}
