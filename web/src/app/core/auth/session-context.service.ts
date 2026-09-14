import { computed, inject, Injectable, signal } from '@angular/core';
import { catchError, finalize, map, Observable, of, shareReplay, tap, throwError } from 'rxjs';

import { parseApiError, type ParsedApiError } from '../http/parse-api-error';
import { AppRole, type AccountStatus } from '../model/account/enums';
import type { UserResponse } from '../model/account/user.dto';
import { UserProfileService } from '../user/user-profile.service';

export interface SessionContext {
  readonly userId: number;
  readonly accountStatus: AccountStatus;
  readonly roles: AppRole[];
  readonly permissions: string[];
  readonly assignedHeadquarterIds: number[];
}

@Injectable({ providedIn: 'root' })
export class SessionContextService {
  private readonly profileService = inject(UserProfileService);
  private readonly storageKeyPrefix = 'pimienta.active-headquarter.';
  private loaded = false;
  private inFlight: Observable<SessionContext> | null = null;

  readonly loading = signal(false);
  readonly error = signal<ParsedApiError | null>(null);
  readonly accountStatus = signal<AccountStatus | null>(null);
  readonly roles = signal<AppRole[]>([]);
  readonly permissions = signal<string[]>([]);
  readonly assignedHeadquarterIds = signal<number[]>([]);
  readonly activeHeadquarterId = signal<number | null>(null);
  readonly userId = signal(0);

  readonly isAdmin = computed(() => this.roles().includes(AppRole.ADMIN));
  readonly isManager = computed(() => this.roles().includes(AppRole.MANAGER));
  readonly managerHeadquarterId = computed(() => {
    const ids = this.assignedHeadquarterIds();
    return this.isManager() && ids.length > 0 ? ids[0] : null;
  });
  readonly canAccessPos = computed(
    () => this.isAdmin() || this.isManager() || this.roles().includes(AppRole.POS_OPERATOR),
  );
  readonly isUnassigned = computed(
    () => this.roles().length === 1 && this.roles()[0] === AppRole.USER,
  );
  readonly ready = computed(() => this.loaded && this.error() === null);

  ensureLoaded(): Observable<SessionContext> {
    if (this.loaded) {
      return of(this.snapshot());
    }
    if (this.inFlight) {
      return this.inFlight;
    }

    this.loading.set(true);
    this.error.set(null);
    this.inFlight = this.profileService.getProfile().pipe(
      tap((user) => this.setProfile(user)),
      map(() => this.snapshot()),
      tap(() => {
        this.loaded = true;
      }),
      catchError((err: unknown) => {
        this.error.set(parseApiError(err));
        this.loaded = false;
        return throwError(() => err);
      }),
      finalize(() => {
        this.loading.set(false);
        this.inFlight = null;
      }),
      shareReplay({ bufferSize: 1, refCount: false }),
    );
    return this.inFlight;
  }

  clear(): void {
    this.loaded = false;
    this.inFlight = null;
    this.accountStatus.set(null);
    this.roles.set([]);
    this.permissions.set([]);
    this.assignedHeadquarterIds.set([]);
    this.activeHeadquarterId.set(null);
    this.userId.set(0);
    this.loading.set(false);
    this.error.set(null);
  }

  private setProfile(user: UserResponse): void {
    this.userId.set(user.id);
    this.accountStatus.set(user.accountStatus);
    this.roles.set(user.roles ?? []);
    this.permissions.set(user.permissions ?? []);
    const assignedIds = [...new Set(user.assignedHeadquarterIds ?? [])];
    this.assignedHeadquarterIds.set(assignedIds);
    this.activeHeadquarterId.set(this.restoreHeadquarter(assignedIds));
  }

  /** Select a headquarters scope. Administrators may use null for global scope. */
  selectHeadquarter(id: number | null): void {
    if (!this.isAdmin() && (id == null || !this.assignedHeadquarterIds().includes(id))) {
      return;
    }
    this.activeHeadquarterId.set(id);
    this.persistHeadquarter(id);
  }

  private snapshot(): SessionContext {
    return {
      userId: this.userId() || 0,
      accountStatus: this.accountStatus() ?? 'PENDING_APPROVAL',
      roles: [...this.roles()],
      permissions: [...this.permissions()],
      assignedHeadquarterIds: [...this.assignedHeadquarterIds()],
    };
  }

  private restoreHeadquarter(assignedIds: number[]): number | null {
    const stored =
      this.userId() === 0 || typeof sessionStorage === 'undefined'
        ? null
        : sessionStorage.getItem(this.storageKeyPrefix + this.userId());
    const storedId = stored == null ? null : Number(stored);
    if (this.isAdmin()) {
      return storedId != null && Number.isInteger(storedId) && storedId > 0 ? storedId : null;
    }
    return storedId != null && assignedIds.includes(storedId) ? storedId : assignedIds[0] ?? null;
  }

  private persistHeadquarter(id: number | null): void {
    if (this.userId() === 0 || typeof sessionStorage === 'undefined') return;
    const key = this.storageKeyPrefix + this.userId();
    if (id == null) sessionStorage.removeItem(key);
    else sessionStorage.setItem(key, String(id));
  }
}
