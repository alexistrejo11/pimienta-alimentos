import { HttpErrorResponse } from '@angular/common/http';
import { TestBed } from '@angular/core/testing';
import { provideRouter, Router, UrlTree } from '@angular/router';
import { firstValueFrom, of, throwError } from 'rxjs';
import { vi } from 'vitest';

import { authTokenStorage } from './auth-token.storage';
import { SERVICE_UNAVAILABLE_PATH } from './session-auth.policy';
import { SessionContextService } from './session-context.service';
import { workspaceAuthGuard } from './workspace-auth.guard';

describe('workspaceAuthGuard', () => {
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

  function runGuard(url = '/app/erp/dashboard') {
    return TestBed.runInInjectionContext(() =>
      workspaceAuthGuard({} as never, { url } as never),
    );
  }

  it('sends unauthenticated users to login', () => {
    const result = runGuard();
    expect(result).toBeInstanceOf(UrlTree);
    expect(router.serializeUrl(result as UrlTree)).toContain('/auth/login');
  });

  it('allows loaded workspace users', async () => {
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

    const result = await firstValueFrom(runGuard() as ReturnType<typeof of>);
    expect(result).toBe(true);
    expect(authTokenStorage.getAccessToken()).toBe('access');
  });

  it('keeps tokens and redirects to recovery on network failure', async () => {
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

    const result = (await firstValueFrom(runGuard('/app/ops') as never)) as UrlTree;
    expect(router.serializeUrl(result)).toContain(SERVICE_UNAVAILABLE_PATH);
    expect(authTokenStorage.getAccessToken()).toBe('access');
    expect(sessionClear).not.toHaveBeenCalled();
  });

  it('clears session and sends to login on explicit 401', async () => {
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

    const result = (await firstValueFrom(runGuard('/app/ops') as never)) as UrlTree;
    expect(router.serializeUrl(result)).toContain('/auth/login');
    expect(authTokenStorage.getAccessToken()).toBeNull();
    expect(sessionClear).toHaveBeenCalled();
  });
});
