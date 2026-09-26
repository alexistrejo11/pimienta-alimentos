import { Component, inject, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { finalize, forkJoin, type Observable } from 'rxjs';

import { HeadquarterLookupService } from '../../../core/headquarters/headquarter-lookup.service';
import { HeadquarterService } from '../../../core/headquarters/headquarter.service';
import { UserManagementService } from '../../../core/user/user-management.service';
import { AppRole } from '../../../core/model/account/enums';
import type { UserResponse } from '../../../core/model/account/user.dto';
import type { HeadQuarterResponse } from '../../../core/model/headquarter/headquarter.dto';
import { accountStatusLabel, roleLabel } from '../../../core/i18n/enum-labels';
import { parseApiError, type ParsedApiError } from '../../../core/http/parse-api-error';

@Component({
  selector: 'app-usuarios-page',
  imports: [FormsModule],
  templateUrl: './usuarios-page.html',
})
export class UsuariosPageComponent implements OnInit {
  private readonly service = inject(UserManagementService);
  private readonly hqService = inject(HeadquarterService);
  private readonly hqLookup = inject(HeadquarterLookupService);

  readonly AppRole = AppRole;
  readonly roles: AppRole[] = Object.values(AppRole).filter(
    (role) => role !== AppRole.USER && role !== AppRole.SUPPORT && role !== AppRole.SALES,
  );
  private readonly sedeRoles: AppRole[] = [AppRole.DIRECTOR, AppRole.MANAGER, AppRole.EMPLOYEE];
  readonly users = signal<UserResponse[]>([]);
  readonly sedes = signal<HeadQuarterResponse[]>([]);
  readonly loading = signal(true);
  readonly savingId = signal<number | null>(null);
  readonly error = signal<ParsedApiError | null>(null);
  readonly roleWarning = signal<string | null>(null);
  readonly statistics = signal({ totalUsers: 0, activeUsers: 0, bannedUsers: 0 });
  readonly selectedRoles = new Map<number, AppRole[]>();
  readonly selectedHeadquarter = new Map<number, number | null>();

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.loading.set(true);
    this.error.set(null);
    this.roleWarning.set(null);
    this.service.statistics().subscribe({ next: (stats) => this.statistics.set(stats), error: () => {} });
    forkJoin({
      users: this.service.list(),
      sedes: this.hqService.list(0, 200),
    })
      .pipe(finalize(() => this.loading.set(false)))
      .subscribe({
        next: ({ users, sedes }) => {
          this.sedes.set(sedes.content);
          void this.hqLookup.ensureLoaded();
          this.users.set(users.items);
          users.items.forEach((user) => {
            this.selectedRoles.set(
              user.id,
              user.roles.filter((role) => this.roles.includes(role)),
            );
            const assigned = user.assignedHeadquarterIds ?? [];
            this.selectedHeadquarter.set(user.id, assigned.length > 0 ? assigned[0] : null);
          });
        },
        error: (err: unknown) => this.error.set(parseApiError(err)),
      });
  }

  pendingCount(): number {
    return this.users().filter((user) => user.accountStatus === 'PENDING_APPROVAL').length;
  }
  statusLabel(value: string): string {
    return accountStatusLabel(value);
  }
  roleLabel(value: string): string {
    return roleLabel(value);
  }
  rolesFor(user: UserResponse): AppRole[] {
    return this.selectedRoles.get(user.id) ?? [];
  }
  setRoles(user: UserResponse, roles: AppRole[]): void {
    this.selectedRoles.set(user.id, roles);
  }

  showsSede(user: UserResponse): boolean {
    const selected = this.rolesFor(user);
    const saved = user.roles ?? [];
    return [...selected, ...saved].some((role) => this.sedeRoles.includes(role));
  }

  headquarterFor(user: UserResponse): number | null {
    return this.selectedHeadquarter.get(user.id) ?? null;
  }
  setHeadquarter(user: UserResponse, id: number | null): void {
    this.selectedHeadquarter.set(user.id, id);
  }

  assignedHeadquarterLabel(user: UserResponse): string {
    const ids = user.assignedHeadquarterIds ?? [];
    if (ids.length === 0) return '—';
    return ids.map((id) => this.hqLookup.name(id)).join(', ');
  }

  approve(user: UserResponse): void {
    this.run(user, () => this.service.approve(user.id));
  }
  unban(user: UserResponse): void {
    this.run(user, () => this.service.unban(user.id));
  }
  ban(user: UserResponse): void {
    const reason = window.prompt('Motivo de bloqueo')?.trim();
    if (reason === undefined) return;
    this.run(user, () => this.service.ban(user.id, { reason: reason || null }));
  }

  saveRoles(user: UserResponse): void {
    const roles = this.rolesFor(user);
    if (roles.length === 0) return;
    const needsSede = roles.some((role) => this.sedeRoles.includes(role));
    const hqId = this.headquarterFor(user);
    if (needsSede && hqId == null) {
      this.roleWarning.set(
        `${user.email}: asigna una sede antes de guardar Director, Gerente o Empleado.`,
      );
      return;
    }
    this.roleWarning.set(null);
    this.savingId.set(user.id);
    const saves: Observable<unknown>[] = [this.service.replaceRoles(user.id, { roles })];
    if (needsSede && hqId != null) {
      saves.push(this.service.assignHeadquarters(user.id, { headquarterIds: [hqId] }));
    }
    forkJoin(saves)
      .pipe(finalize(() => this.savingId.set(null)))
      .subscribe({
        next: () => this.load(),
        error: (err: unknown) => this.error.set(parseApiError(err)),
      });
  }

  saveHeadquarter(user: UserResponse): void {
    const hqId = this.headquarterFor(user);
    if (this.showsSede(user) && (hqId == null || hqId <= 0)) {
      this.roleWarning.set(`${user.email}: asigna una sede antes de guardar.`);
      return;
    }
    this.roleWarning.set(null);
    const ids = hqId != null && hqId > 0 ? [hqId] : [];
    this.savingId.set(user.id);
    this.service
      .assignHeadquarters(user.id, { headquarterIds: ids })
      .pipe(finalize(() => this.savingId.set(null)))
      .subscribe({
        next: () => this.load(),
        error: (err: unknown) => this.error.set(parseApiError(err)),
      });
  }

  private run(user: UserResponse, operation: () => ReturnType<UserManagementService['approve']>): void {
    this.savingId.set(user.id);
    operation()
      .pipe(finalize(() => this.savingId.set(null)))
      .subscribe({
        next: () => this.load(),
        error: (err: unknown) => this.error.set(parseApiError(err)),
      });
  }
}
