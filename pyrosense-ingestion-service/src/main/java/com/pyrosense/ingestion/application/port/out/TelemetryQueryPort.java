package com.pyrosense.ingestion.application.port.out;

import com.pyrosense.shared.id.BuildingId;
import com.pyrosense.shared.id.DeviceId;
import com.pyrosense.shared.id.TenantId;

import java.time.Instant;
import java.util.List;

public interface TelemetryQueryPort {

    record TimeSeriesPoint(
            Instant timestamp,
            double rmsCurrent,
            double rmsVoltage,
            double activePower,
            double powerFactor,
            double thd,
            double temperatureCelsius,
            double hfNoiseLevel,
            int microArcCount,
            int transientCount
    ) {}

    record AggregatedPoint(
            Instant bucket,
            double avgRmsCurrent,
            double avgRmsVoltage,
            double avgActivePower,
            double avgPowerFactor,
            double avgThd,
            double avgTemperature,
            double avgHfNoise,
            int totalMicroArcs,
            int totalTransients,
            double maxTemperature,
            long sampleCount
    ) {}

    List<TimeSeriesPoint> findRawByDevice(TenantId tenantId, DeviceId deviceId,
                                           Instant from, Instant to, int limit);

    List<AggregatedPoint> findAggregated1Min(TenantId tenantId, DeviceId deviceId,
                                              Instant from, Instant to);

    List<AggregatedPoint> findAggregated15Min(TenantId tenantId, DeviceId deviceId,
                                               Instant from, Instant to);

    List<AggregatedPoint> findAggregated1Hour(TenantId tenantId, DeviceId deviceId,
                                               Instant from, Instant to);

    List<AggregatedPoint> findAggregatedDaily(TenantId tenantId, DeviceId deviceId,
                                               Instant from, Instant to);

    List<AggregatedPoint> findByBuilding1Hour(TenantId tenantId, BuildingId buildingId,
                                               Instant from, Instant to);

    long countByDevice(TenantId tenantId, DeviceId deviceId, Instant from, Instant to);
}
