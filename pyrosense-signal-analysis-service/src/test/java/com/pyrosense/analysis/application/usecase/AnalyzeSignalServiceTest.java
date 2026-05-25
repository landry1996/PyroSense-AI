package com.pyrosense.analysis.application.usecase;

import com.pyrosense.analysis.application.port.in.AnalyzeSignalUseCase.AnalyzeTelemetryWindowCommand;
import com.pyrosense.analysis.application.port.out.*;
import com.pyrosense.analysis.domain.detection.SignalAnalysisEngine;
import com.pyrosense.analysis.domain.model.*;
import com.pyrosense.shared.domain.DomainEvent;
import com.pyrosense.shared.id.DeviceId;
import com.pyrosense.shared.id.TenantId;
import com.pyrosense.shared.util.ClockProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AnalyzeSignalServiceTest {

    @Mock private BaselineProfileRepositoryPort baselineRepo;
    @Mock private AnomalyRepositoryPort anomalyRepo;
    @Mock private AnalysisEventPublisherPort eventPublisher;
    @Mock private MachineLearningInferencePort mlPort;

    private AnalyzeSignalService service;
    private final DeviceId deviceId = DeviceId.generate();
    private final TenantId tenantId = TenantId.generate();
    private final Instant now = Instant.parse("2025-01-15T10:00:00Z");

    @BeforeEach
    void setUp() {
        ClockProvider.setClock(Clock.fixed(now, ZoneId.of("UTC")));
        var engine = new SignalAnalysisEngine(DetectionThresholds.defaults());
        service = new AnalyzeSignalService(baselineRepo, anomalyRepo, eventPublisher, mlPort, engine);
    }

    @AfterEach
    void tearDown() {
        ClockProvider.reset();
    }

    @Test
    void shouldReturnNoAnomalyForNormalSignal() {
        when(baselineRepo.findByDeviceId(deviceId)).thenReturn(Optional.empty());
        when(mlPort.isAvailable()).thenReturn(false);

        var sample = new SignalWindow.SignalSample(now, Map.of(
                SignalFeature.THD, 4.0, SignalFeature.TEMPERATURE, 40.0, SignalFeature.MICRO_ARC_COUNT, 0.0));
        var window = new SignalWindow(deviceId, tenantId, List.of(sample));

        AnalysisResult result = service.analyze(new AnalyzeTelemetryWindowCommand(window));

        assertThat(result.hasAnomalies()).isFalse();
        verify(anomalyRepo).saveResult(result);
        verify(eventPublisher, never()).publish(any());
    }

    @Test
    void shouldPublishEventWhenAnomalyDetected() {
        when(baselineRepo.findByDeviceId(deviceId)).thenReturn(Optional.empty());
        when(mlPort.isAvailable()).thenReturn(false);

        List<SignalWindow.SignalSample> samples = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            samples.add(new SignalWindow.SignalSample(now.plusSeconds(i * 60),
                    Map.of(SignalFeature.MICRO_ARC_COUNT, 5.0, SignalFeature.TEMPERATURE, 90.0)));
        }
        var window = new SignalWindow(deviceId, tenantId, samples);

        AnalysisResult result = service.analyze(new AnalyzeTelemetryWindowCommand(window));

        assertThat(result.hasAnomalies()).isTrue();
        verify(eventPublisher).publish(any(DomainEvent.class));
    }

    @Test
    void shouldUseBaselineForZScoreDetection() {
        Map<SignalFeature, StatisticalRange> stats = new EnumMap<>(SignalFeature.class);
        stats.put(SignalFeature.THD, new StatisticalRange(4.0, 0.5, 3.0, 5.0, 3.2, 3.7, 4.0, 4.3, 4.8, 1000));
        var baseline = new BaselineProfile(UUID.randomUUID(), deviceId, stats, 1000, now, now, 100);
        when(baselineRepo.findByDeviceId(deviceId)).thenReturn(Optional.of(baseline));
        when(mlPort.isAvailable()).thenReturn(false);

        var sample = new SignalWindow.SignalSample(now, Map.of(SignalFeature.THD, 12.0));
        var window = new SignalWindow(deviceId, tenantId, List.of(sample));

        AnalysisResult result = service.analyze(new AnalyzeTelemetryWindowCommand(window));

        assertThat(result.hasAnomalies()).isTrue();
        assertThat(result.baselineAvailable()).isTrue();
    }

    @Test
    void shouldGracefullyHandleMlFailure() {
        when(baselineRepo.findByDeviceId(deviceId)).thenReturn(Optional.empty());
        when(mlPort.isAvailable()).thenReturn(true);
        when(mlPort.infer(any())).thenThrow(new RuntimeException("ML service down"));

        var sample = new SignalWindow.SignalSample(now, Map.of(SignalFeature.THD, 4.0));
        var window = new SignalWindow(deviceId, tenantId, List.of(sample));

        assertThatNoException().isThrownBy(() ->
                service.analyze(new AnalyzeTelemetryWindowCommand(window)));
    }
}
