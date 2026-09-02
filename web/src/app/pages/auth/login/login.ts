import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { finalize } from 'rxjs';

import { AuthService } from '../../../core/auth/auth.service';
import {
  fieldMessage,
  parseApiError,
  type ParsedApiError,
} from '../../../core/http/parse-api-error';
import type { LoginRequest } from '../../../core/model/account/auth.dto';

@Component({
  selector: 'app-login',
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './login.html',
})
export class Login {
  private readonly fb = inject(FormBuilder);
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);

  readonly submitting = signal(false);
  readonly apiError = signal<ParsedApiError | null>(null);

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
      .pipe(finalize(() => this.submitting.set(false)))
      .subscribe({
        next: (tokens) => {
          sessionStorage.setItem('accessToken', tokens.accessToken);
          sessionStorage.setItem('refreshToken', tokens.refreshToken);
          const returnUrl = this.route.snapshot.queryParamMap.get('returnUrl');
          const safeReturn =
            returnUrl &&
            returnUrl.startsWith('/') &&
            !returnUrl.startsWith('//') &&
            !returnUrl.includes(':')
              ? returnUrl
              : '/app/dashboard';
          void this.router.navigateByUrl(safeReturn);
        },
        error: (err: unknown) => {
          this.apiError.set(parseApiError(err));
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
