import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  DeviceTechnicalHealth,
  TelemetryQualityReport,
  DeviceSecurityStatus,
  PilotDashboard,
} from '../models/device-technical.model';

@Injectable({ providedIn: 'root' })
export class DeviceTechnicalApiService {
  private readonly baseUrl = '/api/v1';

  constructor(private http: HttpClient) {}

  getDeviceTechnicalHealth(deviceId: string): Observable<DeviceTechnicalHealth> {
    return this.http.get<DeviceTechnicalHealth>(
      `${this.baseUrl}/devices/${deviceId}/technical-health`
    );
  }

  getTelemetryQuality(deviceId: string, from: string, to: string): Observable<TelemetryQualityReport> {
    const params = new HttpParams().set('from', from).set('to', to);
    return this.http.get<TelemetryQualityReport>(
      `${this.baseUrl}/devices/${deviceId}/telemetry-quality`,
      { params }
    );
  }

  getDeviceSecurityStatus(deviceId: string): Observable<DeviceSecurityStatus> {
    return this.http.get<DeviceSecurityStatus>(
      `${this.baseUrl}/devices/${deviceId}/security-status`
    );
  }

  getPilotDashboard(pilotId: string): Observable<PilotDashboard> {
    return this.http.get<PilotDashboard>(
      `${this.baseUrl}/pilots/${pilotId}/dashboard`
    );
  }
}
