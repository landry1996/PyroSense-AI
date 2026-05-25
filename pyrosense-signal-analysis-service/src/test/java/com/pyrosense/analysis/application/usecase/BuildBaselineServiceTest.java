package com.pyrosense.analysis.application.usecase;

import com.pyrosense.analysis.application.port.in.BuildBaselineUseCase.BuildBaselineCommand;
import com.pyrosense.analysis.application.port.out.AnalysisEventPublisherPort;
import com.pyrosense.analysis.application.port.out.BaselineProfileRepositoryPort;
import com.pyrosense.analysis.domain.event.BaselineBuiltEvent;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BuildBaselineServiceTest {

    @Mock private BaselineProfileRepositoryPort baselineRepo;
    @Mock private AnalysisEventPublisherPort eventPublisher;

    private BuildBaselineService service;
    private final DeviceId deviceId = DeviceId.generate();
    private final TenantId tenantId = TenantId.generate();
    private final Instant now = Instant.parse("2025-01-15T10:00:00Z");

    @BeforeEach
    void setUp() {
        ClockProvider.setClock(Clock.fixed(now, ZoneId.of("UTC")));
        var thresholds = new DetectionThresholds(3.0, 8.0, 85.0, 5.0, 3, 10, -40.0, 0.85, 15.0, 0.3, 5);
        service = new BuildBaselineService(baselineRepo, eventPublisher, thresholds);
    }

    @AfterEach
    void tearDown() {
        ClockProvider.reset();
    }

    @Test
    void shouldBuildBaselineWhenEnoughSamples() {
        List<SignalWindow.SignalSample> samples = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            samples.add(new SignalWindow.SignalSample(now.plusSeconds(i),
                    Map.of(SignalFeature.THD, 4.0 + i * 0.2)));
        }
        var window = new SignalWindow(deviceId, tenantId, samples);

        BaselineProfile result = service.buildOrUpdate(new BuildBaselineCommand(deviceId, window));

        assertThat(result.isReady()).isTrue();
        verify(baselineRepo).save(any(BaselineProfile.class));

        var captor = ArgumentCaptor.forClass(DomainEvent.class);
        verify(eventPublisher).publish(captor.capture());
        assertThat(captor.getValue()).isInstanceOf(BaselineBuiltEvent.class);
    }

    @Test
    void shouldAccumulateAcrossMultipleCalls() {
        var s1 = new SignalWindow.SignalSample(now, Map.of(SignalFeature.THD, 4.0));
        var window1 = new SignalWindow(deviceId, tenantId, List.of(s1));

        BaselineProfile result1 = service.buildOrUpdate(new BuildBaselineCommand(deviceId, window1));
        assertThat(result1.isReady()).isFalse();
        verify(baselineRepo, never()).save(any());

        List<SignalWindow.SignalSample> samples = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            samples.add(new SignalWindow.SignalSample(now.plusSeconds(i + 1),
                    Map.of(SignalFeature.THD, 4.5 + i * 0.1)));
        }
        var window2 = new SignalWindow(deviceId, tenantId, samples);

        BaselineProfile result2 = service.buildOrUpdate(new BuildBaselineCommand(deviceId, window2));
        assertThat(result2.isReady()).isTrue();
        verify(baselineRepo).save(any());
    }

    @Test
    void shouldNotPublishEventBeforeReady() {
        var s = new SignalWindow.SignalSample(now, Map.of(SignalFeature.THD, 4.0));
        var window = new SignalWindow(deviceId, tenantId, List.of(s));

        service.buildOrUpdate(new BuildBaselineCommand(deviceId, window));

        verify(eventPublisher, never()).publish(any());
    }
}
