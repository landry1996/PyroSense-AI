export interface DeviceTechnicalHealth {
  deviceId: string;
  serialNumber: string;
  firmwareVersion: string;
  hardwareRevision: string;
  connectivity: string;
  lastHeartbeat: string;
  uptimeSeconds: number;
  signalQuality: number;
  dataQualityScore: number;
  dataQualityGrade: string;
  batteryPercent: number | null;
  deviceTemperature: number | null;
  clockDriftMs: number;
  sequenceGaps: number;
  rejectedTelemetryCount: number;
  status: string;
}

export interface HourlyCount {
  hour: string;
  count: number;
}

export interface RejectionReason {
  reason: string;
  count: number;
}

export interface QualityPoint {
  timestamp: string;
  quality: number;
}

export interface OfflinePeriod {
  start: string;
  end: string;
  durationMinutes: number;
}

export interface TelemetryQualityReport {
  deviceId: string;
  receivedPerHour: HourlyCount[];
  rejectedPerHour: HourlyCount[];
  rejectionReasons: RejectionReason[];
  signalQualityTrend: QualityPoint[];
  offlinePeriods: OfflinePeriod[];
}

export interface DeviceSecurityStatus {
  deviceId: string;
  credentialStatus: string;
  credentialVersion: number;
  lastRotation: string | null;
  failedAuthAttempts: number;
  replayAttemptsBlocked: number;
  isRevoked: boolean;
  lastAuthFailure: string | null;
}

export interface PilotDeviceSummary {
  deviceId: string;
  serialNumber: string;
  status: string;
  signalQuality: number;
  dataQualityGrade: string;
  lastHeartbeat: string;
  installationStatus: string;
}

export interface IncidentsSummary {
  open: number;
  investigating: number;
  resolved: number;
  total: number;
}

export interface PilotDashboard {
  pilotId: string;
  pilotName: string;
  status: string;
  devices: PilotDeviceSummary[];
  incidentsSummary: IncidentsSummary;
  kpiSnapshot: PilotKpiSnapshot | null;
}

export interface PilotKpiSnapshot {
  id: string;
  date: string;
  totalDevices: number;
  activeDevices: number;
  uptimePercent: number;
  telemetryValidPercent: number;
  avgSignalQuality: number;
  alertsGenerated: number;
  alertsConfirmed: number;
  falsePositives: number;
  falsePositiveRate: number;
  incidentsOpen: number;
  incidentsResolved: number;
  computedAt: string;
}
