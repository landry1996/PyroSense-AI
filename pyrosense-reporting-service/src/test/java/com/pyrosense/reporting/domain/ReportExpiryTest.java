package com.pyrosense.reporting.domain;

import com.pyrosense.reporting.domain.model.*;
import com.pyrosense.shared.id.BuildingId;
import com.pyrosense.shared.id.TenantId;
import com.pyrosense.shared.util.ClockProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ReportExpiryTest {

    private static final Instant NOW = Instant.parse("2025-03-10T10:00:00Z");

    @BeforeEach
    void setUp() { ClockProvider.setClock(Clock.fixed(NOW, ZoneId.of("UTC"))); }

    @AfterEach
    void tearDown() { ClockProvider.reset(); }

    @Test
    void newReportShouldHaveExpiresAt() {
        Report report = createReport();
        assertNotNull(report.getExpiresAt());
        assertTrue(report.getExpiresAt().isAfter(NOW));
    }

    @Test
    void generatedReportShouldNotBeExpiredWithinTtl() {
        Report report = createReport();
        report.startGeneration();
        report.markGenerated("content".getBytes(), createMetadata());
        assertFalse(report.isExpired());
    }

    @Test
    void shouldTransitionToExpired() {
        Report report = createReport();
        report.startGeneration();
        report.markGenerated("content".getBytes(), createMetadata());
        report.markExpired();

        assertEquals(ReportStatus.EXPIRED, report.getStatus());
        assertTrue(report.isExpired());
        assertNull(report.getContent());
    }

    @Test
    void expiredReportShouldNotBeAccessibleByInsurer() {
        Report report = new Report(UUID.randomUUID(),
                new TenantId(UUID.randomUUID()), new BuildingId(UUID.randomUUID()),
                ReportType.INSURER_EXPORT, NOW, NOW.plus(Duration.ofDays(30)));
        report.startGeneration();
        report.markGenerated("content".getBytes(), createMetadata());
        report.markExpired();

        assertFalse(report.isAccessibleByInsurer());
    }

    private Report createReport() {
        return new Report(UUID.randomUUID(),
                new TenantId(UUID.randomUUID()), new BuildingId(UUID.randomUUID()),
                ReportType.MONTHLY_HEALTH, NOW, NOW.plus(Duration.ofDays(30)));
    }

    private ReportMetadata createMetadata() {
        return new ReportMetadata("Building", "Address", 10, 45.0, 5, 1, 3, 2, List.of("Reco"));
    }
}
