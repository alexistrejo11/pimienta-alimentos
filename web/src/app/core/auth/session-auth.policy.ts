import { HttpErrorResponse } from '@angular/common/http';

/** Cold-load recovery when the API is unreachable but tokens must stay. */
export const SERVICE_UNAVAILABLE_PATH = '/auth/servicio-no-disponible';

function readApiErrorCode(err: HttpErrorResponse): string | null {
  const body = err.error;
  if (body === null || typeof body !== 'object') {
    return null;
  }
  const code = (body as Record<string, unknown>)['errorCode'];
  return typeof code === 'string' ? code : null;
}

function asHttpError(err: unknown): HttpErrorResponse | null {
  return err instanceof HttpErrorResponse ? err : null;
}

/**
 * Spring / API auth vs permission rules for the web session (POS-aligned).
 * Clear tokens only on definitive credential rejection — never on network / 5xx / RBAC.
 */
export const sessionAuthPolicy = {
  /** Authenticated caller lacks permission ({@code 403} + {@code FORBIDDEN}). */
  isPermissionDenied(err: unknown): boolean {
    const http = asHttpError(err);
    return http !== null && http.status === 403 && readApiErrorCode(http) === 'FORBIDDEN';
  },

  /**
   * Transport or server outage: no response, non-HTTP throw, or {@code 5xx}.
   * Tokens must stay so the user can retry when the API recovers.
   */
  isTransientFailure(err: unknown): boolean {
    const http = asHttpError(err);
    if (http === null) {
      return true;
    }
    return http.status === 0 || http.status >= 500;
  },

  /**
   * Whether a protected API failure should trigger one access-token refresh.
   * {@code 401} always; {@code 403} only when it is not an explicit RBAC denial.
   */
  shouldAttemptTokenRefresh(err: unknown): boolean {
    const http = asHttpError(err);
    if (http === null) {
      return false;
    }
    if (http.status === 401) {
      return true;
    }
    if (http.status !== 403) {
      return false;
    }
    return readApiErrorCode(http) !== 'FORBIDDEN';
  },

  /**
   * Refresh endpoint (or post-refresh auth shape) rejected credentials.
   * Mirrors POS: only HTTP {@code 401}/{@code 403}, excluding RBAC {@code FORBIDDEN}.
   */
  refreshFailureRequiresLogout(err: unknown): boolean {
    const http = asHttpError(err);
    if (http === null) {
      return false;
    }
    if (http.status === 401) {
      return true;
    }
    if (http.status !== 403) {
      return false;
    }
    return readApiErrorCode(http) !== 'FORBIDDEN';
  },

  /**
   * Guards: wipe session and send to login only for explicit auth rejection.
   * Permission denials and transient failures must not clear tokens.
   */
  requiresSessionClear(err: unknown): boolean {
    if (sessionAuthPolicy.isPermissionDenied(err) || sessionAuthPolicy.isTransientFailure(err)) {
      return false;
    }
    return sessionAuthPolicy.shouldAttemptTokenRefresh(err);
  },
};
