import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { vi } from 'vitest';

import { WebTelemetryService } from '../telemetry/web-telemetry.service';
import { apiLoggingInterceptor } from './api-logging.interceptor';
import { traceIdInterceptor } from './trace-id.interceptor';

describe('apiLoggingInterceptor', () => {
  let http: HttpClient;
  let backend: HttpTestingController;
  let reportApiFailure: ReturnType<typeof vi.fn>;

  beforeEach(() => {
    reportApiFailure = vi.fn();
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([traceIdInterceptor, apiLoggingInterceptor])),
        provideHttpClientTesting(),
        { provide: WebTelemetryService, useValue: { reportApiFailure } },
      ],
    });
    http = TestBed.inject(HttpClient);
    backend = TestBed.inject(HttpTestingController);
  });

  afterEach(() => backend.verify());

  it('reports the request X-Trace-Id on a status-0 failure', () => {
    http.get('/api/v1/employees').subscribe({ error: () => undefined });

    const request = backend.expectOne('/api/v1/employees');
    const sentTraceId = request.request.headers.get('X-Trace-Id');
    expect(sentTraceId).toBeTruthy();
    request.error(new ProgressEvent('error'), { status: 0, statusText: 'Unknown Error' });

    expect(reportApiFailure).toHaveBeenCalledWith('GET', '/api/v1/employees', 0, sentTraceId);
  });
});
