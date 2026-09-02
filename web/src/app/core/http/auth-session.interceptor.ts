import { isPlatformBrowser } from '@angular/common';
import {
  HttpBackend,
  HttpClient,
  HttpContextToken,
  HttpErrorResponse,
  HttpInterceptorFn,
} from '@angular/common/http';
import { inject, PLATFORM_ID } from '@angular/core';
import { Router } from '@angular/router';
import { Observable, catchError, finalize, shareReplay, switchMap, throwError } from 'rxjs';

import { API_BASE_URL } from '../config/api.config';
import type { TokenResponse } from '../model/account/auth.dto';

/**
 * When true, this request already ran after a token refresh; do not refresh again (avoids loops).
 */
export const AUTH_RETRY_AFTER_REFRESH = new HttpContextToken<boolean>(() => false);

function isProtectedApiUrl(url: string): boolean {
  return url.includes('/api/v1/') && !url.includes('/api/v1/auth/');
}

let refreshInFlight: Observable<TokenResponse> | null = null;

/** POST /auth/refresh — issues a new access token; the refresh token is reused (not rotated). */
function refreshAccessToken(http: HttpClient): Observable<TokenResponse> {
  if (!refreshInFlight) {
    const refreshToken = sessionStorage.getItem('refreshToken');
    if (!refreshToken) {
      return throwError(() => new Error('Missing refresh token'));
    }
    refreshInFlight = http
      .post<TokenResponse>(`${API_BASE_URL}/auth/refresh`, { refreshToken })
      .pipe(
        finalize(() => {
          refreshInFlight = null;
        }),
        shareReplay({ bufferSize: 1, refCount: true }),
      );
  }
  return refreshInFlight;
}

function clearSessionAndRedirect(router: Router, err: HttpErrorResponse): Observable<never> {
  sessionStorage.removeItem('accessToken');
  sessionStorage.removeItem('refreshToken');
  const returnUrl = router.url.startsWith('/app') ? router.url : undefined;
  void router.navigate(['/auth/login'], returnUrl ? { queryParams: { returnUrl } } : {});
  return throwError(() => err);
}

function readApiErrorCode(err: HttpErrorResponse): string | null {
  const body = err.error;
  if (body === null || typeof body !== 'object') {
    return null;
  }
  const code = (body as Record<string, unknown>)['errorCode'];
  return typeof code === 'string' ? code : null;
}

/**
 * Spring Security returns {@code 403} (not {@code 401}) when the JWT is missing, invalid, or expired.
 * Only skip refresh when the API explicitly denies permission ({@code errorCode: FORBIDDEN}).
 */
function shouldAttemptTokenRefresh(err: HttpErrorResponse): boolean {
  if (err.status === 401) {
    return true;
  }
  if (err.status !== 403) {
    return false;
  }
  return readApiErrorCode(err) !== 'FORBIDDEN';
}

/**
 * On {@code 401}, or {@code 403} from an unauthenticated session (missing/expired JWT): one refresh via
 * {@code POST /auth/refresh}, then retries the original request once with the new access token.
 * The refresh token is not rotated — it stays valid until logout or JWT expiry.
 *
 * {@code 403} with {@code errorCode: FORBIDDEN} is RBAC and is not retried. If refresh fails or the
 * retried request still indicates an auth problem, clears session and sends the user to login.
 */
export const authSessionInterceptor: HttpInterceptorFn = (req, next) => {
  const platformId = inject(PLATFORM_ID);
  if (!isPlatformBrowser(platformId) || !isProtectedApiUrl(req.url)) {
    return next(req);
  }

  const router = inject(Router);
  const httpBackend = inject(HttpBackend);
  const bareHttp = new HttpClient(httpBackend);

  return next(req).pipe(
    catchError((err: unknown) => {
      if (!(err instanceof HttpErrorResponse)) {
        return throwError(() => err);
      }
      if (!shouldAttemptTokenRefresh(err)) {
        return throwError(() => err);
      }
      if (req.context.get(AUTH_RETRY_AFTER_REFRESH)) {
        return clearSessionAndRedirect(router, err);
      }
      const refreshToken = sessionStorage.getItem('refreshToken');
      if (!refreshToken) {
        return clearSessionAndRedirect(router, err);
      }

      return refreshAccessToken(bareHttp).pipe(
        switchMap((tokens) => {
          sessionStorage.setItem('accessToken', tokens.accessToken);
          const retryReq = req.clone({
            context: req.context.set(AUTH_RETRY_AFTER_REFRESH, true),
            headers: req.headers.delete('Authorization'),
          });
          return next(retryReq);
        }),
        catchError(() => clearSessionAndRedirect(router, err)),
      );
    }),
  );
};
