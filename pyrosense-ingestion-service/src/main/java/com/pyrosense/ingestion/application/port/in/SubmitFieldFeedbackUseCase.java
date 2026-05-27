package com.pyrosense.ingestion.application.port.in;

import com.pyrosense.ingestion.domain.model.dataset.DataLabel;
import com.pyrosense.ingestion.domain.model.dataset.TechnicianFeedback;

import java.util.UUID;

public interface SubmitFieldFeedbackUseCase {

    record FieldFeedbackCommand(
            UUID interventionId,
            String technicianId,
            String deviceId,
            String tenantId,
            DataLabel.LabelValue defectObserved,
            double confidenceLevel,
            String visualInspection,
            String measurementMethod,
            String measurementResult,
            boolean defectConfirmed,
            boolean falsePositive,
            String additionalNotes
    ) {}

    TechnicianFeedback execute(FieldFeedbackCommand command);
}
