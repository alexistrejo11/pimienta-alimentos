import { computed, inject, Injectable, signal } from '@angular/core';
import { finalize } from 'rxjs';

import { parseApiError, type ParsedApiError } from '../http/parse-api-error';
import { UserProfileService } from '../user/user-profile.service';

/**
 * Sesión del workspace: perfil del usuario autenticado y alcance por sede.
 * Se carga una vez en {@link WorkspaceShellComponent}.
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
          this.roles.set(user.roles);
          this.assignedHeadquarterIds.set(user.assignedHeadquarterIds ?? []);
        },
        error: (err: unknown) => this.error.set(parseApiError(err)),
      });
  }
}
