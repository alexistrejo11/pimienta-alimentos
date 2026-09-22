import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { authTokenStorage } from '../auth/auth-token.storage';
import { WebTelemetryService } from './web-telemetry.service';

describe('WebTelemetryService', () => {
  let telemetry: WebTelemetryService;
  let http: HttpTestingController;

  beforeEach(() => {
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
  });

  it('sends an authenticated, bounded API failure event', () => {
    telemetry.reportApiFailure('POST', '/employees', 500, 'trace-1');

    const request = http.expectOne((req) => req.url.endsWith('/telemetry/web/events'));
    expect(request.request.headers.get('Authorization')).toBe('Bearer test-token');
    expect(request.request.body).toMatchObject({
      schemaVersion: 1,
      eventType: 'api_failure',
      level: 'ERROR',
      route: window.location.pathname,
      traceId: 'trace-1',
      release: '2.2.0',
    });
    request.flush({ accepted: 1 });
  });

  it('does not send without an access token', () => {
    authTokenStorage.clear();

    telemetry.reportError(new Error('not sent'));

    http.expectNone((req) => req.url.endsWith('/telemetry/web/events'));
  });
});
