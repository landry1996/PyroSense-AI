package com.pyrosense.scoring.performance;

import com.pyrosense.scoring.application.port.in.CalculateRiskUseCase.CalculateRiskCommand;
import com.pyrosense.scoring.application.port.out.RiskAssessmentRepositoryPort;
import com.pyrosense.scoring.application.port.out.RiskModelPort;
import com.pyrosense.scoring.application.port.out.ScoringEventPublisherPort;
import com.pyrosense.scoring.application.usecase.CalculateRiskService;
import com.pyrosense.scoring.domain.model.AnomalyInput;
import com.pyrosense.scoring.domain.model.RiskAssessment;
import com.pyrosense.scoring.domain.model.ScoringWeights;
import com.pyrosense.scoring.domain.scoring.RiskScoringEngine;
import com.pyrosense.shared.id.DeviceId;
import com.pyrosense.shared.id.ElectricalPanelId;
import com.pyrosense.shared.util.ClockProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RiskScoringPerformanceSmokeTest {

    private static final int REQUEST_COUNT = 500;
    private static final long MAX_AVG_MS = 5;
    private static final long MAX_TOTAL_MS = 3000;

    @Mock private RiskAssessmentRepositoryPort repository;
    @Mock private ScoringEventPublisherPort eventPublisher;
    @Mock private RiskModelPort riskModel;

    private CalculateRiskService service;
    private final Instant fixedNow = Instant.parse("2025-06-01T12:00:00Z");

    @BeforeEach
    void setUp() {
        ClockProvider.setClock(Clock.fixed(fixedNow, ZoneId.of("UTC")));
        var engine = new RiskScoringEngine(ScoringWeights.defaults());
        service = new CalculateRiskService(repository, eventPublisher, riskModel, engine);

        when(repository.findRecentScores(any(), anyInt())).thenReturn(List.of());
        when(riskModel.isAvailable()).thenReturn(false);
        doNothing().when(repository).save(any());
        doNothing().when(eventPublisher).publish(any());
    }

    @AfterEach
    void tearDown() {
        ClockProvider.reset();
    }

    @Test
    void shouldProcess500CalculationsWithAcceptableLatency() {
        List<CalculateRiskCommand> commands = buildCommands(REQUEST_COUNT);

        // Warm up JIT
        for (int i = 0; i < 30; i++) {
            service.calculate(commands.get(i));
        }

        long startNanos = System.nanoTime();
        List<RiskAssessment> results = new ArrayList<>(REQUEST_COUNT);

        for (CalculateRiskCommand command : commands) {
            RiskAssessment result = service.calculate(command);
            results.add(result);
        }

        long elapsedNanos = System.nanoTime() - startNanos;
        long elapsedMs = elapsedNanos / 1_000_000;
        double avgMs = (double) elapsedNanos / REQUEST_COUNT / 1_000_000.0;

        assertThat(results).hasSize(REQUEST_COUNT);
        assertThat(results).allSatisfy(assessment -> {
            assertThat(assessment).isNotNull();
            assertThat(assessment.score().value()).isBetween(0, 100);
        });

        assertThat(elapsedMs)
                .as("Total processing time for %d calculations should be under %d ms, was %d ms",
                        REQUEST_COUNT, MAX_TOTAL_MS, elapsedMs)
                .isLessThan(MAX_TOTAL_MS);

        assertThat(avgMs)
                .as("Average processing time per calculation should be under %d ms, was %.2f ms",
                        MAX_AVG_MS, avgMs)
                .isLessThan((double) MAX_AVG_MS);
    }

    private List<CalculateRiskCommand> buildCommands(int count) {
        List<CalculateRiskCommand> commands = new ArrayList<>(count);
        String[] anomalyTypes = {
                "MICRO_ARC_RECURRENT", "THD_ABNORMAL", "TEMPERATURE_RISING",
                "TRANSIENT_ABNORMAL", "HF_NOISE_ELEVATED", "BASELINE_DRIFT"
        };

        for (int i = 0; i < count; i++) {
            DeviceId deviceId = DeviceId.generate();
            ElectricalPanelId panelId = ElectricalPanelId.generate();
            UUID circuitId = UUID.randomUUID();

            int anomalyCount = (i % 5) + 1;
            List<AnomalyInput> anomalies = new ArrayList<>(anomalyCount);
            for (int j = 0; j < anomalyCount; j++) {
                anomalies.add(new AnomalyInput(
                        deviceId,
                        anomalyTypes[(i + j) % anomalyTypes.length],
                        0.5 + (((i + j) % 5) * 0.1),
                        2.0 + (j * 0.5),
                        fixedNow.minusSeconds((j + 1) * 3600L)
                ));
            }

            commands.add(new CalculateRiskCommand(
                    deviceId,
                    panelId,
                    circuitId,
                    anomalies,
                    i % 10 != 0,
                    i % 7 != 0
            ));
        }
        return commands;
    }
}
