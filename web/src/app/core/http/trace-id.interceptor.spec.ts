import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { traceIdInterceptor } from './trace-id.interceptor';

const UUID =
  /^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$/;

describe('traceIdInterceptor', () => {
  let http: HttpClient;
  let backend: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(withInterceptors([traceIdInterceptor])), provideHttpClientTesting()],
    });
    http = TestBed.inject(HttpClient);
    backend = TestBed.inject(HttpTestingController);
  });

  afterEach(() => backend.verify());

  it('sets X-Trace-Id on /api/v1/ calls to a canonical UUID', () => {
    http.get('/api/v1/employees').subscribe();

    const request = backend.expectOne('/api/v1/employees');
    expect(request.request.headers.get('X-Trace-Id')).toMatch(UUID);
    request.flush([]);
  });

  it('does not replace an existing X-Trace-Id', () => {
    const existing = '550e8400-e29b-41d4-a716-446655440000';
    http.get('/api/v1/employees', { headers: { 'X-Trace-Id': existing } }).subscribe();

    const request = backend.expectOne('/api/v1/employees');
    expect(request.request.headers.get('X-Trace-Id')).toBe(existing);
    request.flush([]);
  });

  it('leaves calls outside /api/ unchanged', () => {
    http.get('/assets/logo.svg').subscribe();

    const request = backend.expectOne('/assets/logo.svg');
    expect(request.request.headers.has('X-Trace-Id')).toBe(false);
    request.flush({});
  });
});
