import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ReportListComponent } from './report-list.component';
import { ApiService } from '../../core/services/api.service';
import { AuthService } from '../../core/services/auth.service';
import { MatSnackBar } from '@angular/material/snack-bar';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { of } from 'rxjs';

describe('ReportListComponent', () => {
  let component: ReportListComponent;
  let fixture: ComponentFixture<ReportListComponent>;

  const mockReports = [
    { id: '1', reportNumber: 'RPT-001', type: 'MONTHLY_HEALTH', periodStart: '2026-05-01', periodEnd: '2026-05-31', status: 'GENERATED', createdAt: '2026-05-28' },
    { id: '2', reportNumber: 'RPT-002', type: 'CRITICAL_ALERT_REPORT', periodStart: '2026-05-01', periodEnd: '2026-05-28', status: 'GENERATING', createdAt: '2026-05-28' },
  ];

  const mockBuildings = [
    { id: 'b1', name: 'Batiment A', address: '1 rue X', status: 'OK', riskScore: 30, totalDevices: 10, activeDevices: 9 },
  ];

  const mockApi = {
    getReports: jasmine.createSpy('getReports').and.returnValue(of(mockReports)),
    getBuildings: jasmine.createSpy('getBuildings').and.returnValue(of(mockBuildings)),
    generateReport: jasmine.createSpy('generateReport').and.returnValue(of({})),
    getReportDownloadToken: jasmine.createSpy('getReportDownloadToken').and.returnValue(of({ token: 'abc' })),
    getReportDownloadUrl: jasmine.createSpy('getReportDownloadUrl').and.returnValue('http://localhost/download'),
  };

  const mockAuth = {
    currentUser: () => ({ tenantId: 'tenant-1' }),
  };

  const mockSnackBar = {
    open: jasmine.createSpy('open'),
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ReportListComponent, NoopAnimationsModule],
      providers: [
        { provide: ApiService, useValue: mockApi },
        { provide: AuthService, useValue: mockAuth },
        { provide: MatSnackBar, useValue: mockSnackBar },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(ReportListComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should show skeleton when loading', () => {
    component.loading.set(true);
    fixture.detectChanges();
    const skeleton = fixture.nativeElement.querySelector('app-skeleton');
    expect(skeleton).toBeTruthy();
  });

  it('should display reports table after loading', () => {
    fixture.detectChanges();
    const table = fixture.nativeElement.querySelector('.reports-table');
    expect(table).toBeTruthy();
  });

  it('should show report numbers', () => {
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('RPT-001');
  });

  it('should show empty state when no reports', () => {
    component.loading.set(false);
    component.reports.set([]);
    fixture.detectChanges();
    const emptyState = fixture.nativeElement.querySelector('app-empty-state');
    expect(emptyState).toBeTruthy();
  });

  it('should have aria-label on generate button', () => {
    fixture.detectChanges();
    const btn = fixture.nativeElement.querySelector('button[aria-label="Generer un nouveau rapport"]');
    expect(btn).toBeTruthy();
  });

  it('should toggle generate form visibility', () => {
    expect(component.showGenerateForm).toBeFalse();
    component.showGenerateForm = true;
    fixture.detectChanges();
    const form = fixture.nativeElement.querySelector('.generate-form');
    expect(form).toBeTruthy();
  });

  it('should return correct type labels', () => {
    expect(component.getTypeLabel('MONTHLY_HEALTH')).toBe('Mensuel');
    expect(component.getTypeLabel('CRITICAL_ALERT_REPORT')).toBe('Alertes critiques');
    expect(component.getTypeLabel('INTERVENTION_REPORT')).toBe('Interventions');
  });

  it('should return correct status labels', () => {
    expect(component.getStatusLabel('PENDING')).toBe('En attente');
    expect(component.getStatusLabel('GENERATED')).toBe('Disponible');
    expect(component.getStatusLabel('FAILED')).toBe('Echoue');
  });

  it('should not generate report when fields are empty', () => {
    component.genType = '';
    component.generateReport();
    expect(mockApi.generateReport).not.toHaveBeenCalled();
  });

  it('should have role=main on container', () => {
    const container = fixture.nativeElement.querySelector('[role="main"]');
    expect(container).toBeTruthy();
  });

  it('should clean up on destroy', () => {
    spyOn(component['destroy$'], 'next');
    component.ngOnDestroy();
    expect(component['destroy$'].next).toHaveBeenCalled();
  });
});
