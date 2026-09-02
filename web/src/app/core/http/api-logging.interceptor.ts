import { isPlatformBrowser } from '@angular/common';
import {
  HttpContextToken,
  HttpErrorResponse,
  HttpEvent,
  HttpHandlerFn,
  HttpInterceptorFn,
  HttpRequest,
  HttpResponse,
} from '@angular/common/http';
import { inject, PLATFORM_ID } from '@angular/core';
import { Observable, catchError, tap, throwError } from 'rxjs';

import {
  logApiHttpError,
  logApiSuccess,
  summarizeRequest,
  summarizeResponse,
} from './api-logger';

/** Set on a request to skip console logging (rare; default is to log all /api/v1 calls). */
export const SKIP_API_LOGGING = new HttpContextToken<boolean>(() => false);

function isApiUrl(url: string): boolean {
  return url.includes('/api/v1/');
}

/**
 * Logs every Pimienta API call once: method, path, status, duration, and a safe response summary.
 * Auth bodies and tokens are never logged; list responses include item counts and first-item field names
 * so DTO mismatches are easier to spot during development.
 */
export const apiLoggingInterceptor: HttpInterceptorFn = (
  req: HttpRequest<unknown>,
  next: HttpHandlerFn,
): Observable<HttpEvent<unknown>> => {
  const platformId = inject(PLATFORM_ID);
  if (!isPlatformBrowser(platformId) || !isApiUrl(req.url) || req.context.get(SKIP_API_LOGGING)) {
    return next(req);
  }

  const started = performance.now();
  const reqSummary = summarizeRequest(req);

  return next(req).pipe(
    tap((event) => {
      if (!(event instanceof HttpResponse)) {
        return;
      }
      const durationMs = Math.round(performance.now() - started);
      logApiSuccess(
        req.method,
        reqSummary.path,
        event.status,
        durationMs,
        summarizeResponse(event.body, reqSummary),
      );
    }),
    catchError((err: unknown) => {
      if (err instanceof HttpErrorResponse) {
        const durationMs = Math.round(performance.now() - started);
        logApiHttpError(req.method, reqSummary.path, err, durationMs);
      }
      return throwError(() => err);
    }),
  );
};
