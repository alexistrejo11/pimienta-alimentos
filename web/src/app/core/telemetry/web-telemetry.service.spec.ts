import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { WebTelemetryService } from './web-telemetry.service';

describe('WebTelemetryService', () => {
  let telemetry: WebTelemetryService;
  let http: HttpTestingController;

  beforeEach(() => {
    sessionStorage.setItem('accessToken', 'test-token');
    TestBed.configureTestingModule({
      providers: [WebTelemetryService, provideHttpClient(), provideHttpClientTesting()],
    });
    telemetry = TestBed.inject(WebTelemetryService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    sessionStorage.removeItem('accessToken');
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
    });
    request.flush({ accepted: 1 });
  });

  it('does not send without an access token', () => {
    sessionStorage.removeItem('accessToken');

    telemetry.reportError(new Error('not sent'));

    http.expectNone((req) => req.url.endsWith('/telemetry/web/events'));
  });
});
