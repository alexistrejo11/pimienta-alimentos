import { computed, inject, Injectable, signal } from '@angular/core';
import { finalize } from 'rxjs';

import { parseApiError, type ParsedApiError } from '../http/parse-api-error';
import { UserProfileService } from '../user/user-profile.service';

/**
 * Sesión del workspace: perfil del usuario autenticado y alcance por sede.
 * Se carga una vez en {@link WorkspaceShellComponent}.
 *
 * `/users/me` returns enum names (`ADMIN`); Spring authorities use `ROLE_ADMIN`.
 * Roles are normalized to the `ROLE_*` form when the profile is loaded.
 */
@Injectable({ providedIn: 'root' })
export class SessionContextService {
  private readonly profileService = inject(UserProfileService);

  readonly loading = signal(true);
  readonly error = signal<ParsedApiError | null>(null);
  readonly roles = signal<string[]>([]);
  readonly assignedHeadquarterIds = signal<number[]>([]);

  readonly isAdmin = computed(() => this.roles().includes('ROLE_ADMIN'));
  readonly isManager = computed(() => this.roles().includes('ROLE_MANAGER'));
  readonly managerHeadquarterId = computed(() => {
    const ids = this.assignedHeadquarterIds();
    if (!this.isManager() || ids.length === 0) {
      return null;
    }
    return ids[0];
  });
  readonly canAccessPos = computed(() => this.isAdmin() || this.isManager());
  readonly ready = computed(() => !this.loading() && this.error() === null);

  load(): void {
    this.loading.set(true);
    this.error.set(null);
    this.profileService
      .getProfile()
      .pipe(finalize(() => this.loading.set(false)))
      .subscribe({
        next: (user) => {
          this.roles.set((user.roles ?? []).map(toSpringRole));
          this.assignedHeadquarterIds.set(user.assignedHeadquarterIds ?? []);
        },
        error: (err: unknown) => this.error.set(parseApiError(err)),
      });
  }

  /** Limpia el estado de sesión (p. ej. tras logout). */
  clear(): void {
    this.roles.set([]);
    this.assignedHeadquarterIds.set([]);
    this.loading.set(false);
    this.error.set(null);
  }
}

/** Maps API enum names (`ADMIN`) to Spring-style authorities (`ROLE_ADMIN`). */
function toSpringRole(role: string): string {
  const trimmed = role.trim();
  if (!trimmed) return trimmed;
  return trimmed.startsWith('ROLE_') ? trimmed : `ROLE_${trimmed}`;
}
