package com.pyrosense.scoring.application.usecase;

import com.pyrosense.scoring.application.port.in.ProcessFieldFeedbackUseCase.FieldFeedbackCommand;
import com.pyrosense.scoring.application.port.out.ScoringEventPublisherPort;
import com.pyrosense.scoring.application.port.out.ScoringFeedbackRepositoryPort;
import com.pyrosense.scoring.domain.model.FeedbackOutcome;
import com.pyrosense.scoring.domain.model.ScoringAdjustment;
import com.pyrosense.scoring.domain.model.ScoringFeedback;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProcessFieldFeedbackServiceTest {

    @Mock private ScoringFeedbackRepositoryPort feedbackRepository;
    @Mock private ScoringEventPublisherPort eventPublisher;

    private ProcessFieldFeedbackService service;
    private SimpleMeterRegistry registry;

    @BeforeEach
    void setUp() {
        registry = new SimpleMeterRegistry();
        service = new ProcessFieldFeedbackService(feedbackRepository, eventPublisher, registry);
    }

    @Test
    void shouldSaveFeedbackAndReturnResult() {
        var command = new FieldFeedbackCommand(
                UUID.randomUUID().toString(), "tenant-1", UUID.randomUUID(), UUID.randomUUID(),
                FeedbackOutcome.CONFIRMED_DEFECT, "MICRO_ARC", 72.0, "maintenance-service", "Confirmed arc fault");

        when(feedbackRepository.countByOutcome(command.deviceId(), "MICRO_ARC", FeedbackOutcome.CONFIRMED_DEFECT))
                .thenReturn(1);
        when(feedbackRepository.countByOutcome(command.deviceId(), "MICRO_ARC", FeedbackOutcome.FALSE_POSITIVE))
                .thenReturn(0);
        when(feedbackRepository.findAdjustments(command.deviceId(), "MICRO_ARC"))
                .thenReturn(List.of());

        ScoringFeedback result = service.process(command);

        assertThat(result).isNotNull();
        assertThat(result.getOutcome()).isEqualTo(FeedbackOutcome.CONFIRMED_DEFECT);
        assertThat(result.getAnomalyType()).isEqualTo("MICRO_ARC");
        verify(feedbackRepository).saveFeedback(any(ScoringFeedback.class));
    }

    @Test
    void shouldComputeAndSaveAdjustmentForConfirmedDefect() {
        var command = new FieldFeedbackCommand(
                UUID.randomUUID().toString(), "tenant-1", UUID.randomUUID(), null,
                FeedbackOutcome.CONFIRMED_DEFECT, "THD_DRIFT", 65.0, "maintenance-service", null);

        when(feedbackRepository.countByOutcome(command.deviceId(), "THD_DRIFT", FeedbackOutcome.CONFIRMED_DEFECT))
                .thenReturn(1);
        when(feedbackRepository.countByOutcome(command.deviceId(), "THD_DRIFT", FeedbackOutcome.FALSE_POSITIVE))
                .thenReturn(0);
        when(feedbackRepository.findAdjustments(command.deviceId(), "THD_DRIFT"))
                .thenReturn(List.of());

        service.process(command);

        var captor = ArgumentCaptor.forClass(ScoringAdjustment.class);
        verify(feedbackRepository).saveAdjustment(captor.capture(), eq(command.deviceId()), eq("tenant-1"));

        ScoringAdjustment adjustment = captor.getValue();
        assertThat(adjustment.type()).isEqualTo(ScoringAdjustment.AdjustmentType.CONFIDENCE_BOOST);
        assertThat(adjustment.mode()).isEqualTo(ScoringAdjustment.AdjustmentMode.SUGGESTION_ONLY);
        assertThat(adjustment.delta()).isGreaterThan(0);
    }

    @Test
    void shouldNotSaveAdjustmentForInconclusive() {
        var command = new FieldFeedbackCommand(
                UUID.randomUUID().toString(), "tenant-1", UUID.randomUUID(), null,
                FeedbackOutcome.INCONCLUSIVE, "TEMPERATURE", 45.0, "maintenance-service", "Needs recheck");

        when(feedbackRepository.countByOutcome(command.deviceId(), "TEMPERATURE", FeedbackOutcome.INCONCLUSIVE))
                .thenReturn(1);
        when(feedbackRepository.countByOutcome(command.deviceId(), "TEMPERATURE", FeedbackOutcome.FALSE_POSITIVE))
                .thenReturn(0);
        when(feedbackRepository.findAdjustments(command.deviceId(), "TEMPERATURE"))
                .thenReturn(List.of());

        service.process(command);

        verify(feedbackRepository, never()).saveAdjustment(any(), any(), any());
    }

    @Test
    void shouldIncrementConfirmedDefectMetric() {
        var command = new FieldFeedbackCommand(
                UUID.randomUUID().toString(), "tenant-1", UUID.randomUUID(), null,
                FeedbackOutcome.CONFIRMED_DEFECT, "MICRO_ARC", 80.0, "maintenance-service", null);

        when(feedbackRepository.countByOutcome(any(), any(), any())).thenReturn(0);
        when(feedbackRepository.findAdjustments(any(), any())).thenReturn(List.of());

        service.process(command);

        double count = registry.counter("pyrosense.scoring.confirmed_defects_total").count();
        assertThat(count).isEqualTo(1.0);
    }

    @Test
    void shouldIncrementFalsePositiveMetric() {
        var command = new FieldFeedbackCommand(
                UUID.randomUUID().toString(), "tenant-1", UUID.randomUUID(), null,
                FeedbackOutcome.FALSE_POSITIVE, "THD_DRIFT", 55.0, "maintenance-service", null);

        when(feedbackRepository.countByOutcome(any(), any(), any())).thenReturn(0);
        when(feedbackRepository.findAdjustments(any(), any())).thenReturn(List.of());

        service.process(command);

        double count = registry.counter("pyrosense.scoring.false_positives_total").count();
        assertThat(count).isEqualTo(1.0);
    }

    @Test
    void shouldApplyDecayWhenMultipleFeedbacksExist() {
        var command = new FieldFeedbackCommand(
                UUID.randomUUID().toString(), "tenant-1", UUID.randomUUID(), null,
                FeedbackOutcome.CONFIRMED_DEFECT, "MICRO_ARC", 70.0, "maintenance-service", null);

        when(feedbackRepository.countByOutcome(command.deviceId(), "MICRO_ARC", FeedbackOutcome.CONFIRMED_DEFECT))
                .thenReturn(3);
        when(feedbackRepository.countByOutcome(command.deviceId(), "MICRO_ARC", FeedbackOutcome.FALSE_POSITIVE))
                .thenReturn(0);

        ScoringAdjustment existingAdj = ScoringAdjustment.confidenceBoost("MICRO_ARC", 0.5, 0.05, "first");
        when(feedbackRepository.findAdjustments(command.deviceId(), "MICRO_ARC"))
                .thenReturn(List.of(existingAdj));

        service.process(command);

        var captor = ArgumentCaptor.forClass(ScoringAdjustment.class);
        verify(feedbackRepository).saveAdjustment(captor.capture(), any(), any());
        assertThat(captor.getValue().delta()).isLessThan(0.05);
    }
}
