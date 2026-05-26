import { TestBed } from '@angular/core/testing';
import { AuthService } from './auth.service';
import { KeycloakService } from 'keycloak-angular';

describe('AuthService', () => {
  let service: AuthService;
  let keycloakSpy: jasmine.SpyObj<KeycloakService>;

  beforeEach(() => {
    keycloakSpy = jasmine.createSpyObj('KeycloakService', [
      'isLoggedIn', 'getKeycloakInstance', 'getUserRoles', 'logout'
    ]);
    keycloakSpy.isLoggedIn.and.returnValue(false);
    keycloakSpy.getUserRoles.and.returnValue([]);

    TestBed.configureTestingModule({
      providers: [
        AuthService,
        { provide: KeycloakService, useValue: keycloakSpy },
      ],
    });
    service = TestBed.inject(AuthService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('currentUser should be null when not authenticated', () => {
    expect(service.currentUser()).toBeNull();
  });

  it('isAuthenticated should be false when no profile', () => {
    expect(service.isAuthenticated()).toBeFalse();
  });

  it('getUserId should return empty string when not logged in', () => {
    expect(service.getUserId()).toBe('');
  });

  it('hasRole should return false when no roles', () => {
    expect(service.hasRole('ADMIN')).toBeFalse();
  });

  it('hasAnyRole should return false when no roles', () => {
    expect(service.hasAnyRole('ADMIN', 'USER')).toBeFalse();
  });

  it('loadProfile should set profile from token', async () => {
    keycloakSpy.isLoggedIn.and.returnValue(true);
    keycloakSpy.getKeycloakInstance.and.returnValue({
      tokenParsed: { sub: 'user-123', email: 'test@pyrosense.io', name: 'Test User', tenant_id: 'tenant-1' },
    } as any);
    keycloakSpy.getUserRoles.and.returnValue(['TENANT_ADMIN']);

    await service.loadProfile();

    expect(service.currentUser()).toEqual({
      id: 'user-123',
      email: 'test@pyrosense.io',
      fullName: 'Test User',
      roles: ['TENANT_ADMIN'],
      tenantId: 'tenant-1',
    });
    expect(service.isAuthenticated()).toBeTrue();
    expect(service.hasRole('TENANT_ADMIN')).toBeTrue();
    expect(service.hasAnyRole('PLATFORM_ADMIN', 'TENANT_ADMIN')).toBeTrue();
    expect(service.getUserId()).toBe('user-123');
  });

  it('logout should call keycloak logout', async () => {
    keycloakSpy.logout.and.returnValue(Promise.resolve());
    await service.logout();
    expect(keycloakSpy.logout).toHaveBeenCalled();
  });
});
