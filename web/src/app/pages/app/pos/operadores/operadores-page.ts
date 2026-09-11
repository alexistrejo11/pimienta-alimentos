import { Component, inject, OnInit, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { finalize } from 'rxjs';

import { SessionContextService } from '../../../../core/auth/session-context.service';
import { PosAdminService } from '../../../../core/pos/pos-admin.service';
import { parseApiError, type ParsedApiError } from '../../../../core/http/parse-api-error';
import { posRoleLabel } from '../../../../core/i18n/enum-labels';
import type { PosOperatorResponse } from '../../../../core/model/pos/pos.dto';
import type { PosRole } from '../../../../core/model/pos/pos.enums';
import { PageHeaderComponent } from '../../../../shared/ui/page-header/page-header';
import { DataStateComponent } from '../../../../shared/ui/data-state/data-state';

const POS_ROLES: PosRole[] = ['CASHIER', 'MANAGER', 'SUPERADMIN'];

@Component({
  selector: 'app-operadores-page',
  imports: [PageHeaderComponent, DataStateComponent, ReactiveFormsModule],
  templateUrl: './operadores-page.html',
})
export class OperadoresPageComponent implements OnInit {
  private readonly posAdmin = inject(PosAdminService);
  private readonly session = inject(SessionContextService);
  private readonly fb = inject(FormBuilder);

  readonly loading = signal(true);
  readonly error = signal<ParsedApiError | null>(null);
  readonly operators = signal<PosOperatorResponse[]>([]);
  readonly creating = signal(false);
  readonly showForm = signal(false);

  readonly posRoles = POS_ROLES;
  readonly posRoleLabel = posRoleLabel;
  readonly managerHqId = this.session.managerHeadquarterId;

  readonly form = this.fb.nonNullable.group({
    displayName: ['', Validators.required],
    posRole: ['CASHIER' as PosRole, Validators.required],
    pin: ['', [Validators.required, Validators.minLength(4)]],
  });

  ngOnInit(): void {
    this.cargar();
  }

  cargar(): void {
    this.error.set(null);
    this.loading.set(true);
    this.posAdmin
      .listOperators(0, 50)
      .pipe(finalize(() => this.loading.set(false)))
      .subscribe({
        next: (page) => this.operators.set(page.items),
        error: (err: unknown) => this.error.set(parseApiError(err)),
      });
  }

  toggleForm(): void {
    this.showForm.update((v) => !v);
  }

  crear(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    const v = this.form.getRawValue();
    const hqIds = this.managerHqId() != null ? [this.managerHqId()!] : undefined;

    this.creating.set(true);
    this.posAdmin
      .createOperator({
        displayName: v.displayName,
        posRole: v.posRole,
        pin: v.pin,
        headquarterIds: hqIds,
      })
      .pipe(finalize(() => this.creating.set(false)))
      .subscribe({
        next: () => {
          this.form.reset({ displayName: '', posRole: 'CASHIER', pin: '' });
          this.showForm.set(false);
          this.cargar();
        },
        error: (err: unknown) => this.error.set(parseApiError(err)),
      });
  }
}
