package com.pyrosense.scoring.application.port.in;

import com.pyrosense.scoring.domain.model.FeedbackOutcome;
import com.pyrosense.scoring.domain.model.ScoringFeedback;

import java.util.UUID;

public interface ProcessFieldFeedbackUseCase {

    record FieldFeedbackCommand(
            String deviceId,
            String tenantId,
            UUID alertId,
            UUID interventionId,
            FeedbackOutcome outcome,
            String anomalyType,
            double riskScoreAtAlert,
            String feedbackSource,
            String comment
    ) {}

    ScoringFeedback process(FieldFeedbackCommand command);
}
