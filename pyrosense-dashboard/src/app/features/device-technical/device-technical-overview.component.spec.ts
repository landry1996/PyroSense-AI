import { ComponentFixture, TestBed } from '@angular/core/testing';
import { DeviceTechnicalOverviewComponent } from './device-technical-overview.component';
import { DeviceTechnicalApiService } from '../../core/services/device-technical-api.service';
import { ActivatedRoute } from '@angular/router';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { of, throwError } from 'rxjs';
import { DeviceTechnicalHealth } from '../../core/models/device-technical.model';

describe('DeviceTechnicalOverviewComponent', () => {
  let component: DeviceTechnicalOverviewComponent;
  let fixture: ComponentFixture<DeviceTechnicalOverviewComponent>;
  let mockApiService: jasmine.SpyObj<DeviceTechnicalApiService>;

  const mockHealth: DeviceTechnicalHealth = {
    deviceId: 'dev-1',
    serialNumber: 'SN-TEST-001',
    firmwareVersion: '2.3.1',
    hardwareRevision: 'rev-C',
    connectivity: 'LoRaWAN',
    lastHeartbeat: new Date(Date.now() - 300000).toISOString(), // 5 min ago
    uptimeSeconds: 172800, // 2 days
    signalQuality: 82,
    dataQualityScore: 91,
    dataQualityGrade: 'A',
    batteryPercent: 65,
    deviceTemperature: 38,
    clockDriftMs: 12,
    sequenceGaps: 0,
    rejectedTelemetryCount: 3,
    status: 'ACTIVE',
  };

  beforeEach(async () => {
    mockApiService = jasmine.createSpyObj('DeviceTechnicalApiService', ['getDeviceTechnicalHealth']);
    mockApiService.getDeviceTechnicalHealth.and.returnValue(of(mockHealth));

    await TestBed.configureTestingModule({
      imports: [DeviceTechnicalOverviewComponent, NoopAnimationsModule],
      providers: [
        { provide: DeviceTechnicalApiService, useValue: mockApiService },
        {
          provide: ActivatedRoute,
          useValue: { snapshot: { paramMap: { get: () => 'dev-1' } } },
        },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(DeviceTechnicalOverviewComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should call API with device id on init', () => {
    expect(mockApiService.getDeviceTechnicalHealth).toHaveBeenCalledWith('dev-1');
  });

  it('should display loading state initially before data loads', () => {
    // Reset to test loading state
    mockApiService.getDeviceTechnicalHealth.and.returnValue(of(mockHealth));
    const freshFixture = TestBed.createComponent(DeviceTechnicalOverviewComponent);
    // Before detectChanges, loading is true by default
    expect(freshFixture.componentInstance.loading()).toBeTrue();
  });

  it('should hide loading and show data after load', () => {
    expect(component.loading()).toBeFalse();
    expect(component.health()).toEqual(mockHealth);
  });

  it('should display serial number', () => {
    const el: HTMLElement = fixture.nativeElement;
    expect(el.textContent).toContain('SN-TEST-001');
  });

  it('should display firmware version', () => {
    const el: HTMLElement = fixture.nativeElement;
    expect(el.textContent).toContain('2.3.1');
  });

  it('should display hardware revision', () => {
    const el: HTMLElement = fixture.nativeElement;
    expect(el.textContent).toContain('rev-C');
  });

  it('should display signal quality', () => {
    const el: HTMLElement = fixture.nativeElement;
    expect(el.textContent).toContain('82%');
  });

  it('should display data quality score and grade', () => {
    const el: HTMLElement = fixture.nativeElement;
    expect(el.textContent).toContain('91');
    expect(el.textContent).toContain('A');
  });

  it('should format uptime correctly', () => {
    expect(component.formatUptime(172800)).toBe('2j 0h');
    expect(component.formatUptime(3661)).toBe('1h 1min');
    expect(component.formatUptime(90000)).toBe('1j 1h');
  });

  it('should show error state on API failure', () => {
    mockApiService.getDeviceTechnicalHealth.and.returnValue(throwError(() => new Error('Network error')));
    const errorFixture = TestBed.createComponent(DeviceTechnicalOverviewComponent);
    errorFixture.detectChanges();

    expect(errorFixture.componentInstance.error()).toBeTruthy();
    expect(errorFixture.componentInstance.loading()).toBeFalse();
  });

  it('should return correct signal color for good quality', () => {
    expect(component.getSignalColor(85)).toBe('#388e3c');
    expect(component.getSignalVariant(85)).toBe('default');
  });

  it('should return correct signal color for warning quality', () => {
    expect(component.getSignalColor(50)).toBe('#f57c00');
    expect(component.getSignalVariant(50)).toBe('warning');
  });

  it('should return correct signal color for critical quality', () => {
    expect(component.getSignalColor(20)).toBe('#d32f2f');
    expect(component.getSignalVariant(20)).toBe('critical');
  });

  it('should display battery and temperature when available', () => {
    const el: HTMLElement = fixture.nativeElement;
    expect(el.textContent).toContain('65%');
    expect(el.textContent).toContain('38');
  });

  it('should cleanup on destroy', () => {
    const spy = spyOn(component['destroy$'], 'next');
    component.ngOnDestroy();
    expect(spy).toHaveBeenCalled();
  });
});
