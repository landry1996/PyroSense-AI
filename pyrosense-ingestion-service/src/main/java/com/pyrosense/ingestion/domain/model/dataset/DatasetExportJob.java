package com.pyrosense.ingestion.domain.model.dataset;

import java.time.Instant;
import java.util.UUID;

public class DatasetExportJob {

    public enum ExportStatus {
        PENDING,
        RUNNING,
        COMPLETED,
        FAILED
    }

    public enum ExportFormat {
        PARQUET,
        CSV,
        JSON_LINES
    }

    private final UUID id;
    private final String requestedBy;
    private final String pseudonymizedTenantId;
    private ExportStatus status;
    private final ExportFormat format;
    private final Instant windowStart;
    private final Instant windowEnd;
    private final DataLabel.LabelValue labelFilter;
    private final DatasetCandidate.DataQualityTier minQualityTier;
    private final boolean includeUnlabeled;
    private final Instant requestedAt;
    private Instant completedAt;
    private int candidateCount;
    private long fileSizeBytes;
    private String outputPath;
    private String failureReason;

    public DatasetExportJob(String requestedBy, String pseudonymizedTenantId,
                            ExportFormat format, Instant windowStart, Instant windowEnd,
                            DataLabel.LabelValue labelFilter,
                            DatasetCandidate.DataQualityTier minQualityTier,
                            boolean includeUnlabeled) {
        this.id = UUID.randomUUID();
        this.requestedBy = requestedBy;
        this.pseudonymizedTenantId = pseudonymizedTenantId;
        this.status = ExportStatus.PENDING;
        this.format = format;
        this.windowStart = windowStart;
        this.windowEnd = windowEnd;
        this.labelFilter = labelFilter;
        this.minQualityTier = minQualityTier;
        this.includeUnlabeled = includeUnlabeled;
        this.requestedAt = Instant.now();
    }

    public void markRunning() {
        this.status = ExportStatus.RUNNING;
    }

    public void markCompleted(int candidateCount, long fileSizeBytes, String outputPath) {
        this.status = ExportStatus.COMPLETED;
        this.completedAt = Instant.now();
        this.candidateCount = candidateCount;
        this.fileSizeBytes = fileSizeBytes;
        this.outputPath = outputPath;
    }

    public void markFailed(String reason) {
        this.status = ExportStatus.FAILED;
        this.completedAt = Instant.now();
        this.failureReason = reason;
    }

    public UUID getId() { return id; }
    public String getRequestedBy() { return requestedBy; }
    public String getPseudonymizedTenantId() { return pseudonymizedTenantId; }
    public ExportStatus getStatus() { return status; }
    public ExportFormat getFormat() { return format; }
    public Instant getWindowStart() { return windowStart; }
    public Instant getWindowEnd() { return windowEnd; }
    public DataLabel.LabelValue getLabelFilter() { return labelFilter; }
    public DatasetCandidate.DataQualityTier getMinQualityTier() { return minQualityTier; }
    public boolean isIncludeUnlabeled() { return includeUnlabeled; }
    public Instant getRequestedAt() { return requestedAt; }
    public Instant getCompletedAt() { return completedAt; }
    public int getCandidateCount() { return candidateCount; }
    public long getFileSizeBytes() { return fileSizeBytes; }
    public String getOutputPath() { return outputPath; }
    public String getFailureReason() { return failureReason; }
}
