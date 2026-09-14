import { Component, inject, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { finalize } from 'rxjs';

import { UserManagementService } from '../../../core/user/user-management.service';
import { AppRole } from '../../../core/model/account/enums';
import type { UserResponse } from '../../../core/model/account/user.dto';
import { accountStatusLabel, roleLabel } from '../../../core/i18n/enum-labels';
import { parseApiError, type ParsedApiError } from '../../../core/http/parse-api-error';

@Component({
  selector: 'app-usuarios-page',
  imports: [FormsModule],
  templateUrl: './usuarios-page.html',
})
export class UsuariosPageComponent implements OnInit {
  private readonly service = inject(UserManagementService);
  readonly roles: AppRole[] = Object.values(AppRole).filter(
    (role) => role !== AppRole.USER && role !== AppRole.SUPPORT && role !== AppRole.SALES,
  );
  readonly users = signal<UserResponse[]>([]);
  readonly loading = signal(true);
  readonly savingId = signal<number | null>(null);
  readonly error = signal<ParsedApiError | null>(null);
  readonly statistics = signal({ totalUsers: 0, activeUsers: 0, bannedUsers: 0 });
  readonly selectedRoles = new Map<number, AppRole[]>();

  ngOnInit(): void { this.load(); }

  load(): void {
    this.loading.set(true);
    this.error.set(null);
    this.service.statistics().subscribe({ next: (stats) => this.statistics.set(stats), error: () => {} });
    this.service.list().pipe(finalize(() => this.loading.set(false))).subscribe({
      next: (page) => {
        this.users.set(page.items);
        page.items.forEach((user) => this.selectedRoles.set(user.id, user.roles.filter((role) => this.roles.includes(role))));
      },
      error: (err: unknown) => this.error.set(parseApiError(err)),
    });
  }

  pendingCount(): number { return this.users().filter((user) => user.accountStatus === 'PENDING_APPROVAL').length; }
  statusLabel(value: string): string { return accountStatusLabel(value); }
  roleLabel(value: string): string { return roleLabel(value); }
  rolesFor(user: UserResponse): AppRole[] { return this.selectedRoles.get(user.id) ?? []; }
  setRoles(user: UserResponse, roles: AppRole[]): void { this.selectedRoles.set(user.id, roles); }

  approve(user: UserResponse): void { this.run(user, () => this.service.approve(user.id)); }
  unban(user: UserResponse): void { this.run(user, () => this.service.unban(user.id)); }
  ban(user: UserResponse): void {
    const reason = window.prompt('Motivo de bloqueo')?.trim();
    if (reason === undefined) return;
    this.run(user, () => this.service.ban(user.id, { reason: reason || null }));
  }
  saveRoles(user: UserResponse): void {
    const roles = this.rolesFor(user);
    if (roles.length === 0) return;
    this.savingId.set(user.id);
    this.service.replaceRoles(user.id, { roles }).pipe(finalize(() => this.savingId.set(null))).subscribe({ next: () => this.load(), error: (err: unknown) => this.error.set(parseApiError(err)) });
  }

  private run(user: UserResponse, operation: () => ReturnType<UserManagementService['approve']>): void {
    this.savingId.set(user.id);
    operation().pipe(finalize(() => this.savingId.set(null))).subscribe({ next: () => this.load(), error: (err: unknown) => this.error.set(parseApiError(err)) });
  }
}
