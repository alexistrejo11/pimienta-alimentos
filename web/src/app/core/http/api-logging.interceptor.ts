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
import { WebTelemetryService } from '../telemetry/web-telemetry.service';

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
  const telemetry = inject(WebTelemetryService);

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
        if (shouldReportToTelemetry(req.method, err)) {
          telemetry.reportApiFailure(
            req.method,
            reqSummary.path.split('?')[0],
            err.status,
            readTraceId(err),
          );
        }
      }
      return throwError(() => err);
    }),
  );
};

function shouldReportToTelemetry(method: string, error: HttpErrorResponse): boolean {
  const write = !['GET', 'HEAD', 'OPTIONS'].includes(method.toUpperCase());
  return error.status === 0 || error.status >= 500 || (write && error.status >= 400);
}

function readTraceId(error: HttpErrorResponse): string | undefined {
  const traceId = error.headers.get('X-Trace-Id');
  return traceId || undefined;
}
