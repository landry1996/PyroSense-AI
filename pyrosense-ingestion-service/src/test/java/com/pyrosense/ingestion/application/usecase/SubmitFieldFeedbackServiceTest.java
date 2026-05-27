package com.pyrosense.ingestion.application.usecase;

import com.pyrosense.ingestion.application.port.in.SubmitFieldFeedbackUseCase;
import com.pyrosense.ingestion.application.port.out.DatasetAuditPort;
import com.pyrosense.ingestion.application.port.out.DatasetRepositoryPort;
import com.pyrosense.ingestion.domain.model.dataset.DataLabel;
import com.pyrosense.ingestion.domain.model.dataset.PseudonymizationService;
import com.pyrosense.ingestion.domain.model.dataset.TechnicianFeedback;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class SubmitFieldFeedbackServiceTest {

    private DatasetRepositoryPort repository;
    private DatasetAuditPort auditPort;
    private SubmitFieldFeedbackService service;

    @BeforeEach
    void setUp() {
        repository = mock(DatasetRepositoryPort.class);
        auditPort = mock(DatasetAuditPort.class);
        var pseudonymization = new PseudonymizationService("test-key");
        service = new SubmitFieldFeedbackService(repository, auditPort, pseudonymization);
    }

    @Test
    @DisplayName("submit feedback persists with pseudonymized device id")
    void submitFeedback() {
        var command = new SubmitFieldFeedbackUseCase.FieldFeedbackCommand(
                UUID.randomUUID(), "electrician-1", "device-001", "tenant-001",
                DataLabel.LabelValue.MICRO_ARC_CONFIRMED, 0.9,
                "Visible arc marks on conductor", "Thermal camera",
                "Temperature delta 15K", true, false, "Confirmed defect");

        var result = service.execute(command);

        assertThat(result.technicianId()).isEqualTo("electrician-1");
        assertThat(result.defectObserved()).isEqualTo(DataLabel.LabelValue.MICRO_ARC_CONFIRMED);
        assertThat(result.defectConfirmed()).isTrue();
        assertThat(result.pseudonymizedDeviceId()).startsWith("ps_");
        assertThat(result.pseudonymizedDeviceId()).isNotEqualTo("device-001");
        verify(repository).saveFeedback(any(TechnicianFeedback.class));
    }

    @Test
    @DisplayName("submit feedback audits the action")
    void auditsFeedback() {
        UUID interventionId = UUID.randomUUID();
        var command = new SubmitFieldFeedbackUseCase.FieldFeedbackCommand(
                interventionId, "tech-2", "device-002", "tenant-001",
                DataLabel.LabelValue.FALSE_POSITIVE, 1.0,
                null, null, null, false, true, "No defect found");

        service.execute(command);

        verify(auditPort).logFeedbackSubmitted("tenant-001", "tech-2", interventionId, "FALSE_POSITIVE");
    }

    @Test
    @DisplayName("false positive feedback creates correct label via toLabel()")
    void falsePositiveLabel() {
        var command = new SubmitFieldFeedbackUseCase.FieldFeedbackCommand(
                UUID.randomUUID(), "tech-1", "device-001", "tenant-001",
                DataLabel.LabelValue.NORMAL, 0.95,
                null, null, null, false, true, "False positive - noise");

        var result = service.execute(command);
        var label = result.toLabel();

        assertThat(label.value()).isEqualTo(DataLabel.LabelValue.FALSE_POSITIVE);
        assertThat(label.source()).isEqualTo(DataLabel.LabelSource.TECHNICIAN);
    }

    @Test
    @DisplayName("defect confirmation feedback creates correct label")
    void defectConfirmationLabel() {
        var command = new SubmitFieldFeedbackUseCase.FieldFeedbackCommand(
                UUID.randomUUID(), "tech-1", "device-001", "tenant-001",
                DataLabel.LabelValue.LOOSE_CONNECTION_CONFIRMED, 0.9,
                "Loose terminal screw", "Torque wrench", "0.3 Nm (spec: 1.2 Nm)",
                true, false, null);

        var result = service.execute(command);
        var label = result.toLabel();

        assertThat(label.value()).isEqualTo(DataLabel.LabelValue.LOOSE_CONNECTION_CONFIRMED);
        assertThat(label.confidence()).isEqualTo(0.9);
    }
}
