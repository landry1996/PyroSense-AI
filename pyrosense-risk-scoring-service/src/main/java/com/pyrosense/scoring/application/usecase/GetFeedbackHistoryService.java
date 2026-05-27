package com.pyrosense.scoring.application.usecase;

import com.pyrosense.scoring.application.port.in.GetFeedbackHistoryQuery;
import com.pyrosense.scoring.application.port.out.ScoringFeedbackRepositoryPort;
import com.pyrosense.scoring.domain.model.FeedbackOutcome;
import com.pyrosense.scoring.domain.model.ScoringAdjustment;

import java.util.List;

public class GetFeedbackHistoryService implements GetFeedbackHistoryQuery {

    private final ScoringFeedbackRepositoryPort repository;

    public GetFeedbackHistoryService(ScoringFeedbackRepositoryPort repository) {
        this.repository = repository;
    }

    @Override
    public FeedbackPageResult getHistory(FeedbackFilter filter) {
        int limit = Math.min(filter.size(), 100);
        int offset = filter.page() * limit;

        var feedbacks = repository.findByTenantAndDevice(
                filter.tenantId(), filter.deviceId(),
                filter.from(), filter.to(), offset, limit);

        int totalCount = repository.countByTenantAndDevice(
                filter.tenantId(), filter.deviceId(),
                filter.from(), filter.to());

        int totalPages = (int) Math.ceil((double) totalCount / limit);
        return new FeedbackPageResult(feedbacks, totalCount, filter.page(), totalPages);
    }

    @Override
    public FeedbackStats getStats(String tenantId, String deviceId) {
        int confirmed = repository.countByOutcomeForTenant(tenantId, deviceId, FeedbackOutcome.CONFIRMED_DEFECT);
        int falsePositives = repository.countByOutcomeForTenant(tenantId, deviceId, FeedbackOutcome.FALSE_POSITIVE);
        int inconclusive = repository.countByOutcomeForTenant(tenantId, deviceId, FeedbackOutcome.INCONCLUSIVE);
        int noDefect = repository.countByOutcomeForTenant(tenantId, deviceId, FeedbackOutcome.NO_DEFECT_FOUND);

        var adjustments = deviceId != null
                ? repository.findAdjustments(deviceId, null)
                : List.<ScoringAdjustment>of();
        int applied = (int) adjustments.stream().filter(ScoringAdjustment::isApplied).count();
        int suggested = (int) adjustments.stream().filter(ScoringAdjustment::isSuggestionOnly).count();

        return new FeedbackStats(confirmed, falsePositives, inconclusive, noDefect, applied, suggested);
    }

    @Override
    public List<ScoringAdjustment> getAdjustmentHistory(String deviceId, String anomalyType) {
        return repository.findAdjustments(deviceId, anomalyType);
    }
}
