import { HttpErrorResponse } from '@angular/common/http';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute, provideRouter, Router } from '@angular/router';
import { of, throwError } from 'rxjs';
import { vi } from 'vitest';

import { authTokenStorage } from '../../../core/auth/auth-token.storage';
import { SessionContextService } from '../../../core/auth/session-context.service';
import { AppRole } from '../../../core/model/account/enums';
import { ServicioNoDisponiblePage } from './servicio-no-disponible';

describe('ServicioNoDisponiblePage', () => {
  let fixture: ComponentFixture<ServicioNoDisponiblePage>;
  let component: ServicioNoDisponiblePage;
  let ensureLoaded: ReturnType<typeof vi.fn>;
  let sessionClear: ReturnType<typeof vi.fn>;
  let router: Router;

  beforeEach(async () => {
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
    await TestBed.configureTestingModule({
      imports: [ServicioNoDisponiblePage],
      providers: [
        provideRouter([]),
        {
          provide: ActivatedRoute,
          useValue: { snapshot: { queryParamMap: { get: () => '/app/ops/dashboard' } } },
        },
        {
          provide: SessionContextService,
          useValue: {
            ensureLoaded,
            clear: sessionClear,
          },
        },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(ServicioNoDisponiblePage);
    component = fixture.componentInstance;
    router = TestBed.inject(Router);
    vi.spyOn(router, 'navigateByUrl').mockResolvedValue(true);
    vi.spyOn(router, 'navigate').mockResolvedValue(true);
    await fixture.whenStable();
  });

  afterEach(() => {
    authTokenStorage.clear();
    vi.unstubAllGlobals();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('retries into the workspace when the API recovers', () => {
    ensureLoaded.mockReturnValue(
      of({
        userId: 1,
        accountStatus: 'ACTIVE',
        roles: [AppRole.ADMIN],
        permissions: [],
        assignedHeadquarterIds: [],
      }),
    );

    component.retry();

    expect(router.navigateByUrl).toHaveBeenCalledWith('/app/ops/dashboard');
    expect(component.retrying()).toBe(false);
  });

  it('keeps tokens and shows an error when still transient', () => {
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

    component.retry();

    expect(authTokenStorage.getAccessToken()).toBe('access');
    expect(component.errorMessage()).toBeTruthy();
    expect(router.navigate).not.toHaveBeenCalled();
  });

  it('clears session and goes to login on auth failure', () => {
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

    component.retry();

    expect(authTokenStorage.getAccessToken()).toBeNull();
    expect(sessionClear).toHaveBeenCalled();
    expect(router.navigate).toHaveBeenCalledWith(['/auth/login']);
  });
});
