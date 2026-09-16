import { isPlatformBrowser } from '@angular/common';
import { HttpBackend, HttpClient } from '@angular/common/http';
import { ErrorHandler, inject, Injectable, PLATFORM_ID } from '@angular/core';

import { API_BASE_URL, WEB_RELEASE } from '../config/api.config';

type WebTelemetryPayload = {
  schemaVersion: number;
  eventType: string;
  level: 'ERROR' | 'WARN' | 'INFO';
  message: string;
  stack?: string;
  route?: string;
  release?: string;
  traceId?: string;
  userAgent?: string;
  occurredAt: string;
};

/** Sends bounded browser diagnostics without passing through application interceptors. */
@Injectable({ providedIn: 'root' })
export class WebTelemetryService {
  private readonly platformId = inject(PLATFORM_ID);
  private readonly bareHttp = new HttpClient(inject(HttpBackend));

  reportError(error: unknown, eventType = 'unhandled_exception'): void {
    if (!isPlatformBrowser(this.platformId)) return;
    const normalized = normalizeError(error);
    this.send({
      schemaVersion: 1,
      eventType,
      level: 'ERROR',
      message: normalized.message,
      stack: normalized.stack,
      route: window.location.pathname,
      release: WEB_RELEASE,
      userAgent: truncateUserAgent(navigator.userAgent),
      occurredAt: new Date().toISOString(),
    });
  }

  reportApiFailure(method: string, path: string, status: number, traceId?: string): void {
    if (!isPlatformBrowser(this.platformId)) return;
    this.send({
      schemaVersion: 1,
      eventType: 'api_failure',
      level: status >= 500 || status === 0 ? 'ERROR' : 'WARN',
      message: `API ${method} ${path} failed (${status || 'network'})`,
      route: window.location.pathname,
      traceId: traceId ? truncate(traceId, 120) : undefined,
      release: WEB_RELEASE,
      userAgent: truncateUserAgent(navigator.userAgent),
      occurredAt: new Date().toISOString(),
    });
  }

  private send(payload: WebTelemetryPayload): void {
    const accessToken = sessionStorage.getItem('accessToken');
    if (!accessToken) return;

    // Telemetry is best effort and must never affect the user operation.
    this.bareHttp
      .post(`${API_BASE_URL}/telemetry/web/events`, payload, {
        headers: { Authorization: `Bearer ${accessToken}` },
      })
      .subscribe({ error: () => undefined });
  }
}

/** Routes Angular's uncaught browser errors to central telemetry and keeps console diagnostics. */
@Injectable()
export class CentralErrorHandler implements ErrorHandler {
  private readonly telemetry = inject(WebTelemetryService);

  handleError(error: unknown): void {
    console.error(error);
    this.telemetry.reportError(error);
  }
}

function normalizeError(error: unknown): { message: string; stack?: string } {
  if (error instanceof Error) {
    return { message: limit(error.message || error.name) ?? 'Unknown error', stack: limit(error.stack, 12000) };
  }
  return { message: limit(String(error)) ?? 'Unknown error' };
}

function limit(value: string | undefined, max = 2000): string | undefined {
  return truncate(value, max);
}

function truncate(value: string | undefined, max: number): string | undefined {
  if (value === undefined) return undefined;
  const clean = value.replace(/[\r\n]+/g, ' ').trim();
  return clean.length <= max ? clean : clean.slice(0, max);
}

function truncateUserAgent(ua: string): string {
  return truncate(ua, 120) ?? '';
}
