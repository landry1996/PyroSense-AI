package com.pyrosense.analysis.application.usecase;

import com.pyrosense.analysis.application.port.in.DetectMicroArcPatternUseCase.DetectMicroArcCommand;
import com.pyrosense.analysis.domain.model.*;
import com.pyrosense.shared.id.DeviceId;
import com.pyrosense.shared.id.TenantId;
import com.pyrosense.shared.util.ClockProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;

class DetectMicroArcPatternServiceTest {

    private DetectMicroArcPatternService service;
    private final DeviceId deviceId = DeviceId.generate();
    private final TenantId tenantId = TenantId.generate();
    private final Instant now = Instant.parse("2025-01-15T10:00:00Z");

    @BeforeEach
    void setUp() {
        ClockProvider.setClock(Clock.fixed(now, ZoneId.of("UTC")));
        service = new DetectMicroArcPatternService(DetectionThresholds.defaults());
    }

    @AfterEach
    void tearDown() {
        ClockProvider.reset();
    }

    @Test
    void shouldDetectMicroArcPattern() {
        List<SignalWindow.SignalSample> samples = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            samples.add(new SignalWindow.SignalSample(now.plusSeconds(i * 60),
                    Map.of(SignalFeature.MICRO_ARC_COUNT, 3.0)));
        }
        var window = new SignalWindow(deviceId, tenantId, samples);

        List<SignalAnomaly> result = service.detect(new DetectMicroArcCommand(window));

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().type()).isEqualTo(AnomalyType.MICRO_ARC_RECURRENT);
    }

    @Test
    void shouldNotDetectWhenNoRecurrence() {
        var sample = new SignalWindow.SignalSample(now, Map.of(SignalFeature.MICRO_ARC_COUNT, 0.0));
        var window = new SignalWindow(deviceId, tenantId, List.of(sample));

        List<SignalAnomaly> result = service.detect(new DetectMicroArcCommand(window));

        assertThat(result).isEmpty();
    }
}
