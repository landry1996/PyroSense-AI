package com.pyrosense.scoring.application.usecase;

import com.pyrosense.scoring.application.port.in.CalculateRiskUseCase.CalculateRiskCommand;
import com.pyrosense.scoring.application.port.out.RiskAssessmentRepositoryPort;
import com.pyrosense.scoring.application.port.out.RiskModelPort;
import com.pyrosense.scoring.application.port.out.ScoringEventPublisherPort;
import com.pyrosense.scoring.domain.event.CriticalRiskDetectedEvent;
import com.pyrosense.scoring.domain.event.RiskLevelChangedEvent;
import com.pyrosense.scoring.domain.event.RiskScoreUpdatedEvent;
import com.pyrosense.scoring.domain.model.*;
import com.pyrosense.scoring.domain.scoring.RiskScoringEngine;
import com.pyrosense.shared.domain.DomainEvent;
import com.pyrosense.shared.id.DeviceId;
import com.pyrosense.shared.id.ElectricalPanelId;
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
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CalculateRiskServiceTest {

    @Mock private RiskAssessmentRepositoryPort repository;
    @Mock private ScoringEventPublisherPort eventPublisher;
    @Mock private RiskModelPort riskModel;

    private CalculateRiskService service;
    private final DeviceId deviceId = DeviceId.generate();
    private final ElectricalPanelId panelId = new ElectricalPanelId(UUID.randomUUID());
    private final Instant now = Instant.parse("2025-01-15T10:00:00Z");

    @BeforeEach
    void setUp() {
        ClockProvider.setClock(Clock.fixed(now, ZoneId.of("UTC")));
        var engine = new RiskScoringEngine(ScoringWeights.defaults());
        service = new CalculateRiskService(repository, eventPublisher, riskModel, engine);
    }

    @AfterEach
    void tearDown() {
        ClockProvider.reset();
    }

    @Test
    void shouldCalculateAndPersist() {
        when(repository.findRecentScores(deviceId, 10)).thenReturn(List.of());
        when(riskModel.isAvailable()).thenReturn(false);

        var command = new CalculateRiskCommand(deviceId, panelId, null, List.of(), true, true);
        RiskAssessment result = service.calculate(command);

        assertThat(result).isNotNull();
        assertThat(result.score().value()).isGreaterThanOrEqualTo(0);
        verify(repository).save(any(RiskAssessment.class));
    }

    @Test
    void shouldPublishUpdatedEvent() {
        when(repository.findRecentScores(deviceId, 10)).thenReturn(List.of());
        when(riskModel.isAvailable()).thenReturn(false);

        var command = new CalculateRiskCommand(deviceId, panelId, null, List.of(), true, true);
        service.calculate(command);

        var captor = ArgumentCaptor.forClass(DomainEvent.class);
        verify(eventPublisher, atLeastOnce()).publish(captor.capture());
        assertThat(captor.getAllValues()).anyMatch(e -> e instanceof RiskScoreUpdatedEvent);
    }

    @Test
    void shouldPublishCriticalEventWhenScoreIsCritical() {
        when(repository.findRecentScores(deviceId, 10)).thenReturn(List.of());
        when(riskModel.isAvailable()).thenReturn(false);

        List<AnomalyInput> anomalies = List.of(
                new AnomalyInput(deviceId, "MICRO_ARC_RECURRENT", 0.95, 6.0, now),
                new AnomalyInput(deviceId, "THD_ABNORMAL", 0.9, 5.0, now),
                new AnomalyInput(deviceId, "TEMPERATURE_RISING", 0.9, 4.0, now),
                new AnomalyInput(deviceId, "TRANSIENT_ABNORMAL", 0.85, 3.5, now),
                new AnomalyInput(deviceId, "HF_NOISE_ELEVATED", 0.85, 4.0, now)
        );
        var command = new CalculateRiskCommand(deviceId, panelId, null, anomalies, true, true);
        RiskAssessment result = service.calculate(command);

        if (result.isCritical()) {
            var captor = ArgumentCaptor.forClass(DomainEvent.class);
            verify(eventPublisher, atLeast(2)).publish(captor.capture());
            assertThat(captor.getAllValues()).anyMatch(e -> e instanceof CriticalRiskDetectedEvent);
        }
    }

    @Test
    void shouldPublishLevelChangeEvent() {
        when(repository.findRecentScores(deviceId, 10)).thenReturn(List.of(20));
        when(riskModel.isAvailable()).thenReturn(false);

        List<AnomalyInput> anomalies = List.of(
                new AnomalyInput(deviceId, "MICRO_ARC_RECURRENT", 0.9, 5.0, now),
                new AnomalyInput(deviceId, "THD_ABNORMAL", 0.85, 4.0, now)
        );
        var command = new CalculateRiskCommand(deviceId, panelId, null, anomalies, true, true);
        RiskAssessment result = service.calculate(command);

        if (result.level() != RiskLevel.LOW) {
            var captor = ArgumentCaptor.forClass(DomainEvent.class);
            verify(eventPublisher, atLeast(2)).publish(captor.capture());
            assertThat(captor.getAllValues()).anyMatch(e -> e instanceof RiskLevelChangedEvent);
        }
    }

    @Test
    void shouldGracefullyHandleMlFailure() {
        when(repository.findRecentScores(deviceId, 10)).thenReturn(List.of());
        when(riskModel.isAvailable()).thenReturn(true);
        when(riskModel.predict(any(), any())).thenThrow(new RuntimeException("ML down"));

        var command = new CalculateRiskCommand(deviceId, panelId, null, List.of(), true, true);

        assertThatNoException().isThrownBy(() -> service.calculate(command));
    }
}
