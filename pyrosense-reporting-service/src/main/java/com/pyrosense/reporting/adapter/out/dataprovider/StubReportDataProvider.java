package com.pyrosense.reporting.adapter.out.dataprovider;

import com.pyrosense.reporting.application.port.out.ReportDataProviderPort;
import com.pyrosense.reporting.domain.model.ReportMetadata;
import com.pyrosense.reporting.domain.model.ReportType;
import com.pyrosense.shared.id.BuildingId;
import com.pyrosense.shared.id.TenantId;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

@Component
public class StubReportDataProvider implements ReportDataProviderPort {

    @Override
    public ReportMetadata gatherMetadata(TenantId tenantId, BuildingId buildingId,
                                          ReportType type, Instant periodStart, Instant periodEnd) {
        return new ReportMetadata(
                "Bâtiment " + buildingId.value().toString().substring(0, 8),
                "123 Rue de la Sécurité, 75001 Paris",
                12,
                1,
                35.5,
                8,
                2,
                5,
                4,
                3,
                1,
                -5.2,
                List.of("Bâtiment A - Score 72", "Bâtiment B - Score 65"),
                List.of(
                        "Vérifier le câblage du tableau principal",
                        "Planifier la maintenance préventive du circuit B3",
                        "Remplacer les capteurs de température vieillissants"
                ),
                "3 incidents évités estimés à 45,000€ d'économies"
        );
    }
}
