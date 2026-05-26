import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { DashboardComponent } from './dashboard.component';
import { DashboardStateService, DashboardSummary } from './dashboard-state.service';
import { WebSocketService } from '../../core/services/websocket.service';
import { provideRouter } from '@angular/router';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { signal } from '@angular/core';
import { EMPTY } from 'rxjs';

describe('DashboardComponent', () => {
  let component: DashboardComponent;
  let fixture: ComponentFixture<DashboardComponent>;

  const mockSummary: DashboardSummary = {
    totalBuildings: 5,
    totalDevices: 25,
    activeDevices: 22,
    offlineDevices: 3,
    avgRiskScore: 42,
    riskTrend: 'STABLE',
    criticalAlerts: 2,
    warningAlerts: 3,
    slaBreached: 0,
    overdueInterventions: 1,
    buildingsAtRisk: 1,
  };

  const mockState = {
    loading: signal(false),
    summary: signal(mockSummary),
    riskHistory: signal([{ date: '2026-05-01', score: 40 }, { date: '2026-05-02', score: 42 }]),
    error: signal(null),
    riskLevel: signal('WATCH'),
    load: jasmine.createSpy('load'),
  };

  const mockWs = {
    connected: signal(false),
    reconnecting: signal(false),
    connect: jasmine.createSpy('connect'),
    disconnect: jasmine.createSpy('disconnect'),
    onAlerts: () => EMPTY,
    onDashboard: () => EMPTY,
    onTelemetry: () => EMPTY,
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [DashboardComponent, NoopAnimationsModule],
      providers: [
        { provide: DashboardStateService, useValue: mockState },
        { provide: WebSocketService, useValue: mockWs },
        provideRouter([]),
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(DashboardComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should call state.load on init', () => {
    expect(mockState.load).toHaveBeenCalled();
  });

  it('should show skeleton loaders when loading', () => {
    mockState.loading.set(true);
    fixture.detectChanges();
    const skeletons = fixture.nativeElement.querySelectorAll('app-skeleton');
    expect(skeletons.length).toBeGreaterThan(0);
  });

  it('should display stat cards when not loading', () => {
    mockState.loading.set(false);
    fixture.detectChanges();
    const cards = fixture.nativeElement.querySelectorAll('.stat-card');
    expect(cards.length).toBe(6);
  });

  it('should display correct building count', () => {
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('5');
  });

  it('should display critical alerts count', () => {
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('2');
  });

  it('should have aria-label on chart canvas', () => {
    fixture.detectChanges();
    const canvas = fixture.nativeElement.querySelector('canvas[role="img"]');
    expect(canvas).toBeTruthy();
  });

  it('should cleanup interval on destroy', () => {
    spyOn(window, 'clearInterval');
    component.ngOnDestroy();
    expect(window.clearInterval).toHaveBeenCalled();
  });
});
