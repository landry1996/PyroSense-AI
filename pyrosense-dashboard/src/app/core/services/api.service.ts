import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';

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

export interface AlertResponse {
  id: string;
  type: string;
  severity: string;
  status: string;
  deviceId: string;
  tenantId: string;
  title: string;
  description: string;
  createdAt: string;
}

export interface AlertDetailResponse {
  id: string;
  tenantId: string;
  deviceId: string;
  type: string;
  severity: string;
  title: string;
  description: string;
  status: string;
  assignedTo: string | null;
  escalationLevel: string;
  createdAt: string;
  slaDeadline: string;
  acknowledgedAt: string | null;
  acknowledgedBy: string | null;
  resolvedAt: string | null;
  resolvedBy: string | null;
  resolutionNote: string | null;
  occurrenceCount: number;
  lastOccurrenceAt: string | null;
  slaBreached: boolean;
  comments: AlertCommentResponse[];
}

export interface AlertCommentResponse {
  id: string;
  authorId: string;
  content: string;
  createdAt: string;
}

export interface AlertStatistics {
  totalOpen: number;
  totalAcknowledged: number;
  totalInProgress: number;
  totalResolved: number;
  criticalOpen: number;
  slaBreached: number;
}

export interface RiskSummary {
  buildingId: string;
  averageScore: number;
  maxScore: number;
  criticalDevices: number;
  totalDevices: number;
}

export interface InterventionResponse {
  id: string;
  tenantId: string;
  alertId: string;
  deviceId: string;
  type: string;
  priority: string;
  status: string;
  description: string;
  assignedElectricianId: string | null;
  scheduledAt: string | null;
  startedAt: string | null;
  completedAt: string | null;
  result: string | null;
  riskImpact: RiskImpactResponse | null;
  diagnostic: DiagnosticResponse | null;
  createdAt: string;
  updatedAt: string;
}

export interface RiskImpactResponse {
  riskScoreBefore: number;
  riskScoreAfter: number;
  avoidedIncidentEstimateDays: number | null;
  riskReduction: number;
}

export interface DiagnosticResponse {
  observations: string;
  measurementsTaken: string | null;
  recommendations: string | null;
  diagnosticBy: string;
  recordedAt: string;
}

export interface KanbanResponse {
  created: InterventionResponse[];
  planned: InterventionResponse[];
  assigned: InterventionResponse[];
  inProgress: InterventionResponse[];
  completed: InterventionResponse[];
  cancelled: InterventionResponse[];
}

export interface InterventionStatistics {
  total: number;
  inProgress: number;
  completed: number;
  falsePositives: number;
  averageRiskReduction: number;
}

export interface ReportResponse {
  id: string;
  reportNumber: string;
  tenantId: string;
  buildingId: string;
  type: string;
  status: string;
  periodStart: string;
  periodEnd: string;
  fileName: string | null;
  signatureHash: string | null;
  metadata: ReportMetadata | null;
  createdAt: string;
  generatedAt: string | null;
}

export interface ReportMetadata {
  buildingName: string;
  buildingAddress: string;
  sensorCount: number;
  averageRiskScore: number;
  alertCount: number;
  criticalAlertCount: number;
  interventionCount: number;
  resolvedInterventionCount: number;
  recommendations: string[];
}

export interface DownloadTokenResponse {
  token: string;
  expiresAt: string;
}

export interface NotificationResponse {
  id: string;
  tenantId: string;
  recipientId: string;
  channel: string;
  severity: string;
  subject: string;
  status: string;
  retryCount: number;
  createdAt: string;
  sentAt: string | null;
  failureReason: string | null;
}

export interface NotificationStatistics {
  sent: number;
  failed: number;
  pending: number;
  retrying: number;
}

export interface DeviceStatisticsResponse {
  total: number;
  active: number;
  offline: number;
  provisioned: number;
  revoked: number;
}

export interface TenantRiskSummaryResponse {
  avgScore: number;
  trend: string;
  buildingsAtRisk: number;
}

export interface RiskHistoryPoint {
  date: string;
  score: number;
}

export interface BuildingResponse {
  id: string;
  name: string;
  address: string;
  totalDevices: number;
  activeDevices: number;
  riskScore: number;
  status: string;
  lastAlertAt: string | null;
  highestAlertSeverity: string | null;
}

export interface InterventionOverdueResponse {
  count: number;
}

export interface AnomalyResponse {
  id: string;
  type: string;
  severity: string;
  description: string;
  detectedAt: string;
  score: number;
}

@Injectable({ providedIn: 'root' })
export class ApiService {
  private readonly baseUrl = '/api/v1';

  constructor(private http: HttpClient) {}

  // Devices
  getDevicesByTenant(page = 0, size = 50): Observable<DeviceResponse[]> {
    const params = new HttpParams().set('page', page).set('size', size);
    return this.http.get<DeviceResponse[]>(`${this.baseUrl}/devices`, { params });
  }

  getDevicesByBuilding(buildingId: string, page = 0, size = 50): Observable<DeviceResponse[]> {
    const params = new HttpParams().set('page', page).set('size', size);
    return this.http.get<DeviceResponse[]>(`${this.baseUrl}/devices/building/${buildingId}`, { params });
  }

  getDevice(deviceId: string): Observable<DeviceResponse> {
    return this.http.get<DeviceResponse>(`${this.baseUrl}/devices/${deviceId}`);
  }

  // Telemetry
  getDeviceTelemetry(deviceId: string, from: string, to: string, granularity = '1hour'): Observable<TelemetryPoint[]> {
    const params = new HttpParams()
      .set('from', from)
      .set('to', to)
      .set('granularity', granularity);
    return this.http.get<TelemetryPoint[]>(`${this.baseUrl}/telemetry/devices/${deviceId}`, { params });
  }

  getRawTelemetry(deviceId: string, from: string, to: string, limit = 100): Observable<RawTelemetryPoint[]> {
    const params = new HttpParams()
      .set('from', from)
      .set('to', to)
      .set('granularity', 'raw')
      .set('limit', limit);
    return this.http.get<RawTelemetryPoint[]>(`${this.baseUrl}/telemetry/devices/${deviceId}`, { params });
  }

  // Alerts
  getAlerts(page = 0, size = 50, status?: string): Observable<AlertResponse[]> {
    let params = new HttpParams().set('page', page).set('size', size);
    if (status) params = params.set('status', status);
    return this.http.get<AlertResponse[]>(`${this.baseUrl}/alerts`, { params });
  }

  getAlertsList(page = 0, size = 50, status?: string, severity?: string): Observable<AlertDetailResponse[]> {
    let params = new HttpParams().set('page', page).set('size', size);
    if (status) params = params.set('status', status);
    if (severity) params = params.set('severity', severity);
    return this.http.get<AlertDetailResponse[]>(`${this.baseUrl}/alerts`, { params });
  }

  getAlertById(alertId: string): Observable<AlertDetailResponse> {
    return this.http.get<AlertDetailResponse>(`${this.baseUrl}/alerts/${alertId}`);
  }

  getCriticalAlerts(): Observable<AlertResponse[]> {
    return this.http.get<AlertResponse[]>(`${this.baseUrl}/alerts/critical`);
  }

  getAlertStatistics(): Observable<AlertStatistics> {
    return this.http.get<AlertStatistics>(`${this.baseUrl}/alerts/statistics`);
  }

  acknowledgeAlert(alertId: string, userId: string): Observable<AlertDetailResponse> {
    return this.http.post<AlertDetailResponse>(`${this.baseUrl}/alerts/${alertId}/acknowledge`, { userId });
  }

  resolveAlert(alertId: string, userId: string, resolutionNote: string): Observable<AlertDetailResponse> {
    return this.http.post<AlertDetailResponse>(`${this.baseUrl}/alerts/${alertId}/resolve`, { userId, resolutionNote });
  }

  markFalsePositive(alertId: string, userId: string, reason: string): Observable<AlertDetailResponse> {
    return this.http.post<AlertDetailResponse>(`${this.baseUrl}/alerts/${alertId}/false-positive`, { userId, reason });
  }

  addAlertComment(alertId: string, authorId: string, content: string): Observable<AlertDetailResponse> {
    return this.http.post<AlertDetailResponse>(`${this.baseUrl}/alerts/${alertId}/comments`, { authorId, content });
  }

  // Interventions
  getInterventions(page = 0, size = 50, status?: string): Observable<InterventionResponse[]> {
    let params = new HttpParams().set('page', page).set('size', size);
    if (status) params = params.set('status', status);
    return this.http.get<InterventionResponse[]>(`${this.baseUrl}/interventions`, { params });
  }

  getInterventionById(id: string): Observable<InterventionResponse> {
    return this.http.get<InterventionResponse>(`${this.baseUrl}/interventions/${id}`);
  }

  getInterventionKanban(): Observable<KanbanResponse> {
    return this.http.get<KanbanResponse>(`${this.baseUrl}/interventions/kanban`);
  }

  getInterventionStatistics(): Observable<InterventionStatistics> {
    return this.http.get<InterventionStatistics>(`${this.baseUrl}/interventions/statistics`);
  }

  assignIntervention(id: string, electricianId: string, scheduledAt: string): Observable<InterventionResponse> {
    return this.http.post<InterventionResponse>(`${this.baseUrl}/interventions/${id}/assign`, { electricianId, scheduledAt });
  }

  startIntervention(id: string): Observable<InterventionResponse> {
    return this.http.post<InterventionResponse>(`${this.baseUrl}/interventions/${id}/start`, {});
  }

  addDiagnostic(id: string, observations: string, diagnosticBy: string, measurementsTaken?: string, recommendations?: string): Observable<InterventionResponse> {
    return this.http.post<InterventionResponse>(`${this.baseUrl}/interventions/${id}/diagnostic`, {
      observations, diagnosticBy, measurementsTaken, recommendations,
    });
  }

  completeIntervention(id: string, result: string): Observable<InterventionResponse> {
    return this.http.post<InterventionResponse>(`${this.baseUrl}/interventions/${id}/complete`, { result });
  }

  cancelIntervention(id: string): Observable<InterventionResponse> {
    return this.http.post<InterventionResponse>(`${this.baseUrl}/interventions/${id}/cancel`, {});
  }

  recordRiskImpact(id: string, riskScoreBefore: number, riskScoreAfter: number, avoidedIncidentEstimateDays?: number): Observable<InterventionResponse> {
    return this.http.post<InterventionResponse>(`${this.baseUrl}/interventions/${id}/risk-impact`, {
      riskScoreBefore, riskScoreAfter, avoidedIncidentEstimateDays,
    });
  }

  // Risk
  getBuildingRiskSummary(buildingId: string): Observable<RiskSummary> {
    return this.http.get<RiskSummary>(`${this.baseUrl}/risk/buildings/${buildingId}/summary`);
  }

  getDeviceRiskScore(deviceId: string): Observable<any> {
    return this.http.get<any>(`${this.baseUrl}/risk/devices/${deviceId}/latest`);
  }

  // Reports
  getReports(page = 0, size = 50, type?: string): Observable<ReportResponse[]> {
    let params = new HttpParams().set('page', page).set('size', size);
    if (type) params = params.set('type', type);
    return this.http.get<ReportResponse[]>(`${this.baseUrl}/reports`, { params });
  }

  generateReport(tenantId: string, buildingId: string, type: string, periodStart: string, periodEnd: string): Observable<ReportResponse> {
    return this.http.post<ReportResponse>(`${this.baseUrl}/reports/monthly`, {
      tenantId, buildingId, type, periodStart, periodEnd,
    });
  }

  getReportDownloadToken(reportId: string): Observable<DownloadTokenResponse> {
    return this.http.get<DownloadTokenResponse>(`${this.baseUrl}/reports/${reportId}/download-token`);
  }

  getReportDownloadUrl(reportId: string, token: string): string {
    return `${this.baseUrl}/reports/${reportId}/download?token=${token}`;
  }

  // Notifications
  getNotifications(page = 0, size = 50, status?: string): Observable<NotificationResponse[]> {
    let params = new HttpParams().set('page', page).set('size', size);
    if (status) params = params.set('status', status);
    return this.http.get<NotificationResponse[]>(`${this.baseUrl}/notifications`, { params });
  }

  getNotificationsByRecipient(recipientId: string, page = 0, size = 50): Observable<NotificationResponse[]> {
    const params = new HttpParams().set('page', page).set('size', size);
    return this.http.get<NotificationResponse[]>(`${this.baseUrl}/notifications/recipient/${recipientId}`, { params });
  }

  getNotificationStatistics(): Observable<NotificationStatistics> {
    return this.http.get<NotificationStatistics>(`${this.baseUrl}/notifications/statistics`);
  }

  // Device Anomalies
  getDeviceAnomalies(deviceId: string): Observable<AnomalyResponse[]> {
    return this.http.get<AnomalyResponse[]>(`${this.baseUrl}/analysis/anomalies/${deviceId}`);
  }

  // Device Statistics
  getDeviceStatistics(): Observable<DeviceStatisticsResponse> {
    return this.http.get<DeviceStatisticsResponse>(`${this.baseUrl}/devices/statistics`);
  }

  // Risk - Tenant level
  getTenantRiskSummary(): Observable<TenantRiskSummaryResponse> {
    return this.http.get<TenantRiskSummaryResponse>(`${this.baseUrl}/risk/tenant/summary`);
  }

  getTenantRiskHistory(days = 30): Observable<RiskHistoryPoint[]> {
    const params = new HttpParams().set('days', days);
    return this.http.get<RiskHistoryPoint[]>(`${this.baseUrl}/risk/tenant/history`, { params });
  }

  getBuildingRiskHistory(buildingId: string, days = 30): Observable<RiskHistoryPoint[]> {
    const params = new HttpParams().set('days', days);
    return this.http.get<RiskHistoryPoint[]>(`${this.baseUrl}/risk/buildings/${buildingId}/history`, { params });
  }

  // Buildings
  getBuildings(): Observable<BuildingResponse[]> {
    return this.http.get<BuildingResponse[]>(`${this.baseUrl}/buildings`);
  }

  getBuildingById(buildingId: string): Observable<BuildingResponse> {
    return this.http.get<BuildingResponse>(`${this.baseUrl}/buildings/${buildingId}`);
  }

  // Interventions - overdue
  getInterventionsOverdue(): Observable<InterventionOverdueResponse> {
    return this.http.get<InterventionOverdueResponse>(`${this.baseUrl}/interventions/overdue`);
  }

  // Alerts by building
  getAlertsByBuilding(buildingId: string, page = 0, size = 50): Observable<AlertDetailResponse[]> {
    const params = new HttpParams().set('buildingId', buildingId).set('page', page).set('size', size);
    return this.http.get<AlertDetailResponse[]>(`${this.baseUrl}/alerts`, { params });
  }

  // Interventions by building
  getInterventionsByBuilding(buildingId: string, page = 0, size = 50): Observable<InterventionResponse[]> {
    const params = new HttpParams().set('buildingId', buildingId).set('page', page).set('size', size);
    return this.http.get<InterventionResponse[]>(`${this.baseUrl}/interventions`, { params });
  }

  // User
  getCurrentUser(): Observable<any> {
    return this.http.get<any>(`${this.baseUrl}/users/me`);
  }
}
