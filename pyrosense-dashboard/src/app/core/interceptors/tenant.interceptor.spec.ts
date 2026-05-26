import { TestBed } from '@angular/core/testing';
import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { KeycloakService } from 'keycloak-angular';
import { TenantService } from '../services/tenant.service';
import { tenantInterceptor } from './tenant.interceptor';

describe('tenantInterceptor', () => {
  let http: HttpClient;
  let httpMock: HttpTestingController;
  let tenantServiceSpy: jasmine.SpyObj<TenantService>;

  beforeEach(() => {
    tenantServiceSpy = jasmine.createSpyObj('TenantService', ['getCurrentTenantId']);

    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([tenantInterceptor])),
        provideHttpClientTesting(),
        { provide: TenantService, useValue: tenantServiceSpy },
        { provide: KeycloakService, useValue: {} },
      ],
    });

    http = TestBed.inject(HttpClient);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('should add X-Tenant-Id header when tenant is available', () => {
    tenantServiceSpy.getCurrentTenantId.and.returnValue('tenant-123');

    http.get('/api/test').subscribe();

    const req = httpMock.expectOne('/api/test');
    expect(req.request.headers.get('X-Tenant-Id')).toBe('tenant-123');
  });

  it('should not add header when no tenant', () => {
    tenantServiceSpy.getCurrentTenantId.and.returnValue(null);

    http.get('/api/test').subscribe();

    const req = httpMock.expectOne('/api/test');
    expect(req.request.headers.has('X-Tenant-Id')).toBeFalse();
  });
});
