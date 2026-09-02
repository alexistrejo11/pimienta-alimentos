import type { Gender } from './enums';

/** POST /api/v1/auth/register */
export interface RegisterRequest {
  firstName: string;
  lastName: string;
  gender: Gender;
  email: string;
  phone: string;
  password: string;
  /** ISO date (yyyy-MM-dd). */
  dateOfBirth: string;
}

/**
 * POST /api/v1/auth/register — 201.
 * La cuenta queda pendiente de aprobación; no se emiten tokens.
 */
export interface RegisterResponse {
  message: string;
  /** Ej.: `PENDING_APPROVAL` */
  status: string;
}

/** POST /api/v1/auth/login */
export interface LoginRequest {
  email: string;
  password: string;
}

/** POST /api/v1/auth/refresh */
export interface RefreshRequest {
  refreshToken: string;
}

/** POST /api/v1/auth/logout (optional body) */
export interface LogoutRequest {
  refreshToken?: string | null;
}

/** Response from login; refresh returns a new access token and echoes the same refresh token. */
export interface TokenResponse {
  accessToken: string;
  refreshToken: string;
  tokenType: string;
  expiresInSeconds: number;
}
