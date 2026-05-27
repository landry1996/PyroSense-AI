package com.pyrosense.scoring.application.port.out;

import com.pyrosense.scoring.domain.model.FeedbackOutcome;
import com.pyrosense.scoring.domain.model.ScoringAdjustment;
import com.pyrosense.scoring.domain.model.ScoringFeedback;

import java.time.Instant;
import java.util.List;

public interface ScoringFeedbackRepositoryPort {

    void saveFeedback(ScoringFeedback feedback);

    void saveAdjustment(ScoringAdjustment adjustment, String deviceId, String tenantId);

    List<ScoringFeedback> findByTenantAndDevice(String tenantId, String deviceId,
                                                  Instant from, Instant to,
                                                  int offset, int limit);

    int countByTenantAndDevice(String tenantId, String deviceId, Instant from, Instant to);

    int countByOutcome(String deviceId, String anomalyType, FeedbackOutcome outcome);

    List<ScoringAdjustment> findAdjustments(String deviceId, String anomalyType);

    int countByOutcomeForTenant(String tenantId, String deviceId, FeedbackOutcome outcome);
}
