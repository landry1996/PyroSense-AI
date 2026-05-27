package com.pyrosense.scoring.application.port.in;

import com.pyrosense.scoring.domain.model.ScoringAdjustment;
import com.pyrosense.scoring.domain.model.ScoringFeedback;

import java.time.Instant;
import java.util.List;

public interface GetFeedbackHistoryQuery {

    record FeedbackFilter(
            String tenantId,
            String deviceId,
            String anomalyType,
            Instant from,
            Instant to,
            int page,
            int size
    ) {}

    record FeedbackPageResult(
            List<ScoringFeedback> feedbacks,
            int totalCount,
            int page,
            int totalPages
    ) {}

    record FeedbackStats(
            int confirmedDefects,
            int falsePositives,
            int inconclusive,
            int noDefectFound,
            int adjustmentsApplied,
            int adjustmentsSuggested
    ) {}

    FeedbackPageResult getHistory(FeedbackFilter filter);

    FeedbackStats getStats(String tenantId, String deviceId);

    List<ScoringAdjustment> getAdjustmentHistory(String deviceId, String anomalyType);
}
