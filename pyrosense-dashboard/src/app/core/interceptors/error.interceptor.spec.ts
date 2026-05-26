import { TestBed } from '@angular/core/testing';
import { HttpClient, HttpErrorResponse, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { Router } from '@angular/router';
import { MatSnackBar } from '@angular/material/snack-bar';
import { KeycloakService } from 'keycloak-angular';
import { errorInterceptor } from './error.interceptor';

describe('errorInterceptor', () => {
  let http: HttpClient;
  let httpMock: HttpTestingController;
  let snackBarSpy: jasmine.SpyObj<MatSnackBar>;
  let routerSpy: jasmine.SpyObj<Router>;
  let keycloakSpy: jasmine.SpyObj<KeycloakService>;

  beforeEach(() => {
    snackBarSpy = jasmine.createSpyObj('MatSnackBar', ['open']);
    routerSpy = jasmine.createSpyObj('Router', ['navigate']);
    keycloakSpy = jasmine.createSpyObj('KeycloakService', ['login']);

    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([errorInterceptor])),
        provideHttpClientTesting(),
        { provide: MatSnackBar, useValue: snackBarSpy },
        { provide: Router, useValue: routerSpy },
        { provide: KeycloakService, useValue: keycloakSpy },
      ],
    });

    http = TestBed.inject(HttpClient);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('should redirect to login on 401', () => {
    http.get('/api/test').subscribe({ error: () => {} });

    httpMock.expectOne('/api/test').flush(null, { status: 401, statusText: 'Unauthorized' });

    expect(keycloakSpy.login).toHaveBeenCalled();
  });

  it('should show snackbar and navigate on 403', () => {
    http.get('/api/test').subscribe({ error: () => {} });

    httpMock.expectOne('/api/test').flush(null, { status: 403, statusText: 'Forbidden' });

    expect(snackBarSpy.open).toHaveBeenCalledWith(jasmine.stringContaining('Acces refuse'), 'Fermer', jasmine.any(Object));
    expect(routerSpy.navigate).toHaveBeenCalledWith(['/dashboard']);
  });

  it('should show rate limit message on 429', () => {
    http.get('/api/test').subscribe({ error: () => {} });

    httpMock.expectOne('/api/test').flush(null, { status: 429, statusText: 'Too Many Requests' });

    expect(snackBarSpy.open).toHaveBeenCalledWith(jasmine.stringContaining('Trop de requetes'), 'Fermer', jasmine.any(Object));
  });

  it('should show server error with correlation id on 500', () => {
    http.get('/api/test').subscribe({ error: () => {} });

    httpMock.expectOne('/api/test').flush(null, {
      status: 500,
      statusText: 'Internal Server Error',
      headers: { 'X-Correlation-Id': 'abc12345-def6-7890' },
    });

    expect(snackBarSpy.open).toHaveBeenCalledWith(jasmine.stringContaining('ref: abc12345'), 'Fermer', jasmine.any(Object));
  });
});
