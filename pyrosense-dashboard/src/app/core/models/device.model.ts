export interface DeviceResponse {
  id: string;
  serialNumber: string;
  tenantId: string;
  buildingId: string;
  panelId: string;
  status: string;
  firmwareVersion: string;
  connectivityType: string;
  lastSeenAt: string;
}

export interface TelemetryPoint {
  bucket: string;
  avgRmsCurrent: number;
  avgRmsVoltage: number;
  avgActivePower: number;
  avgPowerFactor: number;
  avgThd: number;
  avgTemperature: number;
  avgHfNoise: number;
  totalMicroArcs: number;
  totalTransients: number;
  maxTemperature: number;
  sampleCount: number;
}

export interface RawTelemetryPoint {
  timestamp: string;
  rmsCurrent: number;
  rmsVoltage: number;
  activePower: number;
  powerFactor: number;
  thd: number;
  temperatureCelsius: number;
  hfNoiseLevel: number;
  microArcCount: number;
  transientCount: number;
}

export interface DeviceStatisticsResponse {
  total: number;
  active: number;
  offline: number;
  provisioned: number;
  revoked: number;
}

export interface AnomalyResponse {
  id: string;
  type: string;
  severity: string;
  description: string;
  detectedAt: string;
  score: number;
}
