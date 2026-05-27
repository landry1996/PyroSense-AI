package com.pyrosense.ingestion.domain.model.quality;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

class DataQualityAssessmentTest {

    private static final String DEVICE_ID = "device-001";
    private static final String TENANT_ID = "tenant-001";
    private static final Instant PERIOD_START = Instant.parse("2026-05-26T00:00:00Z");
    private static final Instant PERIOD_END = Instant.parse("2026-05-27T00:00:00Z");

    @Nested
    @DisplayName("Out of range values")
    class OutOfRangeTests {

        @Test
        @DisplayName("critical out of range (>10%) penalizes by 20 points")
        void criticalOutOfRange() {
            var assessment = baseBuilder()
                    .rangeStatus(MeasurementRangeStatus.compute(100, 15, List.of("voltage", "temperature")))
                    .build();

            assertThat(assessment.getOverallScore()).isEqualTo(80);
            assertThat(assessment.getIssues()).anyMatch(i ->
                    i.getType() == DataQualityIssue.IssueType.OUT_OF_RANGE
                            && i.getSeverity() == DataQualityIssue.IssueSeverity.HIGH);
        }

        @Test
        @DisplayName("minor out of range (<=10%) penalizes by 10 points")
        void minorOutOfRange() {
            var assessment = baseBuilder()
                    .rangeStatus(MeasurementRangeStatus.compute(100, 5, List.of("current")))
                    .build();

            assertThat(assessment.getOverallScore()).isEqualTo(90);
            assertThat(assessment.getIssues()).anyMatch(i ->
                    i.getType() == DataQualityIssue.IssueType.OUT_OF_RANGE
                            && i.getSeverity() == DataQualityIssue.IssueSeverity.LOW);
        }
    }

    @Nested
    @DisplayName("Timestamp future / too old (clock drift)")
    class TimestampTests {

        @Test
        @DisplayName("severe clock drift (>300s) penalizes by 15 points")
        void severeClockDrift() {
            var assessment = baseBuilder()
                    .clockDriftSeconds(600.0)
                    .build();

            assertThat(assessment.getOverallScore()).isEqualTo(85);
            assertThat(assessment.getIssues()).anyMatch(i ->
                    i.getType() == DataQualityIssue.IssueType.TIMESTAMP_INCONSISTENT
                            && i.getSeverity() == DataQualityIssue.IssueSeverity.HIGH
                            && i.getDetails().contains("600"));
        }

        @Test
        @DisplayName("moderate clock drift (30-300s) penalizes by 5 points")
        void moderateClockDrift() {
            var assessment = baseBuilder()
                    .clockDriftSeconds(60.0)
                    .build();

            assertThat(assessment.getOverallScore()).isEqualTo(95);
            assertThat(assessment.getIssues()).anyMatch(i ->
                    i.getType() == DataQualityIssue.IssueType.TIMESTAMP_INCONSISTENT
                            && i.getSeverity() == DataQualityIssue.IssueSeverity.LOW);
        }

        @Test
        @DisplayName("no clock drift produces no timestamp issue")
        void noClockDrift() {
            var assessment = baseBuilder()
                    .clockDriftSeconds(5.0)
                    .build();

            assertThat(assessment.getIssues()).noneMatch(i ->
                    i.getType() == DataQualityIssue.IssueType.TIMESTAMP_INCONSISTENT);
        }
    }

    @Nested
    @DisplayName("Sequence number duplicates/gaps")
    class SequenceTests {

        @Test
        @DisplayName("sequence gaps and duplicates create SEQUENCE_ANOMALY issue")
        void sequenceAnomaly() {
            var assessment = baseBuilder()
                    .completeness(new DataCompleteness(
                            Duration.ofHours(24), 1440, 1440, 8640, 8640, 5, 3))
                    .build();

            assertThat(assessment.getIssues()).anyMatch(i ->
                    i.getType() == DataQualityIssue.IssueType.SEQUENCE_ANOMALY
                            && i.getDetails().contains("Gaps: 5")
                            && i.getDetails().contains("Duplicates: 3"));
            assertThat(assessment.getOverallScore()).isLessThan(100);
        }

        @Test
        @DisplayName("large sequence issues penalize up to 10 points")
        void largeSequenceIssues() {
            var assessment = baseBuilder()
                    .completeness(new DataCompleteness(
                            Duration.ofHours(24), 1440, 1440, 8640, 8640, 7, 5))
                    .build();

            assertThat(assessment.getOverallScore()).isEqualTo(90);
        }

        @Test
        @DisplayName("no sequence issues produces no SEQUENCE_ANOMALY")
        void noSequenceIssues() {
            var assessment = baseBuilder().build();

            assertThat(assessment.getIssues()).noneMatch(i ->
                    i.getType() == DataQualityIssue.IssueType.SEQUENCE_ANOMALY);
        }
    }

    @Nested
    @DisplayName("Missing data")
    class MissingDataTests {

        @Test
        @DisplayName("critical missing data (>20%) penalizes by 30 points")
        void criticalMissingData() {
            var assessment = baseBuilder()
                    .missingDataRatio(MissingDataRatio.compute(1000, 700))
                    .build();

            assertThat(assessment.getOverallScore()).isEqualTo(70);
            assertThat(assessment.getGrade()).isEqualTo(DataQualityAssessment.QualityGrade.C);
            assertThat(assessment.getIssues()).anyMatch(i ->
                    i.getType() == DataQualityIssue.IssueType.MISSING_DATA
                            && i.getSeverity() == DataQualityIssue.IssueSeverity.CRITICAL);
        }

        @Test
        @DisplayName("warning missing data (5-20%) penalizes by 15 points")
        void warningMissingData() {
            var assessment = baseBuilder()
                    .missingDataRatio(MissingDataRatio.compute(1000, 850))
                    .build();

            assertThat(assessment.getOverallScore()).isEqualTo(85);
            assertThat(assessment.getIssues()).anyMatch(i ->
                    i.getType() == DataQualityIssue.IssueType.MISSING_DATA
                            && i.getSeverity() == DataQualityIssue.IssueSeverity.MEDIUM);
        }

        @Test
        @DisplayName("acceptable missing data (<5%) produces no issue")
        void acceptableMissingData() {
            var assessment = baseBuilder()
                    .missingDataRatio(MissingDataRatio.compute(1000, 970))
                    .build();

            assertThat(assessment.getIssues()).noneMatch(i ->
                    i.getType() == DataQualityIssue.IssueType.MISSING_DATA);
        }
    }

    @Nested
    @DisplayName("Low signal quality")
    class SignalQualityTests {

        @Test
        @DisplayName("poor signal quality penalizes by 15 points")
        void poorSignalQuality() {
            var assessment = baseBuilder()
                    .averageSignalQuality(0.2)
                    .build();

            assertThat(assessment.getOverallScore()).isEqualTo(85);
            assertThat(assessment.getIssues()).anyMatch(i ->
                    i.getType() == DataQualityIssue.IssueType.LOW_SIGNAL_QUALITY
                            && i.getSeverity() == DataQualityIssue.IssueSeverity.HIGH);
        }

        @Test
        @DisplayName("degraded signal quality penalizes by 5 points")
        void degradedSignalQuality() {
            var assessment = baseBuilder()
                    .averageSignalQuality(0.55)
                    .build();

            assertThat(assessment.getOverallScore()).isEqualTo(95);
            assertThat(assessment.getIssues()).noneMatch(i ->
                    i.getType() == DataQualityIssue.IssueType.LOW_SIGNAL_QUALITY);
        }

        @Test
        @DisplayName("good signal quality produces no issue")
        void goodSignalQuality() {
            var assessment = baseBuilder()
                    .averageSignalQuality(0.85)
                    .build();

            assertThat(assessment.getIssues()).noneMatch(i ->
                    i.getType() == DataQualityIssue.IssueType.LOW_SIGNAL_QUALITY);
            assertThat(assessment.getOverallScore()).isEqualTo(100);
        }
    }

    @Nested
    @DisplayName("Device offline")
    class DeviceOfflineTests {

        @Test
        @DisplayName("frequent offline (>5) penalizes by 10 points")
        void frequentOffline() {
            var assessment = baseBuilder()
                    .offlineCount(8)
                    .build();

            assertThat(assessment.getOverallScore()).isEqualTo(90);
            assertThat(assessment.getIssues()).anyMatch(i ->
                    i.getType() == DataQualityIssue.IssueType.FREQUENT_OFFLINE
                            && i.getDetails().contains("8 times"));
        }

        @Test
        @DisplayName("occasional offline (2-5) penalizes by 3 points")
        void occasionalOffline() {
            var assessment = baseBuilder()
                    .offlineCount(3)
                    .build();

            assertThat(assessment.getOverallScore()).isEqualTo(97);
            assertThat(assessment.getIssues()).noneMatch(i ->
                    i.getType() == DataQualityIssue.IssueType.FREQUENT_OFFLINE);
        }
    }

    @Nested
    @DisplayName("Grading and business rules")
    class GradingTests {

        @Test
        @DisplayName("perfect data yields Grade A and is trustworthy")
        void gradeA() {
            var assessment = baseBuilder().build();

            assertThat(assessment.getOverallScore()).isEqualTo(100);
            assertThat(assessment.getGrade()).isEqualTo(DataQualityAssessment.QualityGrade.A);
            assertThat(assessment.isTrustworthy()).isTrue();
            assertThat(assessment.allowsCriticalAlerts()).isTrue();
            assertThat(assessment.allowsMlTraining()).isTrue();
        }

        @Test
        @DisplayName("Grade F blocks critical alerts")
        void gradeFBlocksAlerts() {
            var assessment = baseBuilder()
                    .missingDataRatio(MissingDataRatio.compute(1000, 500))
                    .rangeStatus(MeasurementRangeStatus.compute(100, 15, List.of("voltage")))
                    .clockDriftSeconds(600)
                    .averageSignalQuality(0.1)
                    .offlineCount(10)
                    .firmwareObsolete(true)
                    .firmwareVersion("0.5.0")
                    .calibrationStatus(DeviceCalibrationStatus.unknown())
                    .build();

            assertThat(assessment.getGrade()).isEqualTo(DataQualityAssessment.QualityGrade.F);
            assertThat(assessment.allowsCriticalAlerts()).isFalse();
            assertThat(assessment.allowsMlTraining()).isFalse();
            assertThat(assessment.isTrustworthy()).isFalse();
        }

        @Test
        @DisplayName("Grade C/D excludes ML training but allows alerts")
        void gradeCDExcludesMl() {
            var assessment = baseBuilder()
                    .missingDataRatio(MissingDataRatio.compute(1000, 700))
                    .clockDriftSeconds(60)
                    .offlineCount(3)
                    .build();

            assertThat(assessment.getGrade()).isIn(
                    DataQualityAssessment.QualityGrade.C,
                    DataQualityAssessment.QualityGrade.D);
            assertThat(assessment.allowsCriticalAlerts()).isTrue();
            assertThat(assessment.allowsMlTraining()).isFalse();
        }

        @Test
        @DisplayName("multiple issues accumulate penalties")
        void multipleIssuesAccumulate() {
            var assessment = baseBuilder()
                    .missingDataRatio(MissingDataRatio.compute(1000, 850))
                    .rangeStatus(MeasurementRangeStatus.compute(100, 15, List.of("voltage")))
                    .clockDriftSeconds(600)
                    .averageSignalQuality(0.2)
                    .offlineCount(8)
                    .firmwareObsolete(true)
                    .firmwareVersion("0.9.0")
                    .build();

            assertThat(assessment.getOverallScore()).isLessThan(30);
            assertThat(assessment.getGrade()).isEqualTo(DataQualityAssessment.QualityGrade.F);
            assertThat(assessment.getIssues()).hasSizeGreaterThanOrEqualTo(5);
        }
    }

    @Nested
    @DisplayName("Noise level")
    class NoiseLevelTests {

        @Test
        @DisplayName("excessive noise penalizes by 10 points")
        void excessiveNoise() {
            var assessment = baseBuilder()
                    .noiseLevel(new SensorNoiseLevel(0.7, 0.9, 0.3, 1.5))
                    .build();

            assertThat(assessment.getOverallScore()).isEqualTo(90);
            assertThat(assessment.getIssues()).anyMatch(i ->
                    i.getType() == DataQualityIssue.IssueType.EXCESSIVE_NOISE);
        }

        @Test
        @DisplayName("normal noise level produces no issue")
        void normalNoise() {
            var assessment = baseBuilder()
                    .noiseLevel(new SensorNoiseLevel(0.3, 0.4, 0.2, 1.0))
                    .build();

            assertThat(assessment.getIssues()).noneMatch(i ->
                    i.getType() == DataQualityIssue.IssueType.EXCESSIVE_NOISE);
        }
    }

    @Nested
    @DisplayName("Firmware and calibration")
    class FirmwareCalibrationTests {

        @Test
        @DisplayName("obsolete firmware penalizes by 5 points")
        void obsoleteFirmware() {
            var assessment = baseBuilder()
                    .firmwareObsolete(true)
                    .firmwareVersion("0.5.0")
                    .build();

            assertThat(assessment.getOverallScore()).isEqualTo(95);
            assertThat(assessment.getIssues()).anyMatch(i ->
                    i.getType() == DataQualityIssue.IssueType.OBSOLETE_FIRMWARE);
        }

        @Test
        @DisplayName("unknown calibration penalizes by 10 points")
        void unknownCalibration() {
            var assessment = baseBuilder()
                    .calibrationStatus(DeviceCalibrationStatus.unknown())
                    .build();

            assertThat(assessment.getOverallScore()).isEqualTo(90);
            assertThat(assessment.getIssues()).anyMatch(i ->
                    i.getType() == DataQualityIssue.IssueType.UNKNOWN_CALIBRATION
                            && i.getSeverity() == DataQualityIssue.IssueSeverity.MEDIUM);
        }
    }

    private DataQualityAssessment.Builder baseBuilder() {
        return DataQualityAssessment.builder()
                .deviceId(DEVICE_ID)
                .tenantId(TENANT_ID)
                .periodStart(PERIOD_START)
                .periodEnd(PERIOD_END)
                .missingDataRatio(MissingDataRatio.compute(1000, 1000))
                .rangeStatus(MeasurementRangeStatus.compute(1000, 0, List.of()))
                .noiseLevel(new SensorNoiseLevel(0.2, 0.3, 0.1, 0.5))
                .completeness(new DataCompleteness(Duration.ofHours(24), 1440, 1440, 8640, 8640, 0, 0))
                .calibrationStatus(DeviceCalibrationStatus.calibrated(PERIOD_START, "factory"))
                .averageSignalQuality(0.95)
                .clockDriftSeconds(0)
                .offlineCount(0)
                .firmwareVersion("1.2.0")
                .firmwareObsolete(false);
    }
}
