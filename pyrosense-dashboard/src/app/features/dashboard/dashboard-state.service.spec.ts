import { TestBed } from '@angular/core/testing';
import { DashboardStateService } from './dashboard-state.service';
import { ApiService } from '../../core/services/api.service';
import { of, throwError } from 'rxjs';

describe('DashboardStateService', () => {
  let service: DashboardStateService;
  let apiSpy: jasmine.SpyObj<ApiService>;

  beforeEach(() => {
    apiSpy = jasmine.createSpyObj('ApiService', [
      'getDeviceStatistics', 'getTenantRiskSummary', 'getAlertStatistics',
      'getBuildings', 'getInterventionsOverdue', 'getTenantRiskHistory',
    ]);

    apiSpy.getDeviceStatistics.and.returnValue(of({ total: 25, active: 22, offline: 3, provisioned: 0, revoked: 0 }));
    apiSpy.getTenantRiskSummary.and.returnValue(of({ avgScore: 45, trend: 'STABLE', buildingsAtRisk: 1 }));
    apiSpy.getAlertStatistics.and.returnValue(of({ totalOpen: 5, totalAcknowledged: 2, totalInProgress: 1, totalResolved: 10, criticalOpen: 2, slaBreached: 0 }));
    apiSpy.getBuildings.and.returnValue(of([{ id: '1' }, { id: '2' }] as any));
    apiSpy.getInterventionsOverdue.and.returnValue(of({ count: 1 }));
    apiSpy.getTenantRiskHistory.and.returnValue(of([{ date: '2026-05-01', score: 40 }]));

    TestBed.configureTestingModule({
      providers: [
        DashboardStateService,
        { provide: ApiService, useValue: apiSpy },
      ],
    });
    service = TestBed.inject(DashboardStateService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('initial state should have loading true', () => {
    expect(service.loading()).toBeTrue();
  });

  it('load should set loading to false after success', () => {
    service.load();
    expect(service.loading()).toBeFalse();
  });

  it('load should populate summary correctly', () => {
    service.load();
    const s = service.summary();
    expect(s.totalBuildings).toBe(2);
    expect(s.totalDevices).toBe(25);
    expect(s.activeDevices).toBe(22);
    expect(s.offlineDevices).toBe(3);
    expect(s.avgRiskScore).toBe(45);
    expect(s.riskTrend).toBe('STABLE');
    expect(s.criticalAlerts).toBe(2);
    expect(s.overdueInterventions).toBe(1);
  });

  it('load should populate riskHistory', () => {
    service.load();
    expect(service.riskHistory().length).toBe(1);
    expect(service.riskHistory()[0].score).toBe(40);
  });

  it('riskLevel should be OK for score < 30', () => {
    apiSpy.getTenantRiskSummary.and.returnValue(of({ avgScore: 20, trend: 'STABLE', buildingsAtRisk: 0 }));
    service.load();
    expect(service.riskLevel()).toBe('OK');
  });

  it('riskLevel should be WATCH for score 30-49', () => {
    service.load();
    expect(service.riskLevel()).toBe('WATCH');
  });

  it('riskLevel should be AT_RISK for score 50-69', () => {
    apiSpy.getTenantRiskSummary.and.returnValue(of({ avgScore: 55, trend: 'DEGRADING', buildingsAtRisk: 2 }));
    service.load();
    expect(service.riskLevel()).toBe('AT_RISK');
  });

  it('riskLevel should be CRITICAL for score >= 70', () => {
    apiSpy.getTenantRiskSummary.and.returnValue(of({ avgScore: 80, trend: 'CRITICAL', buildingsAtRisk: 5 }));
    service.load();
    expect(service.riskLevel()).toBe('CRITICAL');
  });

  it('should handle errors gracefully with fallback data', () => {
    apiSpy.getDeviceStatistics.and.returnValue(throwError(() => new Error('fail')));
    service.load();
    expect(service.loading()).toBeFalse();
    expect(service.summary().totalDevices).toBe(0);
  });
});
