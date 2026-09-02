import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { API_BASE_URL } from '../config/api.config';
import type {
  LoginRequest,
  LogoutRequest,
  RefreshRequest,
  RegisterRequest,
  RegisterResponse,
  TokenResponse,
} from '../model/account/auth.dto';

/**
 * Authentication API calls against `/api/v1/auth`.
 *
 * All `/api/v1/*` HTTP traffic is logged centrally by `apiLoggingInterceptor`
 * (method, path, status, duration, safe summaries — no passwords or tokens).
 *
 * ### RxJS notes (quick refresher)
 * - `HttpClient` methods return a **cold** {@link Observable}: the HTTP call does not start until something
 *   **subscribes** (or the `async` pipe subscribes for you). Each new subscription triggers a new request
 *   unless you use sharing operators (not used here).
 * - A successful response produces **one** `next` emission, then **completes**.
 * - Failed responses (4xx/5xx) go to `subscribe`’s **`error`** callback with an
 *   {@link import('@angular/common/http').HttpErrorResponse}; its `error` property is usually the JSON body
 *   (our {@link ApiErrorResponse} from the global handler) when `Content-Type` is JSON.
 * - {@link import('rxjs').finalize} in the component runs on **both** success and failure—good for turning off
 *   loading flags without duplicating code in `next` and `error`.
 */
@Injectable({
  providedIn: 'root',
})
export class AuthService {
  private readonly http = inject(HttpClient);

  private readonly authUrl = `${API_BASE_URL}/auth`;

  /**
   * Registers a new user (`201`). Response is {@link RegisterResponse} — no tokens until an admin approves the account.
   */
  register(request: RegisterRequest): Observable<RegisterResponse> {
    return this.http.post<RegisterResponse>(`${this.authUrl}/register`, request);
  }

  /**
   * Exchanges email/password for tokens (`POST /api/v1/auth/login`).
   * Same success/error contract as {@link register}.
   */
  login(request: LoginRequest): Observable<TokenResponse> {
    return this.http.post<TokenResponse>(`${this.authUrl}/login`, request);
  }

  /**
   * Emite un nuevo access token reutilizando el refresh token (`POST /api/v1/auth/refresh`).
   * El refresh token no se rota; solo se invalida en logout o al expirar su JWT.
   * El interceptor global también refresca ante {@code 401}; este método sirve para flujos explícitos.
   */
  refresh(request: RefreshRequest): Observable<TokenResponse> {
    return this.http.post<TokenResponse>(`${this.authUrl}/refresh`, request);
  }

  /**
   * Revoca la sesión en el servidor (`POST /api/v1/auth/logout` → {@code 204}).
   * Idempotente; el caller debe limpiar {@code sessionStorage} después.
   */
  logout(request: LogoutRequest = {}): Observable<void> {
    const refreshToken = request.refreshToken ?? sessionStorage.getItem('refreshToken');
    const body: LogoutRequest = refreshToken ? { refreshToken } : {};
    return this.http.post<void>(`${this.authUrl}/logout`, body);
  }
}
