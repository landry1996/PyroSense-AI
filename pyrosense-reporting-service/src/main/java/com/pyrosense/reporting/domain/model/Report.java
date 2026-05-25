package com.pyrosense.reporting.domain.model;

import com.pyrosense.shared.id.BuildingId;
import com.pyrosense.shared.id.TenantId;
import com.pyrosense.shared.util.ClockProvider;

import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

public class Report {

    private static final AtomicLong SEQUENCE = new AtomicLong(System.currentTimeMillis() % 100000);

    private final UUID id;
    private final String reportNumber;
    private final TenantId tenantId;
    private final BuildingId buildingId;
    private final ReportType type;
    private final Instant periodStart;
    private final Instant periodEnd;
    private ReportStatus status;
    private ReportMetadata metadata;
    private ReportSignature signature;
    private byte[] content;
    private String fileName;
    private final Instant createdAt;
    private Instant generatedAt;

    public Report(UUID id, TenantId tenantId, BuildingId buildingId, ReportType type,
                  Instant periodStart, Instant periodEnd) {
        this.id = Objects.requireNonNull(id);
        this.tenantId = Objects.requireNonNull(tenantId);
        this.buildingId = Objects.requireNonNull(buildingId);
        this.type = Objects.requireNonNull(type);
        this.periodStart = Objects.requireNonNull(periodStart);
        this.periodEnd = Objects.requireNonNull(periodEnd);
        this.status = ReportStatus.PENDING;
        this.createdAt = ClockProvider.now();
        this.reportNumber = generateReportNumber(type);
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
    }

    public void markFailed() {
        this.status = ReportStatus.FAILED;
    }

    public boolean isAccessibleByInsurer() {
        return type.isInsurerAccessible() && status.isAvailableForDownload();
    }

    private String buildFileName() {
        String date = DateTimeFormatter.BASIC_ISO_DATE.format(
                LocalDate.ofInstant(periodEnd, java.time.ZoneOffset.UTC));
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

    // Getters
    public UUID getId() { return id; }
    public String getReportNumber() { return reportNumber; }
    public TenantId getTenantId() { return tenantId; }
    public BuildingId getBuildingId() { return buildingId; }
    public ReportType getType() { return type; }
    public Instant getPeriodStart() { return periodStart; }
    public Instant getPeriodEnd() { return periodEnd; }
    public ReportStatus getStatus() { return status; }
    public ReportMetadata getMetadata() { return metadata; }
    public ReportSignature getSignature() { return signature; }
    public byte[] getContent() { return content; }
    public String getFileName() { return fileName; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getGeneratedAt() { return generatedAt; }
}
