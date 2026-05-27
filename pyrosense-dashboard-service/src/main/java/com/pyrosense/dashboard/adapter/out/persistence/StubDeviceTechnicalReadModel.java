package com.pyrosense.dashboard.adapter.out.persistence;

import com.pyrosense.dashboard.application.port.out.DeviceTechnicalReadModelPort;
import com.pyrosense.dashboard.domain.model.DeviceSecurityStatus;
import com.pyrosense.dashboard.domain.model.DeviceTechnicalHealth;
import com.pyrosense.dashboard.domain.model.PilotDashboard.PilotDeviceSummary;
import com.pyrosense.dashboard.domain.model.TelemetryQualityReport;
import com.pyrosense.dashboard.domain.model.TelemetryQualityReport.*;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class StubDeviceTechnicalReadModel implements DeviceTechnicalReadModelPort {

    @Override
    public Optional<DeviceTechnicalHealth> findTechnicalHealth(String deviceId) {
        Instant now = Instant.now();
        return Optional.of(new DeviceTechnicalHealth(
                deviceId,
                "PYR-" + deviceId.substring(0, Math.min(8, deviceId.length())).toUpperCase(),
                "2.4.1",
                "rev-C",
                "LoRaWAN",
                now.minus(Duration.ofMinutes(3)),
                86400L * 12,
                87.5,
                92.3,
                "A",
                78.0,
                42.5,
                120L,
                2,
                7,
                "ACTIVE"
        ));
    }

    @Override
    public TelemetryQualityReport findTelemetryQuality(String deviceId, Instant from, Instant to) {
        Instant now = Instant.now();
        List<HourlyCount> received = new ArrayList<>();
        List<HourlyCount> rejected = new ArrayList<>();
        List<QualityPoint> qualityTrend = new ArrayList<>();

        for (int i = 23; i >= 0; i--) {
            Instant hour = now.minus(Duration.ofHours(i));
            received.add(new HourlyCount(hour, 58 + (i % 5)));
            rejected.add(new HourlyCount(hour, i % 3));
            qualityTrend.add(new QualityPoint(hour, 85.0 + (i % 10) * 1.5));
        }

        List<RejectionReason> reasons = List.of(
                new RejectionReason("CHECKSUM_MISMATCH", 4),
                new RejectionReason("TIMESTAMP_OUT_OF_RANGE", 2),
                new RejectionReason("DUPLICATE_SEQUENCE", 1)
        );

        List<OfflinePeriod> offlinePeriods = List.of(
                new OfflinePeriod(
                        now.minus(Duration.ofHours(48)),
                        now.minus(Duration.ofHours(47)),
                        60L
                )
        );

        return new TelemetryQualityReport(deviceId, received, rejected, reasons, qualityTrend, offlinePeriods);
    }

    @Override
    public DeviceSecurityStatus findSecurityStatus(String deviceId) {
        return new DeviceSecurityStatus(
                deviceId,
                "ACTIVE",
                3,
                Instant.now().minus(Duration.ofDays(14)),
                0,
                0,
                false,
                null
        );
    }

    @Override
    public List<PilotDeviceSummary> findPilotDeviceSummaries(UUID pilotId) {
        Instant now = Instant.now();
        return List.of(
                new PilotDeviceSummary(
                        "dev-001", "PYR-A1B2C3D4", "ACTIVE",
                        92.0, "A", now.minus(Duration.ofMinutes(2)), "INSTALLED"
                ),
                new PilotDeviceSummary(
                        "dev-002", "PYR-E5F6G7H8", "ACTIVE",
                        88.5, "B", now.minus(Duration.ofMinutes(5)), "INSTALLED"
                ),
                new PilotDeviceSummary(
                        "dev-003", "PYR-I9J0K1L2", "OFFLINE",
                        0.0, "F", now.minus(Duration.ofHours(6)), "INSTALLED"
                )
        );
    }
}
