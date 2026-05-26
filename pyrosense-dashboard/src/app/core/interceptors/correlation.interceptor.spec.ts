import { TestBed } from '@angular/core/testing';
import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { correlationInterceptor } from './correlation.interceptor';

describe('correlationInterceptor', () => {
  let http: HttpClient;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([correlationInterceptor])),
        provideHttpClientTesting(),
      ],
    });

    http = TestBed.inject(HttpClient);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('should add X-Correlation-Id header to all requests', () => {
    http.get('/api/test').subscribe();

    const req = httpMock.expectOne('/api/test');
    const correlationId = req.request.headers.get('X-Correlation-Id');
    expect(correlationId).toBeTruthy();
    expect(correlationId!.length).toBeGreaterThan(0);
  });

  it('should generate unique IDs for each request', () => {
    http.get('/api/a').subscribe();
    http.get('/api/b').subscribe();

    const reqA = httpMock.expectOne('/api/a');
    const reqB = httpMock.expectOne('/api/b');

    const idA = reqA.request.headers.get('X-Correlation-Id');
    const idB = reqB.request.headers.get('X-Correlation-Id');
    expect(idA).not.toEqual(idB);
  });
});
