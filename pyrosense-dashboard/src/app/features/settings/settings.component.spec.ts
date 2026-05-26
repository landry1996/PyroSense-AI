import { ComponentFixture, TestBed } from '@angular/core/testing';
import { SettingsComponent } from './settings.component';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { AuthService } from '../../core/services/auth.service';
import { KeycloakService } from 'keycloak-angular';
import { signal } from '@angular/core';

describe('SettingsComponent', () => {
  let component: SettingsComponent;
  let fixture: ComponentFixture<SettingsComponent>;
  let httpMock: HttpTestingController;

  const mockAuth = {
    currentUser: signal({ id: 'user-1', email: 'test@test.com', fullName: 'Test', roles: ['TENANT_ADMIN'], tenantId: 'tenant-123' }),
    userRoles: signal(['TENANT_ADMIN']),
    isReadOnly: signal(false),
    hasAnyRole: (...roles: string[]) => true,
    getUserId: () => 'user-1',
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [SettingsComponent, NoopAnimationsModule],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: AuthService, useValue: mockAuth },
        { provide: KeycloakService, useValue: {} },
      ],
    }).compileComponents();

    httpMock = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(SettingsComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  afterEach(() => {
    httpMock.verify();
  });

  function flushInit() {
    httpMock.expectOne(req => req.url.includes('/settings')).flush({ thresholds: { temperatureMax: 85, thdMax: 40, riskScoreCritical: 70, riskScoreWarning: 50, microArcCountMax: 3 } });
    httpMock.expectOne(req => req.url.includes('/emergency-contacts')).flush([]);
    httpMock.expectOne(req => req.url.includes('/notifications/preferences')).flush({
      emailEnabled: true, smsEnabled: false, pushEnabled: true,
      emailForCritical: true, emailForWarning: true, emailForInfo: false,
      smsForCritical: true, smsForWarning: false,
      quietHoursStart: '22:00', quietHoursEnd: '07:00', digestFrequency: 'DAILY',
    });
  }

  it('should create', () => {
    flushInit();
    expect(component).toBeTruthy();
  });

  it('hasUnsavedChanges should return false initially', () => {
    flushInit();
    expect(component.hasUnsavedChanges()).toBeFalse();
  });

  it('hasUnsavedChanges should return true after threshold change', () => {
    flushInit();
    component.onThresholdChange('temperatureMax', 90);
    expect(component.hasUnsavedChanges()).toBeTrue();
  });

  it('saveThresholds should PUT to API', () => {
    flushInit();

    component.onThresholdChange('temperatureMax', 90);
    component.saveThresholds();

    const saveReq = httpMock.expectOne(req => req.method === 'PUT' && req.url.includes('/settings'));
    expect(saveReq.request.method).toBe('PUT');
    saveReq.flush({});
    expect(component.hasUnsavedChanges()).toBeFalse();
  });

  it('should load contacts on init', () => {
    httpMock.expectOne(req => req.url.includes('/settings')).flush({ thresholds: {} });
    const contactsReq = httpMock.expectOne(req => req.url.includes('/emergency-contacts'));
    contactsReq.flush([{ id: '1', name: 'Jean', phone: '0601', email: 'j@t.co', role: 'ELECTRICIAN', priority: 1 }]);
    httpMock.expectOne(req => req.url.includes('/notifications/preferences')).flush({});
    expect(component.contacts().length).toBe(1);
  });
});
