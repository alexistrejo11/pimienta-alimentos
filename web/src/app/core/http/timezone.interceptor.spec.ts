import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { clientZone } from '../i18n/client-zone';
import { timezoneInterceptor } from './timezone.interceptor';

describe('timezoneInterceptor', () => {
  let http: HttpClient;
  let backend: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(withInterceptors([timezoneInterceptor])), provideHttpClientTesting()],
    });
    http = TestBed.inject(HttpClient);
    backend = TestBed.inject(HttpTestingController);
  });

  afterEach(() => backend.verify());

  it('sets X-Timezone on API calls, including auth', () => {
    http.post('/api/v1/auth/login', {}).subscribe();

    const request = backend.expectOne('/api/v1/auth/login');
    expect(request.request.headers.get('X-Timezone')).toBe(clientZone());
    request.flush({});
  });

  it('leaves calls outside /api/ unchanged', () => {
    http.get('/assets/logo.svg').subscribe();

    const request = backend.expectOne('/assets/logo.svg');
    expect(request.request.headers.has('X-Timezone')).toBe(false);
    request.flush({});
  });
});
