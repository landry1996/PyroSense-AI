package com.pyrosense.reporting.domain.model;

import com.pyrosense.shared.id.BuildingId;
import com.pyrosense.shared.id.TenantId;
import com.pyrosense.shared.util.ClockProvider;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

public class Report {

    private static final AtomicLong SEQUENCE = new AtomicLong(System.currentTimeMillis() % 100000);
    private static final Duration DEFAULT_EXPIRY = Duration.ofDays(90);

    private final UUID id;
    private final String reportNumber;
    private final TenantId tenantId;
    private final BuildingId buildingId;
    private final ReportType type;
    private final ReportPeriod period;
    private ReportStatus status;
    private ReportMetadata metadata;
    private ReportSignature signature;
    private ReportFileReference fileReference;
    private byte[] content;
    private String fileName;
    private final Instant createdAt;
    private Instant generatedAt;
    private Instant expiresAt;
    private UUID sourceAlertId;
    private UUID sourceInterventionId;
    private ReportRecipient requestedBy;

    public Report(UUID id, TenantId tenantId, BuildingId buildingId, ReportType type,
                  Instant periodStart, Instant periodEnd) {
        this.id = Objects.requireNonNull(id);
        this.tenantId = Objects.requireNonNull(tenantId);
        this.buildingId = Objects.requireNonNull(buildingId);
        this.type = Objects.requireNonNull(type);
        this.period = new ReportPeriod(periodStart, periodEnd);
        this.status = ReportStatus.REQUESTED;
        this.createdAt = ClockProvider.now();
        this.reportNumber = generateReportNumber(type);
        this.expiresAt = this.createdAt.plus(DEFAULT_EXPIRY);
    }

    public void startGeneration() {
        this.status = ReportStatus.GENERATING;
    }

    public void markGenerated(byte[] content, ReportMetadata metadata) {
        Objects.requireNonNull(content, "content must not be null");
        Objects.requireNonNull(metadata, "metadata must not be null");
        this.content = content;
        this.metadata = metadata;
        this.signature = ReportSignature.compute(content);
        this.status = ReportStatus.GENERATED;
        this.generatedAt = ClockProvider.now();
        this.fileName = buildFileName();
        this.expiresAt = this.generatedAt.plus(DEFAULT_EXPIRY);
    }

    public void markFailed() {
        this.status = ReportStatus.FAILED;
    }

    public void markExpired() {
        this.status = ReportStatus.EXPIRED;
        this.content = null;
    }

    public boolean isExpired() {
        return status == ReportStatus.EXPIRED
                || (expiresAt != null && ClockProvider.now().isAfter(expiresAt) && status == ReportStatus.GENERATED);
    }

    public boolean isAccessibleByInsurer() {
        return type.isInsurerAccessible() && status.isAvailableForDownload() && !isExpired();
    }

    public void setSourceAlertId(UUID alertId) { this.sourceAlertId = alertId; }
    public void setSourceInterventionId(UUID interventionId) { this.sourceInterventionId = interventionId; }
    public void setRequestedBy(ReportRecipient recipient) { this.requestedBy = recipient; }
    public void setFileReference(ReportFileReference ref) { this.fileReference = ref; }

    private String buildFileName() {
        String date = DateTimeFormatter.BASIC_ISO_DATE.format(
                LocalDate.ofInstant(period.end(), java.time.ZoneOffset.UTC));
        return "pyrosense_%s_%s_%s.pdf".formatted(
                type.name().toLowerCase(), reportNumber, date);
    }

    private static String generateReportNumber(ReportType type) {
        String prefix = switch (type) {
            case MONTHLY_HEALTH -> "MH";
            case CONTINUOUS_MONITORING_CERTIFICATE -> "CM";
            case CRITICAL_ALERT_REPORT -> "CA";
            case INTERVENTION_REPORT -> "IR";
            case ROI_AVOIDED_INCIDENTS -> "ROI";
            case INSURER_EXPORT -> "IE";
        };
        return "%s-%s-%05d".formatted(prefix,
                LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMM")),
                SEQUENCE.incrementAndGet() % 100000);
    }

    public UUID getId() { return id; }
    public String getReportNumber() { return reportNumber; }
    public TenantId getTenantId() { return tenantId; }
    public BuildingId getBuildingId() { return buildingId; }
    public ReportType getType() { return type; }
    public ReportPeriod getPeriod() { return period; }
    public Instant getPeriodStart() { return period.start(); }
    public Instant getPeriodEnd() { return period.end(); }
    public ReportStatus getStatus() { return status; }
    public ReportMetadata getMetadata() { return metadata; }
    public ReportSignature getSignature() { return signature; }
    public ReportFileReference getFileReference() { return fileReference; }
    public byte[] getContent() { return content; }
    public String getFileName() { return fileName; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getGeneratedAt() { return generatedAt; }
    public Instant getExpiresAt() { return expiresAt; }
    public UUID getSourceAlertId() { return sourceAlertId; }
    public UUID getSourceInterventionId() { return sourceInterventionId; }
    public ReportRecipient getRequestedBy() { return requestedBy; }
}
