import { TestBed } from '@angular/core/testing';
import { ActivatedRouteSnapshot, Router } from '@angular/router';
import { KeycloakService } from 'keycloak-angular';
import { roleGuard } from './role.guard';

describe('roleGuard', () => {
  let keycloakSpy: jasmine.SpyObj<KeycloakService>;
  let routerSpy: jasmine.SpyObj<Router>;

  beforeEach(() => {
    keycloakSpy = jasmine.createSpyObj('KeycloakService', ['getUserRoles']);
    routerSpy = jasmine.createSpyObj('Router', ['navigate']);

    TestBed.configureTestingModule({
      providers: [
        { provide: KeycloakService, useValue: keycloakSpy },
        { provide: Router, useValue: routerSpy },
      ],
    });
  });

  function runGuard(roles: string[]): boolean {
    const route = { data: { roles } } as unknown as ActivatedRouteSnapshot;
    return TestBed.runInInjectionContext(() => roleGuard(route, {} as any)) as boolean;
  }

  it('should allow access when user has required role', () => {
    keycloakSpy.getUserRoles.and.returnValue(['TENANT_ADMIN', 'PROPERTY_MANAGER']);
    expect(runGuard(['TENANT_ADMIN'])).toBeTrue();
  });

  it('should deny access and redirect when user lacks role', () => {
    keycloakSpy.getUserRoles.and.returnValue(['ELECTRICIAN']);
    expect(runGuard(['PLATFORM_ADMIN', 'TENANT_ADMIN'])).toBeFalse();
    expect(routerSpy.navigate).toHaveBeenCalledWith(['/dashboard']);
  });

  it('should allow access when no roles required', () => {
    keycloakSpy.getUserRoles.and.returnValue([]);
    expect(runGuard([])).toBeTrue();
  });
});
