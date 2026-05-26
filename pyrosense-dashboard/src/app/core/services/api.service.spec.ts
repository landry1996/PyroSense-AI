import { TestBed } from '@angular/core/testing';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideHttpClient } from '@angular/common/http';
import { ApiService } from './api.service';

describe('ApiService', () => {
  let service: ApiService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        ApiService,
      ],
    });

    service = TestBed.inject(ApiService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('should fetch devices with pagination', () => {
    service.getDevicesByTenant(0, 20).subscribe(devices => {
      expect(devices.length).toBe(1);
    });

    const req = httpMock.expectOne(r => r.url === '/api/v1/devices' && r.params.get('page') === '0');
    expect(req.request.params.get('size')).toBe('20');
    req.flush([{ id: 'd1', serialNumber: 'SN001' }]);
  });

  it('should fetch alerts with status filter', () => {
    service.getAlerts(0, 50, 'OPEN').subscribe();

    const req = httpMock.expectOne(r => r.url === '/api/v1/alerts');
    expect(req.request.params.get('status')).toBe('OPEN');
    req.flush([]);
  });

  it('should fetch alert by id', () => {
    service.getAlertById('alert-1').subscribe(alert => {
      expect(alert.id).toBe('alert-1');
    });

    const req = httpMock.expectOne('/api/v1/alerts/alert-1');
    req.flush({ id: 'alert-1', title: 'Test alert' });
  });

  it('should fetch buildings', () => {
    service.getBuildings().subscribe(buildings => {
      expect(buildings.length).toBe(2);
    });

    const req = httpMock.expectOne('/api/v1/buildings');
    req.flush([{ id: 'b1' }, { id: 'b2' }]);
  });

  it('should acknowledge alert', () => {
    service.acknowledgeAlert('a1', 'user-1').subscribe();

    const req = httpMock.expectOne('/api/v1/alerts/a1/acknowledge');
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ userId: 'user-1' });
    req.flush({});
  });

  it('should fetch intervention kanban', () => {
    service.getInterventionKanban().subscribe(kanban => {
      expect(kanban.created).toBeDefined();
    });

    const req = httpMock.expectOne('/api/v1/interventions/kanban');
    req.flush({ created: [], planned: [], assigned: [], inProgress: [], completed: [], cancelled: [] });
  });

  it('should fetch tenant risk summary', () => {
    service.getTenantRiskSummary().subscribe(summary => {
      expect(summary.avgScore).toBe(42);
    });

    const req = httpMock.expectOne('/api/v1/risk/tenant/summary');
    req.flush({ avgScore: 42, trend: 'STABLE', buildingsAtRisk: 3 });
  });

  it('should fetch reports with type filter', () => {
    service.getReports(0, 10, 'MONTHLY').subscribe();

    const req = httpMock.expectOne(r => r.url === '/api/v1/reports');
    expect(req.request.params.get('type')).toBe('MONTHLY');
    req.flush([]);
  });

  it('should fetch device statistics', () => {
    service.getDeviceStatistics().subscribe(stats => {
      expect(stats.total).toBe(100);
    });

    const req = httpMock.expectOne('/api/v1/devices/statistics');
    req.flush({ total: 100, active: 90, offline: 8, provisioned: 1, revoked: 1 });
  });

  it('should generate report download URL', () => {
    const url = service.getReportDownloadUrl('r1', 'token-abc');
    expect(url).toBe('/api/v1/reports/r1/download?token=token-abc');
  });
});
