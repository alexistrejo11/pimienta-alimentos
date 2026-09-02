import { HttpErrorResponse } from '@angular/common/http';

import type { ApiErrorResponse } from './api-error.types';

/**
 * Normalized error for UI + logging after an HTTP call fails.
 * Built from our backend’s {@link ApiErrorResponse} when present, otherwise from the transport error.
 */
export interface ParsedApiError {
  /** Human-readable summary (usually {@link ApiErrorResponse.message}). */
  message: string;
  httpStatus: number;
  traceId: string | null;
  errorCode: string | null;
  fieldErrors: { field: string; message: string }[];
  context: Record<string, unknown> | null;
  /** Original {@link HttpErrorResponse} when applicable (for advanced logging). */
  httpError?: HttpErrorResponse;
  /** Raw {@link HttpErrorResponse.error} when it was not a recognized API shape. */
  rawBody: unknown;
}

function isApiErrorBody(value: unknown): value is ApiErrorResponse {
  if (value === null || typeof value !== 'object') {
    return false;
  }
  const o = value as Record<string, unknown>;
  return typeof o['errorCode'] === 'string' && typeof o['message'] === 'string';
}

/**
 * Turns any failure from `HttpClient` (or a thrown value) into a single structure the UI can render
 * and you can log consistently across the app.
 */
export function parseApiError(error: unknown): ParsedApiError {
  if (error instanceof HttpErrorResponse) {
    const body = error.error;

    if (isApiErrorBody(body)) {
      return {
        message: body.message,
        httpStatus: error.status,
        traceId: body.traceId ?? null,
        errorCode: body.errorCode ?? null,
        fieldErrors: body.fieldErrors ?? [],
        context: (body.context as Record<string, unknown> | null | undefined) ?? null,
        httpError: error,
        rawBody: body,
      };
    }

    const fallback =
      typeof body === 'string' && body.trim().length > 0
        ? body
        : error.statusText || error.message || 'Error de red o del servidor.';

    return {
      message: fallback,
      httpStatus: error.status,
      traceId: null,
      errorCode: null,
      fieldErrors: [],
      context: null,
      httpError: error,
      rawBody: body,
    };
  }

  if (error instanceof Error) {
    return {
      message: error.message,
      httpStatus: 0,
      traceId: null,
      errorCode: null,
      fieldErrors: [],
      context: null,
      rawBody: error,
    };
  }

  return {
    message: 'Error inesperado.',
    httpStatus: 0,
    traceId: null,
    errorCode: null,
    fieldErrors: [],
    context: null,
    rawBody: error,
  };
}

/**
 * First validation message for a given form field name (Spring often uses simple names like `email`).
 */
export function fieldMessage(parsed: ParsedApiError, field: string): string | undefined {
  const match = parsed.fieldErrors.find((f) => f.field === field);
  return match?.message;
}
