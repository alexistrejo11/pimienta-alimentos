import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { firstValueFrom } from 'rxjs';
import { vi } from 'vitest';

import { authTokenStorage } from '../auth/auth-token.storage';
import { API_BASE_URL } from '../config/api.config';
import { authSessionInterceptor } from './auth-session.interceptor';

describe('authSessionInterceptor', () => {
  let http: HttpClient;
  let backend: HttpTestingController;
  let router: Router;

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
    vi.stubGlobal('sessionStorage', {
      getItem: () => null,
      setItem: () => undefined,
      removeItem: () => undefined,
    });
    authTokenStorage.clear();
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([authSessionInterceptor])),
        provideHttpClientTesting(),
        provideRouter([]),
      ],
    });
    http = TestBed.inject(HttpClient);
    backend = TestBed.inject(HttpTestingController);
    router = TestBed.inject(Router);
    vi.spyOn(router, 'navigate').mockResolvedValue(true);
  });

  afterEach(() => {
    backend.verify();
    authTokenStorage.clear();
    vi.unstubAllGlobals();
  });

  it('does not clear tokens on status 0 (no response)', async () => {
    authTokenStorage.setTokens('access', 'refresh');
    const result = firstValueFrom(http.get(`${API_BASE_URL}/users/me`)).then(
      () => 'ok',
      () => 'err',
    );

    const req = backend.expectOne(`${API_BASE_URL}/users/me`);
    req.error(new ProgressEvent('error'), { status: 0, statusText: 'Unknown Error' });

    expect(await result).toBe('err');
    expect(authTokenStorage.getAccessToken()).toBe('access');
    expect(authTokenStorage.getRefreshToken()).toBe('refresh');
    expect(router.navigate).not.toHaveBeenCalled();
  });

  it('refreshes once on 401 and retries the original request', async () => {
    authTokenStorage.setTokens('stale', 'refresh');
    const result = firstValueFrom(http.get(`${API_BASE_URL}/users/me`));

    const first = backend.expectOne(`${API_BASE_URL}/users/me`);
    first.flush(
      { errorCode: 'UNAUTHORIZED', message: 'no' },
      { status: 401, statusText: 'Unauthorized' },
    );

    const refresh = backend.expectOne(`${API_BASE_URL}/auth/refresh`);
    expect(refresh.request.body).toEqual({ refreshToken: 'refresh' });
    refresh.flush({ accessToken: 'fresh', refreshToken: 'refresh', tokenType: 'Bearer' });

    const retry = backend.expectOne(`${API_BASE_URL}/users/me`);
    retry.flush({ id: 1 });

    await expect(result).resolves.toEqual({ id: 1 });
    expect(authTokenStorage.getAccessToken()).toBe('fresh');
    expect(router.navigate).not.toHaveBeenCalled();
  });

  it('clears session when refresh returns 401', async () => {
    authTokenStorage.setTokens('stale', 'refresh');
    const result = firstValueFrom(http.get(`${API_BASE_URL}/users/me`)).then(
      () => 'ok',
      () => 'err',
    );

    backend
      .expectOne(`${API_BASE_URL}/users/me`)
      .flush({ errorCode: 'UNAUTHORIZED', message: 'no' }, { status: 401, statusText: 'Unauthorized' });
    backend
      .expectOne(`${API_BASE_URL}/auth/refresh`)
      .flush({ errorCode: 'UNAUTHORIZED', message: 'no' }, { status: 401, statusText: 'Unauthorized' });

    expect(await result).toBe('err');
    expect(authTokenStorage.getAccessToken()).toBeNull();
    expect(authTokenStorage.getRefreshToken()).toBeNull();
    expect(router.navigate).toHaveBeenCalledWith(['/auth/login'], expect.anything());
  });

  it('keeps tokens when refresh fails with status 0', async () => {
    authTokenStorage.setTokens('stale', 'refresh');
    const result = firstValueFrom(http.get(`${API_BASE_URL}/users/me`)).then(
      () => 'ok',
      () => 'err',
    );

    backend
      .expectOne(`${API_BASE_URL}/users/me`)
      .flush({ errorCode: 'UNAUTHORIZED', message: 'no' }, { status: 401, statusText: 'Unauthorized' });
    backend
      .expectOne(`${API_BASE_URL}/auth/refresh`)
      .error(new ProgressEvent('error'), { status: 0, statusText: 'Unknown Error' });

    expect(await result).toBe('err');
    expect(authTokenStorage.getAccessToken()).toBe('stale');
    expect(authTokenStorage.getRefreshToken()).toBe('refresh');
    expect(router.navigate).not.toHaveBeenCalled();
  });

  it('does not refresh or clear on 403 FORBIDDEN', async () => {
    authTokenStorage.setTokens('access', 'refresh');
    const result = firstValueFrom(http.get(`${API_BASE_URL}/users/me`)).then(
      () => 'ok',
      () => 'err',
    );

    backend.expectOne(`${API_BASE_URL}/users/me`).flush(
      { errorCode: 'FORBIDDEN', message: 'no' },
      { status: 403, statusText: 'Forbidden' },
    );

    expect(await result).toBe('err');
    expect(authTokenStorage.getAccessToken()).toBe('access');
    expect(router.navigate).not.toHaveBeenCalled();
    backend.verify();
  });
});
