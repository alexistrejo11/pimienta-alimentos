import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { finalize, switchMap } from 'rxjs';

import { AuthService } from '../../../core/auth/auth.service';
import { authTokenStorage } from '../../../core/auth/auth-token.storage';
import { authUserMessage } from '../../../core/auth/auth-user-message';
import { SessionContextService } from '../../../core/auth/session-context.service';
import { coerceWorkspaceUrl } from '../../../core/auth/workspace-area';
import {
  fieldMessage,
  parseApiError,
  type ParsedApiError,
} from '../../../core/http/parse-api-error';
import type { LoginRequest } from '../../../core/model/account/auth.dto';
import { BRAND_LOGO_URL, LANDING_COVER_IMAGE } from '../../home/brand';

@Component({
  selector: 'app-login',
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './login.html',
})
export class Login {
  private readonly fb = inject(FormBuilder);
  private readonly auth = inject(AuthService);
  private readonly session = inject(SessionContextService);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);

  readonly logoUrl = BRAND_LOGO_URL;
  readonly coverImageUrl = LANDING_COVER_IMAGE;
  readonly submitting = signal(false);
  readonly apiError = signal<ParsedApiError | null>(null);
  readonly apiErrorMessage = signal<string | null>(null);

  /**
   * Mirrors backend {@link LoginRequest}: {@code email} + {@code password} with Bean Validation
   * ({@code @NotBlank}, {@code @Email} on email).
   */
  readonly form = this.fb.nonNullable.group({
    email: ['', [Validators.required, Validators.email]],
    password: ['', [Validators.required]],
  });

  submit(): void {
    this.apiError.set(null);
    this.apiErrorMessage.set(null);
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const v = this.form.getRawValue();
    const request: LoginRequest = {
      email: v.email.trim().toLowerCase(),
      password: v.password,
    };

    this.submitting.set(true);

    this.auth
      .login(request)
      .pipe(
        switchMap((tokens) => {
          this.session.clear();
          authTokenStorage.setTokens(tokens.accessToken, tokens.refreshToken);
          return this.session.ensureLoaded();
        }),
        finalize(() => this.submitting.set(false)),
      )
      .subscribe({
        next: (context) => {
          const returnUrl = this.route.snapshot.queryParamMap.get('returnUrl');
          const target = coerceWorkspaceUrl(returnUrl, {
            roles: context.roles,
            accountStatus: context.accountStatus,
          });
          void this.router.navigateByUrl(target);
        },
        error: (err: unknown) => {
          const parsed = parseApiError(err);
          if (parsed.errorCode === 'ACCOUNT_PENDING_APPROVAL') {
            void this.router.navigate(['/auth/pendiente-aprobacion']);
            return;
          }
          if (parsed.traceId) {
            console.warn('[auth/login]', parsed.errorCode, parsed.traceId);
          }
          this.apiError.set(parsed);
          this.apiErrorMessage.set(authUserMessage(parsed, 'login'));
        },
      });
  }

  apiFieldMessage(field: string): string | undefined {
    const p = this.apiError();
    if (!p) {
      return undefined;
    }
    return fieldMessage(p, field);
  }
}
