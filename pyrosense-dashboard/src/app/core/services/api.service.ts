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

export interface RiskSummary {
  buildingId: string;
  averageScore: number;
  maxScore: number;
  criticalDevices: number;
  totalDevices: number;
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

  getCriticalAlerts(): Observable<AlertResponse[]> {
    return this.http.get<AlertResponse[]>(`${this.baseUrl}/alerts/critical`);
  }

  // Risk
  getBuildingRiskSummary(buildingId: string): Observable<RiskSummary> {
    return this.http.get<RiskSummary>(`${this.baseUrl}/risk/buildings/${buildingId}/summary`);
  }

  getDeviceRiskScore(deviceId: string): Observable<any> {
    return this.http.get<any>(`${this.baseUrl}/risk/devices/${deviceId}/latest`);
  }

  // User
  getCurrentUser(): Observable<any> {
    return this.http.get<any>(`${this.baseUrl}/users/me`);
  }
}
