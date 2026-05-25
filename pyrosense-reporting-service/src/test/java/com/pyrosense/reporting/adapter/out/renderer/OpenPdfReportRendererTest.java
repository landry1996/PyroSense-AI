package com.pyrosense.reporting.adapter.out.renderer;

import com.pyrosense.reporting.application.port.out.ReportRendererPort.ReportRenderRequest;
import com.pyrosense.reporting.domain.model.ReportMetadata;
import com.pyrosense.reporting.domain.model.ReportType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class OpenPdfReportRendererTest {

    private OpenPdfReportRenderer renderer;

    @BeforeEach
    void setUp() {
        renderer = new OpenPdfReportRenderer();
    }

    @ParameterizedTest
    @EnumSource(ReportType.class)
    void shouldRenderPdfForAllReportTypes(ReportType type) {
        ReportRenderRequest request = createRequest(type);

        byte[] result = renderer.render(request);

        assertNotNull(result);
        assertTrue(result.length > 0);
        assertEquals('%', (char) result[0]);
        assertEquals('P', (char) result[1]);
        assertEquals('D', (char) result[2]);
        assertEquals('F', (char) result[3]);
    }

    @Test
    void shouldProduceDifferentOutputForDifferentTypes() {
        byte[] monthly = renderer.render(createRequest(ReportType.MONTHLY_HEALTH));
        byte[] certificate = renderer.render(createRequest(ReportType.CONTINUOUS_MONITORING_CERTIFICATE));

        assertNotEquals(monthly.length, certificate.length);
    }

    @Test
    void shouldRenderNonEmptyPdfWithConsistentOutput() {
        ReportRenderRequest request = createRequest(ReportType.MONTHLY_HEALTH);
        byte[] first = renderer.render(request);
        byte[] second = renderer.render(request);

        assertEquals(first.length, second.length);
    }

    @Test
    void shouldProduceValidPdfSize() {
        byte[] result = renderer.render(createRequest(ReportType.MONTHLY_HEALTH));
        assertTrue(result.length > 500);
        assertTrue(result.length < 100_000);
    }

    private ReportRenderRequest createRequest(ReportType type) {
        ReportMetadata metadata = new ReportMetadata(
                "Bâtiment Principal", "45 Avenue de la République, 75011 Paris",
                15, 42.5, 12, 3, 7, 5,
                List.of("Vérifier le circuit B3", "Planifier maintenance préventive", "Remplacer capteur #7")
        );
        return new ReportRenderRequest(
                "MH-202501-00001", type, "Bâtiment Principal",
                Instant.parse("2025-01-01T00:00:00Z"),
                Instant.parse("2025-01-31T23:59:59Z"),
                metadata
        );
    }
}
