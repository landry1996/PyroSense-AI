import { TestBed } from '@angular/core/testing';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideHttpClient } from '@angular/common/http';
import { DeviceTechnicalApiService } from './device-technical-api.service';

describe('DeviceTechnicalApiService', () => {
  let service: DeviceTechnicalApiService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        DeviceTechnicalApiService,
      ],
    });

    service = TestBed.inject(DeviceTechnicalApiService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('should fetch device technical health', () => {
    const mockHealth = {
      deviceId: 'dev-1',
      serialNumber: 'SN-001',
      firmwareVersion: '2.1.0',
      hardwareRevision: 'rev-B',
      connectivity: 'LoRaWAN',
      lastHeartbeat: '2026-05-27T10:00:00Z',
      uptimeSeconds: 86400,
      signalQuality: 85,
      dataQualityScore: 92,
      dataQualityGrade: 'A',
      batteryPercent: 78,
      deviceTemperature: 42,
      clockDriftMs: 15,
      sequenceGaps: 0,
      rejectedTelemetryCount: 2,
      status: 'ACTIVE',
    };

    service.getDeviceTechnicalHealth('dev-1').subscribe(health => {
      expect(health.deviceId).toBe('dev-1');
      expect(health.signalQuality).toBe(85);
      expect(health.firmwareVersion).toBe('2.1.0');
    });

    const req = httpMock.expectOne('/api/v1/devices/dev-1/technical-health');
    expect(req.request.method).toBe('GET');
    req.flush(mockHealth);
  });

  it('should fetch telemetry quality with date params', () => {
    const from = '2026-05-26T00:00:00Z';
    const to = '2026-05-27T00:00:00Z';

    service.getTelemetryQuality('dev-1', from, to).subscribe(report => {
      expect(report.deviceId).toBe('dev-1');
    });

    const req = httpMock.expectOne(
      r => r.url === '/api/v1/devices/dev-1/telemetry-quality'
        && r.params.get('from') === from
        && r.params.get('to') === to
    );
    expect(req.request.method).toBe('GET');
    req.flush({
      deviceId: 'dev-1',
      receivedPerHour: [],
      rejectedPerHour: [],
      rejectionReasons: [],
      signalQualityTrend: [],
      offlinePeriods: [],
    });
  });

  it('should fetch device security status', () => {
    const mockSecurity = {
      deviceId: 'dev-1',
      credentialStatus: 'ACTIVE',
      credentialVersion: 3,
      lastRotation: '2026-05-01T08:00:00Z',
      failedAuthAttempts: 0,
      replayAttemptsBlocked: 0,
      isRevoked: false,
      lastAuthFailure: null,
    };

    service.getDeviceSecurityStatus('dev-1').subscribe(security => {
      expect(security.credentialStatus).toBe('ACTIVE');
      expect(security.isRevoked).toBeFalse();
      expect(security.credentialVersion).toBe(3);
    });

    const req = httpMock.expectOne('/api/v1/devices/dev-1/security-status');
    expect(req.request.method).toBe('GET');
    req.flush(mockSecurity);
  });

  it('should fetch pilot dashboard', () => {
    const mockDashboard = {
      pilotId: 'pilot-1',
      pilotName: 'Pilote Lyon Centre',
      status: 'ACTIVE',
      devices: [
        {
          deviceId: 'dev-1',
          serialNumber: 'SN-001',
          status: 'ACTIVE',
          signalQuality: 90,
          dataQualityGrade: 'A',
          lastHeartbeat: '2026-05-27T10:00:00Z',
          installationStatus: 'COMPLETED',
        },
      ],
      incidentsSummary: { open: 1, investigating: 2, resolved: 5, total: 8 },
      kpiSnapshot: null,
    };

    service.getPilotDashboard('pilot-1').subscribe(dashboard => {
      expect(dashboard.pilotName).toBe('Pilote Lyon Centre');
      expect(dashboard.devices.length).toBe(1);
      expect(dashboard.incidentsSummary.total).toBe(8);
    });

    const req = httpMock.expectOne('/api/v1/pilots/pilot-1/dashboard');
    expect(req.request.method).toBe('GET');
    req.flush(mockDashboard);
  });
});
