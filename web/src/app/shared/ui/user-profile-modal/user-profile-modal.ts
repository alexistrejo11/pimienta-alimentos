import { Component, inject, output, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { finalize } from 'rxjs';

import { AuthService } from '../../../core/auth/auth.service';
import { SessionContextService } from '../../../core/auth/session-context.service';
import {
  fieldMessage,
  parseApiError,
  type ParsedApiError,
} from '../../../core/http/parse-api-error';
import type { Gender } from '../../../core/model/account/enums';
import type { UpdateProfileRequest, UserResponse } from '../../../core/model/account/user.dto';
import { genderLabel, roleLabel as toRoleLabel } from '../../../core/i18n/enum-labels';
import { UserProfileService } from '../../../core/user/user-profile.service';

/**
 * Panel lateral: ver / editar perfil del usuario autenticado y cerrar sesión.
 * Se renderiza desde el workspace shell.
 */
@Component({
  selector: 'app-user-profile-modal',
  imports: [ReactiveFormsModule],
  templateUrl: './user-profile-modal.html',
})
export class UserProfileModalComponent {
  readonly cerrar = output<void>();

  private readonly fb = inject(FormBuilder);
  private readonly profileService = inject(UserProfileService);
  private readonly auth = inject(AuthService);
  private readonly session = inject(SessionContextService);
  private readonly router = inject(Router);

  readonly genderOptions: { value: Gender; label: string }[] = (
    ['MALE', 'FEMALE', 'NON_BINARY', 'OTHER', 'PREFER_NOT_TO_SAY'] as Gender[]
  ).map((value) => ({ value, label: genderLabel(value) }));

  readonly loading = signal(true);
  readonly saving = signal(false);
  readonly loggingOut = signal(false);
  readonly saveSuccess = signal(false);
  readonly error = signal<ParsedApiError | null>(null);
  readonly profile = signal<UserResponse | null>(null);

  readonly form = this.fb.nonNullable.group({
    firstName: ['', [Validators.required, Validators.maxLength(120)]],
    lastName: ['', [Validators.required, Validators.maxLength(120)]],
    gender: ['' as Gender | '', [Validators.required]],
    phone: [
      '',
      [
        Validators.required,
        Validators.pattern(/^[+]?[0-9\s().-]{8,31}$/),
        Validators.maxLength(32),
      ],
    ],
    dateOfBirth: ['', [Validators.required]],
  });

  constructor() {
    this.cargar();
  }

  cargar(): void {
    this.error.set(null);
    this.saveSuccess.set(false);
    this.loading.set(true);
    this.profileService
      .getProfile()
      .pipe(finalize(() => this.loading.set(false)))
      .subscribe({
        next: (user) => {
          this.profile.set(user);
          this.form.patchValue({
            firstName: user.firstName,
            lastName: user.lastName,
            gender: user.gender,
            phone: user.phone,
            dateOfBirth: user.dateOfBirth,
          });
        },
        error: (err: unknown) => this.error.set(parseApiError(err)),
      });
  }

  roleLabel(role: string): string {
    return toRoleLabel(role);
  }

  displayName(): string {
    const u = this.profile();
    if (!u) return '';
    return `${u.firstName} ${u.lastName}`.trim();
  }

  apiFieldMessage(field: string): string | undefined {
    const p = this.error();
    if (!p) return undefined;
    return fieldMessage(p, field);
  }

  guardar(): void {
    this.error.set(null);
    this.saveSuccess.set(false);
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const v = this.form.getRawValue();
    const body: UpdateProfileRequest = {
      firstName: v.firstName.trim(),
      lastName: v.lastName.trim(),
      gender: v.gender as Gender,
      phone: v.phone.trim(),
      dateOfBirth: v.dateOfBirth,
    };

    this.saving.set(true);
    this.profileService
      .updateProfile(body)
      .pipe(finalize(() => this.saving.set(false)))
      .subscribe({
        next: (user) => {
          this.profile.set(user);
          this.saveSuccess.set(true);
        },
        error: (err: unknown) => this.error.set(parseApiError(err)),
      });
  }

  /** Revoca refresh en servidor, limpia sesión local y vuelve al login. */
  cerrarSesion(): void {
    if (this.loggingOut()) return;
    this.loggingOut.set(true);
    this.auth
      .logout()
      .pipe(
        finalize(() => {
          sessionStorage.removeItem('accessToken');
          sessionStorage.removeItem('refreshToken');
          this.session.clear();
          this.loggingOut.set(false);
          void this.router.navigate(['/auth/login']);
        }),
      )
      .subscribe({
        next: () => undefined,
        error: () => undefined,
      });
  }
}
