import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { vi } from 'vitest';

import { authTokenStorage } from '../auth/auth-token.storage';
import { WebTelemetryService } from './web-telemetry.service';

describe('WebTelemetryService', () => {
  let telemetry: WebTelemetryService;
  let http: HttpTestingController;

  beforeEach(() => {
    const store = new Map<string, string>();
    vi.stubGlobal('localStorage', {
      getItem: (key: string) => store.get(key) ?? null,
      setItem: (key: string, value: string) => {
        store.set(key, value);
      },
      removeItem: (key: string) => {
        store.delete(key);
      },
    });
    authTokenStorage.setTokens('test-token', 'test-refresh');
    TestBed.configureTestingModule({
      providers: [WebTelemetryService, provideHttpClient(), provideHttpClientTesting()],
    });
    telemetry = TestBed.inject(WebTelemetryService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    authTokenStorage.clear();
    http.verify();
    vi.unstubAllGlobals();
  });

  it('sends an authenticated, bounded API failure event', () => {
    telemetry.reportApiFailure('POST', '/employees', 500, 'trace-1');

    const request = http.expectOne((req) => req.url.endsWith('/telemetry/web/events'));
    expect(request.request.headers.get('Authorization')).toBe('Bearer test-token');
    expect(request.request.headers.get('X-Trace-Id')).toBe('trace-1');
    expect(request.request.body).toMatchObject({
      schemaVersion: 1,
      eventType: 'api_failure',
      level: 'ERROR',
      route: window.location.pathname,
      traceId: 'trace-1',
      release: '2.2.14',
    });
    request.flush({ accepted: 1 });
  });

  it('does not send without an access token', () => {
    authTokenStorage.clear();

    telemetry.reportError(new Error('not sent'));

    http.expectNone((req) => req.url.endsWith('/telemetry/web/events'));
  });
});
