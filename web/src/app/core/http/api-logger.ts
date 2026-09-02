import { HttpErrorResponse, HttpRequest } from '@angular/common/http';

import { parseApiError, type ParsedApiError } from './parse-api-error';

const SENSITIVE_BODY_KEYS = new Set([
  'password',
  'refreshToken',
  'accessToken',
  'token',
  'clabe',
  'curp',
  'rfc',
  'nss',
]);

const SENSITIVE_QUERY_KEYS = new Set(['password', 'token', 'refreshToken', 'accessToken']);

const AUTH_PATH = /\/auth\/(login|register|refresh|logout)\b/;

/** Compact, safe description of an outgoing API request (no secrets). */
export function summarizeRequest(req: HttpRequest<unknown>): {
  path: string;
  auth: boolean;
  multipart: boolean;
  bodyKeys: string[];
} {
  const path = safePath(req.urlWithParams);
  const auth = AUTH_PATH.test(path);
  const multipart = req.body instanceof FormData;
  let bodyKeys: string[] = [];
  if (!auth && !multipart && req.body !== null && typeof req.body === 'object') {
    bodyKeys = Object.keys(req.body as Record<string, unknown>).sort();
  }
  return { path, auth, multipart, bodyKeys };
}

/**
 * Compact, safe description of a response body — counts and field names only, never PII values.
 * Helps spot DTO mismatches (e.g. API returns `fullName` but the client expects `firstName`).
 */
export function summarizeResponse(
  body: unknown,
  req: { auth: boolean; multipart: boolean },
): Record<string, unknown> {
  if (req.auth) {
    return summarizeAuthResponse(body);
  }
  if (body === null || body === undefined) {
    return {};
  }
  if (Array.isArray(body)) {
    return { count: body.length };
  }
  if (typeof body !== 'object') {
    return { type: typeof body };
  }

  const o = body as Record<string, unknown>;

  if (Array.isArray(o['items'])) {
    const items = o['items'] as unknown[];
    return {
      items: items.length,
      totalElements: readMetadataTotal(o['metadata']),
      firstItemKeys: keysOf(items[0]),
    };
  }

  if (Array.isArray(o['content'])) {
    const content = o['content'] as unknown[];
    return {
      content: content.length,
      totalElements: o['totalElements'],
      firstItemKeys: keysOf(content[0]),
    };
  }

  return { keys: Object.keys(o).sort() };
}

/** Structured console output for a successful API response. */
export function logApiSuccess(
  method: string,
  path: string,
  status: number,
  durationMs: number,
  summary: Record<string, unknown>,
): void {
  console.info(`[API OK] ${method} ${path} — ${status} · ${durationMs}ms`, summary);
}

/** Structured console output for a failed API response. */
export function logApiFailure(
  method: string,
  path: string,
  durationMs: number,
  parsed: ParsedApiError,
): void {
  const payload: Record<string, unknown> = {
    status: parsed.httpStatus,
    errorCode: parsed.errorCode,
    message: parsed.message,
    traceId: parsed.traceId,
    ...(parsed.fieldErrors.length > 0
      ? { fieldErrors: parsed.fieldErrors.map((f) => f.field) }
      : {}),
  };

  if (parsed.httpStatus === 403 && parsed.errorCode !== 'FORBIDDEN') {
    payload['hint'] = 'Likely missing or expired JWT — session refresh should run automatically.';
  }

  if (parsed.httpStatus >= 500 || parsed.httpStatus === 0) {
    console.error(`[API ERR] ${method} ${path} — ${parsed.httpStatus || 'network'} · ${durationMs}ms`, payload);
    return;
  }
  console.warn(`[API ERR] ${method} ${path} — ${parsed.httpStatus} · ${durationMs}ms`, payload);
}

export function logApiHttpError(
  method: string,
  path: string,
  err: HttpErrorResponse,
  durationMs: number,
): void {
  logApiFailure(method, path, durationMs, parseApiError(err));
}

function safePath(url: string): string {
  try {
    const u = new URL(url, 'http://local');
    const params = new URLSearchParams(u.search);
    for (const key of params.keys()) {
      if (SENSITIVE_QUERY_KEYS.has(key)) {
        params.set(key, '***');
      }
    }
    const query = params.toString();
    return u.pathname + (query ? `?${query}` : '');
  } catch {
    return url;
  }
}

function summarizeAuthResponse(body: unknown): Record<string, unknown> {
  if (body === null || typeof body !== 'object') {
    return {};
  }
  const o = body as Record<string, unknown>;
  const summary: Record<string, unknown> = {};
  if (typeof o['status'] === 'string') {
    summary['status'] = o['status'];
  }
  if (typeof o['tokenType'] === 'string') {
    summary['tokenType'] = o['tokenType'];
  }
  if (typeof o['expiresInSeconds'] === 'number') {
    summary['expiresInSeconds'] = o['expiresInSeconds'];
  }
  if (typeof o['message'] === 'string') {
    summary['message'] = o['message'];
  }
  return summary;
}

function readMetadataTotal(metadata: unknown): unknown {
  if (metadata === null || typeof metadata !== 'object') {
    return undefined;
  }
  return (metadata as Record<string, unknown>)['totalElements'];
}

function keysOf(value: unknown): string[] {
  if (value === null || typeof value !== 'object') {
    return [];
  }
  return Object.keys(value as object).sort();
}

/** Redact known sensitive keys from a plain object (for ad-hoc debug only). */
export function redactSensitiveFields(value: Record<string, unknown>): Record<string, unknown> {
  const out: Record<string, unknown> = {};
  for (const [key, v] of Object.entries(value)) {
    out[key] = SENSITIVE_BODY_KEYS.has(key) ? '***' : v;
  }
  return out;
}
