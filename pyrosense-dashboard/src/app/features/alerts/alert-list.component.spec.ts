import { ComponentFixture, TestBed } from '@angular/core/testing';
import { AlertListComponent } from './alert-list.component';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { ApiService } from '../../core/services/api.service';
import { of } from 'rxjs';

describe('AlertListComponent', () => {
  let component: AlertListComponent;
  let fixture: ComponentFixture<AlertListComponent>;
  let httpMock: HttpTestingController;

  const mockApiService = {
    getAlertStatistics: jasmine.createSpy().and.returnValue(of({
      totalOpen: 5, totalAcknowledged: 2, totalInProgress: 1, totalResolved: 10, criticalOpen: 2, slaBreached: 0,
    })),
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [AlertListComponent, NoopAnimationsModule],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([]),
        { provide: ApiService, useValue: mockApiService },
      ],
    }).compileComponents();

    httpMock = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(AlertListComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should create', () => {
    const req = httpMock.expectOne(r => r.url.includes('/api/v1/alerts'));
    req.flush([]);
    expect(component).toBeTruthy();
  });

  it('should show skeleton when loading', () => {
    const skeletons = fixture.nativeElement.querySelectorAll('app-skeleton');
    expect(skeletons.length).toBeGreaterThan(0);
    httpMock.expectOne(r => r.url.includes('/api/v1/alerts')).flush([]);
  });

  it('should show empty state when no alerts', () => {
    const req = httpMock.expectOne(r => r.url.includes('/api/v1/alerts'));
    req.flush([]);
    fixture.detectChanges();
    const empty = fixture.nativeElement.querySelector('app-empty-state');
    expect(empty).toBeTruthy();
  });

  it('should render table when alerts exist', () => {
    const req = httpMock.expectOne(r => r.url.includes('/api/v1/alerts'));
    req.flush([{
      id: 'a1', title: 'Test Alert', severity: 'CRITICAL', status: 'OPEN',
      type: 'MICRO_ARC_DETECTED', createdAt: '2026-05-20T10:00:00Z', slaBreached: false,
    }]);
    fixture.detectChanges();
    const table = fixture.nativeElement.querySelector('table');
    expect(table).toBeTruthy();
  });

  it('getSeverityLabel should return French labels', () => {
    httpMock.expectOne(r => r.url.includes('/api/v1/alerts')).flush([]);
    expect(component.getSeverityLabel('CRITICAL')).toBe('Critique');
    expect(component.getSeverityLabel('WARNING')).toBe('Warning');
  });

  it('getStatusLabel should return French labels', () => {
    httpMock.expectOne(r => r.url.includes('/api/v1/alerts')).flush([]);
    expect(component.getStatusLabel('OPEN')).toBe('Ouvert');
    expect(component.getStatusLabel('RESOLVED')).toBe('Resolu');
  });
});
