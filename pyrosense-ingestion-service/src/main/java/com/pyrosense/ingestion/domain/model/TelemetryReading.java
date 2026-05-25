package com.pyrosense.ingestion.domain.model;

import com.pyrosense.shared.id.BuildingId;
import com.pyrosense.shared.id.CircuitId;
import com.pyrosense.shared.id.DeviceId;
import com.pyrosense.shared.id.ElectricalPanelId;
import com.pyrosense.shared.id.TenantId;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public final class TelemetryReading {

    private final UUID id;
    private final DeviceId deviceId;
    private final TenantId tenantId;
    private final BuildingId buildingId;
    private final ElectricalPanelId electricalPanelId;
    private final CircuitId circuitId;
    private final Instant timestamp;
    private final int samplingWindowMs;
    private final double rmsCurrent;
    private final double rmsVoltage;
    private final double activePower;
    private final double reactivePower;
    private final double powerFactor;
    private final double thd;
    private final double temperatureCelsius;
    private final double hfNoiseLevel;
    private final int microArcCount;
    private final int transientCount;
    private final String firmwareVersion;
    private final String payloadHash;
    private final Instant ingestedAt;

    private TelemetryReading(Builder builder) {
        this.id = Objects.requireNonNull(builder.id);
        this.deviceId = Objects.requireNonNull(builder.deviceId);
        this.tenantId = Objects.requireNonNull(builder.tenantId);
        this.buildingId = builder.buildingId;
        this.electricalPanelId = builder.electricalPanelId;
        this.circuitId = builder.circuitId;
        this.timestamp = Objects.requireNonNull(builder.timestamp);
        this.samplingWindowMs = builder.samplingWindowMs;
        this.rmsCurrent = builder.rmsCurrent;
        this.rmsVoltage = builder.rmsVoltage;
        this.activePower = builder.activePower;
        this.reactivePower = builder.reactivePower;
        this.powerFactor = builder.powerFactor;
        this.thd = builder.thd;
        this.temperatureCelsius = builder.temperatureCelsius;
        this.hfNoiseLevel = builder.hfNoiseLevel;
        this.microArcCount = builder.microArcCount;
        this.transientCount = builder.transientCount;
        this.firmwareVersion = builder.firmwareVersion;
        this.payloadHash = Objects.requireNonNull(builder.payloadHash);
        this.ingestedAt = Objects.requireNonNull(builder.ingestedAt);
    }

    public boolean hasMicroArcActivity() {
        return microArcCount > 0;
    }

    public boolean hasHighTemperature(double thresholdCelsius) {
        return temperatureCelsius > thresholdCelsius;
    }

    public boolean hasHighHfNoise(double threshold) {
        return hfNoiseLevel > threshold;
    }

    public static Builder builder() {
        return new Builder();
    }

    public UUID getId() { return id; }
    public DeviceId getDeviceId() { return deviceId; }
    public TenantId getTenantId() { return tenantId; }
    public BuildingId getBuildingId() { return buildingId; }
    public ElectricalPanelId getElectricalPanelId() { return electricalPanelId; }
    public CircuitId getCircuitId() { return circuitId; }
    public Instant getTimestamp() { return timestamp; }
    public int getSamplingWindowMs() { return samplingWindowMs; }
    public double getRmsCurrent() { return rmsCurrent; }
    public double getRmsVoltage() { return rmsVoltage; }
    public double getActivePower() { return activePower; }
    public double getReactivePower() { return reactivePower; }
    public double getPowerFactor() { return powerFactor; }
    public double getThd() { return thd; }
    public double getTemperatureCelsius() { return temperatureCelsius; }
    public double getHfNoiseLevel() { return hfNoiseLevel; }
    public int getMicroArcCount() { return microArcCount; }
    public int getTransientCount() { return transientCount; }
    public String getFirmwareVersion() { return firmwareVersion; }
    public String getPayloadHash() { return payloadHash; }
    public Instant getIngestedAt() { return ingestedAt; }

    public static final class Builder {
        private UUID id;
        private DeviceId deviceId;
        private TenantId tenantId;
        private BuildingId buildingId;
        private ElectricalPanelId electricalPanelId;
        private CircuitId circuitId;
        private Instant timestamp;
        private int samplingWindowMs;
        private double rmsCurrent;
        private double rmsVoltage;
        private double activePower;
        private double reactivePower;
        private double powerFactor;
        private double thd;
        private double temperatureCelsius;
        private double hfNoiseLevel;
        private int microArcCount;
        private int transientCount;
        private String firmwareVersion;
        private String payloadHash;
        private Instant ingestedAt;

        public Builder id(UUID id) { this.id = id; return this; }
        public Builder deviceId(DeviceId deviceId) { this.deviceId = deviceId; return this; }
        public Builder tenantId(TenantId tenantId) { this.tenantId = tenantId; return this; }
        public Builder buildingId(BuildingId buildingId) { this.buildingId = buildingId; return this; }
        public Builder electricalPanelId(ElectricalPanelId id) { this.electricalPanelId = id; return this; }
        public Builder circuitId(CircuitId circuitId) { this.circuitId = circuitId; return this; }
        public Builder timestamp(Instant timestamp) { this.timestamp = timestamp; return this; }
        public Builder samplingWindowMs(int v) { this.samplingWindowMs = v; return this; }
        public Builder rmsCurrent(double v) { this.rmsCurrent = v; return this; }
        public Builder rmsVoltage(double v) { this.rmsVoltage = v; return this; }
        public Builder activePower(double v) { this.activePower = v; return this; }
        public Builder reactivePower(double v) { this.reactivePower = v; return this; }
        public Builder powerFactor(double v) { this.powerFactor = v; return this; }
        public Builder thd(double v) { this.thd = v; return this; }
        public Builder temperatureCelsius(double v) { this.temperatureCelsius = v; return this; }
        public Builder hfNoiseLevel(double v) { this.hfNoiseLevel = v; return this; }
        public Builder microArcCount(int v) { this.microArcCount = v; return this; }
        public Builder transientCount(int v) { this.transientCount = v; return this; }
        public Builder firmwareVersion(String v) { this.firmwareVersion = v; return this; }
        public Builder payloadHash(String v) { this.payloadHash = v; return this; }
        public Builder ingestedAt(Instant v) { this.ingestedAt = v; return this; }

        public TelemetryReading build() {
            return new TelemetryReading(this);
        }
    }
}
