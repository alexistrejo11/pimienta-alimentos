import { HttpErrorResponse } from '@angular/common/http';

import { sessionAuthPolicy } from './session-auth.policy';

function httpError(status: number, errorCode?: string): HttpErrorResponse {
  return new HttpErrorResponse({
    status,
    statusText: 'Error',
    url: '/api/v1/users/me',
    error: errorCode ? { errorCode, message: 'x' } : null,
  });
}

describe('sessionAuthPolicy', () => {
  it('treats 401 as refresh + session-clear candidate', () => {
    const err = httpError(401, 'UNAUTHORIZED');
    expect(sessionAuthPolicy.shouldAttemptTokenRefresh(err)).toBe(true);
    expect(sessionAuthPolicy.requiresSessionClear(err)).toBe(true);
    expect(sessionAuthPolicy.isTransientFailure(err)).toBe(false);
    expect(sessionAuthPolicy.isPermissionDenied(err)).toBe(false);
  });

  it('treats 403 FORBIDDEN as permission only', () => {
    const err = httpError(403, 'FORBIDDEN');
    expect(sessionAuthPolicy.isPermissionDenied(err)).toBe(true);
    expect(sessionAuthPolicy.shouldAttemptTokenRefresh(err)).toBe(false);
    expect(sessionAuthPolicy.requiresSessionClear(err)).toBe(false);
    expect(sessionAuthPolicy.refreshFailureRequiresLogout(err)).toBe(false);
  });

  it('treats odd 403 without FORBIDDEN as auth-shaped', () => {
    const err = httpError(403);
    expect(sessionAuthPolicy.shouldAttemptTokenRefresh(err)).toBe(true);
    expect(sessionAuthPolicy.requiresSessionClear(err)).toBe(true);
    expect(sessionAuthPolicy.refreshFailureRequiresLogout(err)).toBe(true);
  });

  it('keeps session on network / 5xx / non-HTTP errors', () => {
    expect(sessionAuthPolicy.isTransientFailure(httpError(0))).toBe(true);
    expect(sessionAuthPolicy.isTransientFailure(httpError(502))).toBe(true);
    expect(sessionAuthPolicy.isTransientFailure(new Error('boom'))).toBe(true);
    expect(sessionAuthPolicy.requiresSessionClear(httpError(0))).toBe(false);
    expect(sessionAuthPolicy.requiresSessionClear(httpError(503))).toBe(false);
    expect(sessionAuthPolicy.shouldAttemptTokenRefresh(httpError(0))).toBe(false);
    expect(sessionAuthPolicy.refreshFailureRequiresLogout(httpError(0))).toBe(false);
  });

  it('does not clear session on other business 4xx', () => {
    const err = httpError(404, 'NOT_FOUND');
    expect(sessionAuthPolicy.requiresSessionClear(err)).toBe(false);
    expect(sessionAuthPolicy.isTransientFailure(err)).toBe(false);
    expect(sessionAuthPolicy.shouldAttemptTokenRefresh(err)).toBe(false);
  });

  it('flags refresh 401/403 (non-FORBIDDEN) as logout', () => {
    expect(sessionAuthPolicy.refreshFailureRequiresLogout(httpError(401))).toBe(true);
    expect(sessionAuthPolicy.refreshFailureRequiresLogout(httpError(403))).toBe(true);
    expect(sessionAuthPolicy.refreshFailureRequiresLogout(httpError(500))).toBe(false);
  });
});
