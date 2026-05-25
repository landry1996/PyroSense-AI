package com.pyrosense.reporting.domain;

import com.pyrosense.reporting.domain.model.*;
import com.pyrosense.shared.id.BuildingId;
import com.pyrosense.shared.id.TenantId;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ReportTest {

    private static final TenantId TENANT = new TenantId(UUID.randomUUID());
    private static final BuildingId BUILDING = new BuildingId(UUID.randomUUID());
    private static final Instant PERIOD_START = Instant.parse("2025-01-01T00:00:00Z");
    private static final Instant PERIOD_END = Instant.parse("2025-01-31T23:59:59Z");

    @Test
    void shouldCreateReportWithPendingStatus() {
        Report report = new Report(UUID.randomUUID(), TENANT, BUILDING,
                ReportType.MONTHLY_HEALTH, PERIOD_START, PERIOD_END);

        assertEquals(ReportStatus.PENDING, report.getStatus());
        assertNotNull(report.getReportNumber());
        assertTrue(report.getReportNumber().startsWith("MH-"));
        assertNotNull(report.getCreatedAt());
    }

    @Test
    void shouldTransitionToGenerating() {
        Report report = createReport();
        report.startGeneration();

        assertEquals(ReportStatus.GENERATING, report.getStatus());
    }

    @Test
    void shouldTransitionToGenerated() {
        Report report = createReport();
        report.startGeneration();

        byte[] content = "PDF content".getBytes();
        ReportMetadata metadata = createMetadata();

        report.markGenerated(content, metadata);

        assertEquals(ReportStatus.GENERATED, report.getStatus());
        assertArrayEquals(content, report.getContent());
        assertEquals(metadata, report.getMetadata());
        assertNotNull(report.getSignature());
        assertNotNull(report.getFileName());
        assertNotNull(report.getGeneratedAt());
    }

    @Test
    void shouldTransitionToFailed() {
        Report report = createReport();
        report.startGeneration();
        report.markFailed();

        assertEquals(ReportStatus.FAILED, report.getStatus());
    }

    @Test
    void shouldComputeSignatureOnGeneration() {
        Report report = createReport();
        report.startGeneration();

        byte[] content = "test content for hash".getBytes();
        report.markGenerated(content, createMetadata());

        assertNotNull(report.getSignature());
        assertEquals("SHA-256", report.getSignature().algorithm());
        assertNotNull(report.getSignature().hash());
        assertFalse(report.getSignature().hash().isEmpty());
    }

    @Test
    void shouldGenerateUniqueReportNumbers() {
        Report r1 = new Report(UUID.randomUUID(), TENANT, BUILDING,
                ReportType.MONTHLY_HEALTH, PERIOD_START, PERIOD_END);
        Report r2 = new Report(UUID.randomUUID(), TENANT, BUILDING,
                ReportType.MONTHLY_HEALTH, PERIOD_START, PERIOD_END);

        assertNotEquals(r1.getReportNumber(), r2.getReportNumber());
    }

    @Test
    void shouldGenerateCorrectPrefixForEachType() {
        assertReportPrefix(ReportType.MONTHLY_HEALTH, "MH-");
        assertReportPrefix(ReportType.CONTINUOUS_MONITORING_CERTIFICATE, "CM-");
        assertReportPrefix(ReportType.CRITICAL_ALERT_REPORT, "CA-");
        assertReportPrefix(ReportType.INTERVENTION_REPORT, "IR-");
        assertReportPrefix(ReportType.ROI_AVOIDED_INCIDENTS, "ROI-");
        assertReportPrefix(ReportType.INSURER_EXPORT, "IE-");
    }

    @Test
    void shouldBeAccessibleByInsurerForCorrectTypes() {
        Report insurerReport = new Report(UUID.randomUUID(), TENANT, BUILDING,
                ReportType.INSURER_EXPORT, PERIOD_START, PERIOD_END);
        insurerReport.startGeneration();
        insurerReport.markGenerated("content".getBytes(), createMetadata());

        assertTrue(insurerReport.isAccessibleByInsurer());

        Report monthlyReport = new Report(UUID.randomUUID(), TENANT, BUILDING,
                ReportType.MONTHLY_HEALTH, PERIOD_START, PERIOD_END);
        monthlyReport.startGeneration();
        monthlyReport.markGenerated("content".getBytes(), createMetadata());

        assertFalse(monthlyReport.isAccessibleByInsurer());
    }

    @Test
    void shouldNotBeAccessibleByInsurerIfNotGenerated() {
        Report report = new Report(UUID.randomUUID(), TENANT, BUILDING,
                ReportType.INSURER_EXPORT, PERIOD_START, PERIOD_END);

        assertFalse(report.isAccessibleByInsurer());
    }

    @Test
    void shouldRejectNullContent() {
        Report report = createReport();
        report.startGeneration();

        assertThrows(NullPointerException.class,
                () -> report.markGenerated(null, createMetadata()));
    }

    @Test
    void shouldRejectNullMetadata() {
        Report report = createReport();
        report.startGeneration();

        assertThrows(NullPointerException.class,
                () -> report.markGenerated("content".getBytes(), null));
    }

    @Test
    void shouldGenerateFileName() {
        Report report = createReport();
        report.startGeneration();
        report.markGenerated("content".getBytes(), createMetadata());

        assertNotNull(report.getFileName());
        assertTrue(report.getFileName().startsWith("pyrosense_monthly_health_"));
        assertTrue(report.getFileName().endsWith(".pdf"));
    }

    private void assertReportPrefix(ReportType type, String prefix) {
        Report r = new Report(UUID.randomUUID(), TENANT, BUILDING, type, PERIOD_START, PERIOD_END);
        assertTrue(r.getReportNumber().startsWith(prefix),
                "Expected prefix '%s' for type %s but got '%s'".formatted(prefix, type, r.getReportNumber()));
    }

    private Report createReport() {
        return new Report(UUID.randomUUID(), TENANT, BUILDING,
                ReportType.MONTHLY_HEALTH, PERIOD_START, PERIOD_END);
    }

    private ReportMetadata createMetadata() {
        return new ReportMetadata("Bâtiment A", "123 Rue Test", 10, 45.0, 5, 1, 3, 2,
                List.of("Vérifier câblage"));
    }
}
