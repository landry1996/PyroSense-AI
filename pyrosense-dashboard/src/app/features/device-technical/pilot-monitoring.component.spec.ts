import { ComponentFixture, TestBed } from '@angular/core/testing';
import { PilotMonitoringComponent } from './pilot-monitoring.component';
import { DeviceTechnicalApiService } from '../../core/services/device-technical-api.service';
import { ActivatedRoute } from '@angular/router';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { of, throwError } from 'rxjs';
import { PilotDashboard } from '../../core/models/device-technical.model';

describe('PilotMonitoringComponent', () => {
  let component: PilotMonitoringComponent;
  let fixture: ComponentFixture<PilotMonitoringComponent>;
  let mockApiService: jasmine.SpyObj<DeviceTechnicalApiService>;

  const mockDashboard: PilotDashboard = {
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
        lastHeartbeat: new Date(Date.now() - 120000).toISOString(), // 2 min ago
        installationStatus: 'COMPLETED',
      },
      {
        deviceId: 'dev-2',
        serialNumber: 'SN-002',
        status: 'OFFLINE',
        signalQuality: 25,
        dataQualityGrade: 'D',
        lastHeartbeat: new Date(Date.now() - 7200000).toISOString(), // 2h ago
        installationStatus: 'COMPLETED',
      },
    ],
    incidentsSummary: { open: 2, investigating: 1, resolved: 10, total: 13 },
    kpiSnapshot: {
      id: 'kpi-1',
      date: '2026-05-27',
      totalDevices: 10,
      activeDevices: 8,
      uptimePercent: 96.5,
      telemetryValidPercent: 94.2,
      avgSignalQuality: 78,
      alertsGenerated: 15,
      alertsConfirmed: 12,
      falsePositives: 3,
      falsePositiveRate: 20,
      incidentsOpen: 2,
      incidentsResolved: 10,
      computedAt: '2026-05-27T08:00:00Z',
    },
  };

  beforeEach(async () => {
    mockApiService = jasmine.createSpyObj('DeviceTechnicalApiService', ['getPilotDashboard']);
    mockApiService.getPilotDashboard.and.returnValue(of(mockDashboard));

    await TestBed.configureTestingModule({
      imports: [PilotMonitoringComponent, NoopAnimationsModule],
      providers: [
        { provide: DeviceTechnicalApiService, useValue: mockApiService },
        {
          provide: ActivatedRoute,
          useValue: { snapshot: { paramMap: { get: () => 'pilot-1' } } },
        },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(PilotMonitoringComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should call API with pilot id on init', () => {
    expect(mockApiService.getPilotDashboard).toHaveBeenCalledWith('pilot-1');
  });

  it('should display pilot name', () => {
    const el: HTMLElement = fixture.nativeElement;
    expect(el.textContent).toContain('Pilote Lyon Centre');
  });

  it('should display incidents summary counts', () => {
    const el: HTMLElement = fixture.nativeElement;
    expect(el.textContent).toContain('2'); // open
    expect(el.textContent).toContain('1'); // investigating
    expect(el.textContent).toContain('10'); // resolved
    expect(el.textContent).toContain('13'); // total
  });

  it('should display KPI uptime', () => {
    const el: HTMLElement = fixture.nativeElement;
    expect(el.textContent).toContain('96.5%');
  });

  it('should display KPI signal quality', () => {
    const el: HTMLElement = fixture.nativeElement;
    expect(el.textContent).toContain('78%');
  });

  it('should display KPI false positive rate', () => {
    const el: HTMLElement = fixture.nativeElement;
    expect(el.textContent).toContain('20%');
  });

  it('should display device serial numbers in table', () => {
    const el: HTMLElement = fixture.nativeElement;
    expect(el.textContent).toContain('SN-001');
    expect(el.textContent).toContain('SN-002');
  });

  it('should display device signal quality values', () => {
    const el: HTMLElement = fixture.nativeElement;
    expect(el.textContent).toContain('90%');
    expect(el.textContent).toContain('25%');
  });

  it('should display data quality grades', () => {
    const el: HTMLElement = fixture.nativeElement;
    expect(el.textContent).toContain('A');
    expect(el.textContent).toContain('D');
  });

  it('should show error state on API failure', () => {
    mockApiService.getPilotDashboard.and.returnValue(throwError(() => new Error('Network error')));
    const errorFixture = TestBed.createComponent(PilotMonitoringComponent);
    errorFixture.detectChanges();

    expect(errorFixture.componentInstance.error()).toBeTruthy();
    expect(errorFixture.componentInstance.loading()).toBeFalse();
  });

  it('should show empty state when no devices', () => {
    const emptyDashboard: PilotDashboard = {
      ...mockDashboard,
      devices: [],
    };
    mockApiService.getPilotDashboard.and.returnValue(of(emptyDashboard));
    const emptyFixture = TestBed.createComponent(PilotMonitoringComponent);
    emptyFixture.detectChanges();

    const el: HTMLElement = emptyFixture.nativeElement;
    expect(el.textContent).toContain('Aucun capteur');
  });

  it('should format relative time correctly', () => {
    // Just under a minute
    const justNow = new Date(Date.now() - 30000).toISOString();
    expect(component.formatRelativeTime(justNow)).toBe('A l\'instant');

    // 45 minutes ago
    const minutesAgo = new Date(Date.now() - 2700000).toISOString();
    expect(component.formatRelativeTime(minutesAgo)).toBe('Il y a 45 min');

    // 3 hours ago
    const hoursAgo = new Date(Date.now() - 10800000).toISOString();
    expect(component.formatRelativeTime(hoursAgo)).toBe('Il y a 3h');

    // 2 days ago
    const daysAgo = new Date(Date.now() - 172800000).toISOString();
    expect(component.formatRelativeTime(daysAgo)).toBe('Il y a 2j');
  });

  it('should cleanup on destroy', () => {
    const spy = spyOn(component['destroy$'], 'next');
    component.ngOnDestroy();
    expect(spy).toHaveBeenCalled();
  });
});
