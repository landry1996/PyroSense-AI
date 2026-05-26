import { TestBed, fakeAsync, tick } from '@angular/core/testing';
import { WebSocketService } from './websocket.service';
import { AuthService } from './auth.service';
import { signal } from '@angular/core';

describe('WebSocketService', () => {
  let service: WebSocketService;
  let authMock: jasmine.SpyObj<AuthService>;

  beforeEach(() => {
    authMock = jasmine.createSpyObj('AuthService', ['hasRole'], {
      currentUser: signal({ tenantId: 'tenant-123', fullName: 'Test', email: 'test@test.com', roles: [] }),
      isAuthenticated: signal(true),
      userRoles: signal(['OPERATOR']),
    });

    TestBed.configureTestingModule({
      providers: [
        WebSocketService,
        { provide: AuthService, useValue: authMock },
      ],
    });

    service = TestBed.inject(WebSocketService);
  });

  afterEach(() => {
    service.disconnect();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('should start disconnected', () => {
    expect(service.connected()).toBeFalse();
    expect(service.reconnecting()).toBeFalse();
  });

  it('should not connect without a tenant', () => {
    (authMock as any).currentUser = signal(null);
    TestBed.resetTestingModule();
    TestBed.configureTestingModule({
      providers: [
        WebSocketService,
        { provide: AuthService, useValue: { ...authMock, currentUser: signal(null) } },
      ],
    });
    const svc = TestBed.inject(WebSocketService);
    svc.connect();
    expect(svc.connected()).toBeFalse();
  });

  it('should provide alerts observable', () => {
    const obs = service.onAlerts();
    expect(obs).toBeTruthy();
    expect(obs.subscribe).toBeDefined();
  });

  it('should provide dashboard observable', () => {
    const obs = service.onDashboard();
    expect(obs).toBeTruthy();
    expect(obs.subscribe).toBeDefined();
  });

  it('should provide telemetry observable for a device', () => {
    const obs = service.onTelemetry('device-abc');
    expect(obs).toBeTruthy();
    expect(obs.subscribe).toBeDefined();
  });
});
