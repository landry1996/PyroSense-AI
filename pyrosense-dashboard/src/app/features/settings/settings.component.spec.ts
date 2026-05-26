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
    hasAnyRole: (...roles: string[]) => true,
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

  it('should create', () => {
    const settingsReq = httpMock.expectOne(req => req.url.includes('/settings'));
    settingsReq.flush({ thresholds: { temperatureMax: 85, thdMax: 40, riskScoreCritical: 70, riskScoreWarning: 50, microArcCountMax: 3 } });
    const contactsReq = httpMock.expectOne(req => req.url.includes('/emergency-contacts'));
    contactsReq.flush([]);
    expect(component).toBeTruthy();
  });

  it('hasUnsavedChanges should return false initially', () => {
    httpMock.expectOne(req => req.url.includes('/settings')).flush({ thresholds: {} });
    httpMock.expectOne(req => req.url.includes('/emergency-contacts')).flush([]);
    expect(component.hasUnsavedChanges()).toBeFalse();
  });

  it('hasUnsavedChanges should return true after threshold change', () => {
    httpMock.expectOne(req => req.url.includes('/settings')).flush({ thresholds: { temperatureMax: 85 } });
    httpMock.expectOne(req => req.url.includes('/emergency-contacts')).flush([]);
    component.onThresholdChange('temperatureMax', 90);
    expect(component.hasUnsavedChanges()).toBeTrue();
  });

  it('saveThresholds should PUT to API', () => {
    httpMock.expectOne(req => req.url.includes('/settings')).flush({ thresholds: { temperatureMax: 85 } });
    httpMock.expectOne(req => req.url.includes('/emergency-contacts')).flush([]);

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
    expect(component.contacts().length).toBe(1);
  });
});
