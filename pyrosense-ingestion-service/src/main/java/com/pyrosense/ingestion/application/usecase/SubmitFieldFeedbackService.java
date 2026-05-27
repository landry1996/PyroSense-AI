package com.pyrosense.ingestion.application.usecase;

import com.pyrosense.ingestion.application.port.in.SubmitFieldFeedbackUseCase;
import com.pyrosense.ingestion.application.port.out.DatasetAuditPort;
import com.pyrosense.ingestion.application.port.out.DatasetRepositoryPort;
import com.pyrosense.ingestion.domain.model.dataset.PseudonymizationService;
import com.pyrosense.ingestion.domain.model.dataset.TechnicianFeedback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SubmitFieldFeedbackService implements SubmitFieldFeedbackUseCase {

    private static final Logger log = LoggerFactory.getLogger(SubmitFieldFeedbackService.class);

    private final DatasetRepositoryPort repository;
    private final DatasetAuditPort auditPort;
    private final PseudonymizationService pseudonymization;

    public SubmitFieldFeedbackService(DatasetRepositoryPort repository,
                                       DatasetAuditPort auditPort,
                                       PseudonymizationService pseudonymization) {
        this.repository = repository;
        this.auditPort = auditPort;
        this.pseudonymization = pseudonymization;
    }

    @Override
    public TechnicianFeedback execute(FieldFeedbackCommand command) {
        String pseudonymizedDeviceId = pseudonymization.pseudonymizeDevice(command.deviceId());

        var feedback = TechnicianFeedback.create(
                command.interventionId(),
                command.technicianId(),
                pseudonymizedDeviceId,
                command.defectObserved(),
                command.confidenceLevel(),
                command.visualInspection(),
                command.measurementMethod(),
                command.measurementResult(),
                command.defectConfirmed(),
                command.falsePositive(),
                command.additionalNotes());

        repository.saveFeedback(feedback);

        auditPort.logFeedbackSubmitted(
                command.tenantId(),
                command.technicianId(),
                command.interventionId(),
                command.defectObserved().name());

        log.info("Field feedback submitted: intervention={} technician={} defect={} confirmed={}",
                command.interventionId(), command.technicianId(),
                command.defectObserved(), command.defectConfirmed());

        return feedback;
    }
}
