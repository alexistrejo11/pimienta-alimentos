import { HttpErrorResponse } from '@angular/common/http';
import { TestBed } from '@angular/core/testing';
import { provideRouter, Router, UrlTree } from '@angular/router';
import { firstValueFrom, of, throwError } from 'rxjs';
import { vi } from 'vitest';

import { authTokenStorage } from './auth-token.storage';
import { guestAuthGuard } from './guest-auth.guard';
import { SERVICE_UNAVAILABLE_PATH } from './session-auth.policy';
import { SessionContextService } from './session-context.service';

describe('guestAuthGuard', () => {
  let ensureLoaded: ReturnType<typeof vi.fn>;
  let sessionClear: ReturnType<typeof vi.fn>;
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
    ensureLoaded = vi.fn();
    sessionClear = vi.fn();
    TestBed.configureTestingModule({
      providers: [
        provideRouter([]),
        {
          provide: SessionContextService,
          useValue: {
            ensureLoaded,
            clear: sessionClear,
          },
        },
      ],
    });
    router = TestBed.inject(Router);
  });

  afterEach(() => {
    authTokenStorage.clear();
    vi.unstubAllGlobals();
  });

  function runGuard() {
    return TestBed.runInInjectionContext(() => guestAuthGuard({} as never, {} as never));
  }

  it('allows the login page when there is no token', () => {
    expect(runGuard()).toBe(true);
  });

  it('redirects authenticated users into the workspace', async () => {
    authTokenStorage.setTokens('access', 'refresh');
    ensureLoaded.mockReturnValue(
      of({
        userId: 1,
        accountStatus: 'ACTIVE',
        roles: ['ADMIN'],
        permissions: [],
        assignedHeadquarterIds: [],
      }),
    );

    const result = (await firstValueFrom(runGuard() as never)) as UrlTree;
    expect(router.serializeUrl(result)).toBe('/app');
  });

  it('keeps tokens and sends to recovery on network failure', async () => {
    authTokenStorage.setTokens('access', 'refresh');
    ensureLoaded.mockReturnValue(
      throwError(
        () =>
          new HttpErrorResponse({
            status: 0,
            statusText: 'Unknown Error',
            url: '/api/v1/users/me',
          }),
      ),
    );

    const result = (await firstValueFrom(runGuard() as never)) as UrlTree;
    expect(router.serializeUrl(result)).toContain(SERVICE_UNAVAILABLE_PATH);
    expect(authTokenStorage.getAccessToken()).toBe('access');
    expect(sessionClear).not.toHaveBeenCalled();
  });

  it('clears invalid tokens so login can proceed', async () => {
    authTokenStorage.setTokens('access', 'refresh');
    ensureLoaded.mockReturnValue(
      throwError(
        () =>
          new HttpErrorResponse({
            status: 401,
            statusText: 'Unauthorized',
            error: { errorCode: 'UNAUTHORIZED', message: 'no' },
            url: '/api/v1/users/me',
          }),
      ),
    );

    expect(await firstValueFrom(runGuard() as never)).toBe(true);
    expect(authTokenStorage.getAccessToken()).toBeNull();
    expect(sessionClear).toHaveBeenCalled();
  });
});
