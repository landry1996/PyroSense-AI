package com.pyrosense.ingestion.domain.model.quality;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class DataQualityAssessment {

    public enum QualityGrade {
        A(90, 100, "Données fiables, exploitables pour scoring et ML"),
        B(75, 90, "Données acceptables, exploitables avec précaution"),
        C(50, 75, "Données dégradées, scoring pondéré, ML avec annotation"),
        D(25, 50, "Données insuffisantes, exclues du ML, scoring inhibé"),
        F(0, 25, "Données non exploitables, alertes critiques bloquées");

        private final int minScore;
        private final int maxScore;
        private final String description;

        QualityGrade(int minScore, int maxScore, String description) {
            this.minScore = minScore;
            this.maxScore = maxScore;
            this.description = description;
        }

        public static QualityGrade fromScore(int score) {
            for (var grade : values()) {
                if (score >= grade.minScore && score <= grade.maxScore) return grade;
            }
            return F;
        }

        public String getDescription() { return description; }
        public boolean isTrustworthy() { return this == A || this == B; }
        public boolean allowsCriticalAlerts() { return this != F; }
        public boolean allowsMlTraining() { return this == A || this == B; }
    }

    private final UUID id;
    private final String deviceId;
    private final String tenantId;
    private final Instant assessedAt;
    private final Instant periodStart;
    private final Instant periodEnd;
    private int overallScore;
    private QualityGrade grade;
    private final MissingDataRatio missingDataRatio;
    private final MeasurementRangeStatus rangeStatus;
    private final SensorNoiseLevel noiseLevel;
    private final DataCompleteness completeness;
    private final DeviceCalibrationStatus calibrationStatus;
    private final double averageSignalQuality;
    private final double clockDriftSeconds;
    private final int offlineCount;
    private final String firmwareVersion;
    private final boolean firmwareObsolete;
    private final List<DataQualityIssue> issues;

    private DataQualityAssessment(Builder builder) {
        this.id = UUID.randomUUID();
        this.deviceId = builder.deviceId;
        this.tenantId = builder.tenantId;
        this.assessedAt = Instant.now();
        this.periodStart = builder.periodStart;
        this.periodEnd = builder.periodEnd;
        this.missingDataRatio = builder.missingDataRatio;
        this.rangeStatus = builder.rangeStatus;
        this.noiseLevel = builder.noiseLevel;
        this.completeness = builder.completeness;
        this.calibrationStatus = builder.calibrationStatus;
        this.averageSignalQuality = builder.averageSignalQuality;
        this.clockDriftSeconds = builder.clockDriftSeconds;
        this.offlineCount = builder.offlineCount;
        this.firmwareVersion = builder.firmwareVersion;
        this.firmwareObsolete = builder.firmwareObsolete;
        this.issues = new ArrayList<>();
        computeScore();
    }

    private void computeScore() {
        int score = 100;

        // 1. Missing data (-30 max)
        if (missingDataRatio.isCritical()) {
            score -= 30;
            issues.add(DataQualityIssue.create(deviceId, tenantId,
                    DataQualityIssue.IssueType.MISSING_DATA,
                    DataQualityIssue.IssueSeverity.CRITICAL,
                    "Missing data ratio: %.1f%%".formatted(missingDataRatio.ratio() * 100)));
        } else if (missingDataRatio.isWarning()) {
            score -= 15;
            issues.add(DataQualityIssue.create(deviceId, tenantId,
                    DataQualityIssue.IssueType.MISSING_DATA,
                    DataQualityIssue.IssueSeverity.MEDIUM,
                    "Missing data ratio: %.1f%%".formatted(missingDataRatio.ratio() * 100)));
        }

        // 2. Out of range values (-20 max)
        if (rangeStatus.isCritical()) {
            score -= 20;
            issues.add(DataQualityIssue.create(deviceId, tenantId,
                    DataQualityIssue.IssueType.OUT_OF_RANGE,
                    DataQualityIssue.IssueSeverity.HIGH,
                    "%.1f%% readings out of range: %s".formatted(
                            rangeStatus.outOfRangeRatio() * 100,
                            String.join(", ", rangeStatus.outOfRangeFields()))));
        } else if (rangeStatus.hasIssues()) {
            score -= 10;
            issues.add(DataQualityIssue.create(deviceId, tenantId,
                    DataQualityIssue.IssueType.OUT_OF_RANGE,
                    DataQualityIssue.IssueSeverity.LOW,
                    "%d readings out of range".formatted(rangeStatus.outOfRangeCount())));
        }

        // 3. Clock drift (-15 max)
        if (clockDriftSeconds > 300) {
            score -= 15;
            issues.add(DataQualityIssue.create(deviceId, tenantId,
                    DataQualityIssue.IssueType.TIMESTAMP_INCONSISTENT,
                    DataQualityIssue.IssueSeverity.HIGH,
                    "Clock drift: %.0fs".formatted(clockDriftSeconds)));
        } else if (clockDriftSeconds > 30) {
            score -= 5;
            issues.add(DataQualityIssue.create(deviceId, tenantId,
                    DataQualityIssue.IssueType.TIMESTAMP_INCONSISTENT,
                    DataQualityIssue.IssueSeverity.LOW,
                    "Clock drift: %.0fs".formatted(clockDriftSeconds)));
        }

        // 4. Sequence issues (-10 max)
        if (completeness.hasSequenceIssues()) {
            int penalty = Math.min(10, completeness.sequenceGaps() + completeness.duplicateSequences());
            score -= penalty;
            issues.add(DataQualityIssue.create(deviceId, tenantId,
                    DataQualityIssue.IssueType.SEQUENCE_ANOMALY,
                    penalty >= 5 ? DataQualityIssue.IssueSeverity.MEDIUM : DataQualityIssue.IssueSeverity.LOW,
                    "Gaps: %d, Duplicates: %d".formatted(completeness.sequenceGaps(), completeness.duplicateSequences())));
        }

        // 5. Noise level (-10 max)
        if (noiseLevel.isExcessive()) {
            score -= 10;
            issues.add(DataQualityIssue.create(deviceId, tenantId,
                    DataQualityIssue.IssueType.EXCESSIVE_NOISE,
                    DataQualityIssue.IssueSeverity.MEDIUM,
                    noiseLevel.describe()));
        }

        // 6. Signal quality (-15 max)
        SignalQualityLevel sigLevel = SignalQualityLevel.fromScore(averageSignalQuality);
        if (sigLevel.requiresReview()) {
            score -= 15;
            issues.add(DataQualityIssue.create(deviceId, tenantId,
                    DataQualityIssue.IssueType.LOW_SIGNAL_QUALITY,
                    DataQualityIssue.IssueSeverity.HIGH,
                    "Average signal quality: %.2f (%s)".formatted(averageSignalQuality, sigLevel.name())));
        } else if (!sigLevel.isTrustworthy()) {
            score -= 5;
        }

        // 7. Offline frequency (-10 max)
        if (offlineCount > 5) {
            score -= 10;
            issues.add(DataQualityIssue.create(deviceId, tenantId,
                    DataQualityIssue.IssueType.FREQUENT_OFFLINE,
                    DataQualityIssue.IssueSeverity.MEDIUM,
                    "Device went offline %d times in period".formatted(offlineCount)));
        } else if (offlineCount > 1) {
            score -= 3;
        }

        // 8. Firmware obsolete (-5 max)
        if (firmwareObsolete) {
            score -= 5;
            issues.add(DataQualityIssue.create(deviceId, tenantId,
                    DataQualityIssue.IssueType.OBSOLETE_FIRMWARE,
                    DataQualityIssue.IssueSeverity.LOW,
                    "Firmware %s is outdated".formatted(firmwareVersion)));
        }

        // 9. Calibration unknown (-10 max)
        if (!calibrationStatus.isValid()) {
            score -= 10;
            var severity = calibrationStatus.isExpired() ?
                    DataQualityIssue.IssueSeverity.MEDIUM : DataQualityIssue.IssueSeverity.HIGH;
            issues.add(DataQualityIssue.create(deviceId, tenantId,
                    DataQualityIssue.IssueType.UNKNOWN_CALIBRATION,
                    severity,
                    calibrationStatus.calibrated() ?
                            "Calibration expired" : "No calibration record"));
        }

        this.overallScore = Math.max(0, score);
        this.grade = QualityGrade.fromScore(this.overallScore);
    }

    public UUID getId() { return id; }
    public String getDeviceId() { return deviceId; }
    public String getTenantId() { return tenantId; }
    public Instant getAssessedAt() { return assessedAt; }
    public Instant getPeriodStart() { return periodStart; }
    public Instant getPeriodEnd() { return periodEnd; }
    public int getOverallScore() { return overallScore; }
    public QualityGrade getGrade() { return grade; }
    public MissingDataRatio getMissingDataRatio() { return missingDataRatio; }
    public MeasurementRangeStatus getRangeStatus() { return rangeStatus; }
    public SensorNoiseLevel getNoiseLevel() { return noiseLevel; }
    public DataCompleteness getCompleteness() { return completeness; }
    public DeviceCalibrationStatus getCalibrationStatus() { return calibrationStatus; }
    public double getAverageSignalQuality() { return averageSignalQuality; }
    public double getClockDriftSeconds() { return clockDriftSeconds; }
    public int getOfflineCount() { return offlineCount; }
    public String getFirmwareVersion() { return firmwareVersion; }
    public boolean isFirmwareObsolete() { return firmwareObsolete; }
    public List<DataQualityIssue> getIssues() { return List.copyOf(issues); }
    public boolean isTrustworthy() { return grade.isTrustworthy(); }
    public boolean allowsCriticalAlerts() { return grade.allowsCriticalAlerts(); }
    public boolean allowsMlTraining() { return grade.allowsMlTraining(); }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private String deviceId;
        private String tenantId;
        private Instant periodStart;
        private Instant periodEnd;
        private MissingDataRatio missingDataRatio = MissingDataRatio.compute(0, 0);
        private MeasurementRangeStatus rangeStatus = MeasurementRangeStatus.compute(0, 0, List.of());
        private SensorNoiseLevel noiseLevel = new SensorNoiseLevel(0, 0, 0, 0);
        private DataCompleteness completeness = new DataCompleteness(java.time.Duration.ofHours(24), 0, 0, 0, 0, 0, 0);
        private DeviceCalibrationStatus calibrationStatus = DeviceCalibrationStatus.unknown();
        private double averageSignalQuality = 1.0;
        private double clockDriftSeconds = 0;
        private int offlineCount = 0;
        private String firmwareVersion = "unknown";
        private boolean firmwareObsolete = false;

        public Builder deviceId(String deviceId) { this.deviceId = deviceId; return this; }
        public Builder tenantId(String tenantId) { this.tenantId = tenantId; return this; }
        public Builder periodStart(Instant periodStart) { this.periodStart = periodStart; return this; }
        public Builder periodEnd(Instant periodEnd) { this.periodEnd = periodEnd; return this; }
        public Builder missingDataRatio(MissingDataRatio r) { this.missingDataRatio = r; return this; }
        public Builder rangeStatus(MeasurementRangeStatus s) { this.rangeStatus = s; return this; }
        public Builder noiseLevel(SensorNoiseLevel n) { this.noiseLevel = n; return this; }
        public Builder completeness(DataCompleteness c) { this.completeness = c; return this; }
        public Builder calibrationStatus(DeviceCalibrationStatus s) { this.calibrationStatus = s; return this; }
        public Builder averageSignalQuality(double q) { this.averageSignalQuality = q; return this; }
        public Builder clockDriftSeconds(double d) { this.clockDriftSeconds = d; return this; }
        public Builder offlineCount(int c) { this.offlineCount = c; return this; }
        public Builder firmwareVersion(String v) { this.firmwareVersion = v; return this; }
        public Builder firmwareObsolete(boolean o) { this.firmwareObsolete = o; return this; }

        public DataQualityAssessment build() {
            return new DataQualityAssessment(this);
        }
    }
}
