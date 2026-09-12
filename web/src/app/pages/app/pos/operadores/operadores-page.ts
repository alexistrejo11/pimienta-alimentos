import { Component, inject, OnInit, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { finalize } from 'rxjs';

import { SessionContextService } from '../../../../core/auth/session-context.service';
import { HeadquarterLookupService } from '../../../../core/headquarters/headquarter-lookup.service';
import { PosAdminService } from '../../../../core/pos/pos-admin.service';
import { parseApiError, type ParsedApiError } from '../../../../core/http/parse-api-error';
import { posRoleLabel } from '../../../../core/i18n/enum-labels';
import type { PosOperatorResponse } from '../../../../core/model/pos/pos.dto';
import type { PosRole } from '../../../../core/model/pos/pos.enums';
import { ApiFormErrorComponent } from '../../../../shared/ui/api-form-error/api-form-error';
import { HeadquarterSelectComponent } from '../../../../shared/ui/headquarter-select/headquarter-select';
import { PageHeaderComponent } from '../../../../shared/ui/page-header/page-header';
import { DataStateComponent } from '../../../../shared/ui/data-state/data-state';

const POS_ROLES: PosRole[] = ['CASHIER', 'MANAGER', 'SUPERADMIN'];

function atLeastOneHq(ids: number[] | null | undefined): boolean {
  return Array.isArray(ids) && ids.length > 0;
}

@Component({
  selector: 'app-operadores-page',
  imports: [
    PageHeaderComponent,
    DataStateComponent,
    ReactiveFormsModule,
    HeadquarterSelectComponent,
    ApiFormErrorComponent,
  ],
  templateUrl: './operadores-page.html',
})
export class OperadoresPageComponent implements OnInit {
  private readonly posAdmin = inject(PosAdminService);
  private readonly session = inject(SessionContextService);
  private readonly lookup = inject(HeadquarterLookupService);
  private readonly fb = inject(FormBuilder);

  readonly loading = signal(true);
  readonly loadError = signal<ParsedApiError | null>(null);
  readonly submitError = signal<ParsedApiError | null>(null);
  readonly operators = signal<PosOperatorResponse[]>([]);
  readonly creating = signal(false);
  readonly showForm = signal(false);

  readonly posRoles = POS_ROLES;
  readonly posRoleLabel = posRoleLabel;
  readonly isAdmin = this.session.isAdmin;
  readonly hqNames = this.lookup.namesList.bind(this.lookup);

  readonly form = this.fb.nonNullable.group({
    displayName: ['', Validators.required],
    posRole: ['CASHIER' as PosRole, Validators.required],
    pin: ['', [Validators.required, Validators.minLength(4)]],
    headquarterIds: this.fb.nonNullable.control<number[]>([], {
      validators: [(c) => (atLeastOneHq(c.value) ? null : { required: true })],
    }),
  });

  ngOnInit(): void {
    void this.lookup.ensureLoaded();
    this.cargar();
  }

  cargar(): void {
    this.loadError.set(null);
    this.loading.set(true);
    this.posAdmin
      .listOperators({ page: 0, size: 50 })
      .pipe(finalize(() => this.loading.set(false)))
      .subscribe({
        next: (page) => this.operators.set(page.items),
        error: (err: unknown) => this.loadError.set(parseApiError(err)),
      });
  }

  toggleForm(): void {
    this.showForm.update((v) => !v);
    if (!this.showForm()) {
      this.submitError.set(null);
    }
  }

  onHeadquartersChange(value: number | number[] | null): void {
    const ids = Array.isArray(value) ? value : value != null ? [value] : [];
    this.form.controls.headquarterIds.setValue(ids);
    this.form.controls.headquarterIds.markAsTouched();
  }

  crear(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    const v = this.form.getRawValue();
    this.submitError.set(null);
    this.creating.set(true);
    this.posAdmin
      .createOperator({
        displayName: v.displayName,
        posRole: v.posRole,
        pin: v.pin,
        headquarterIds: v.headquarterIds,
      })
      .pipe(finalize(() => this.creating.set(false)))
      .subscribe({
        next: () => {
          this.form.reset({
            displayName: '',
            posRole: 'CASHIER',
            pin: '',
            headquarterIds: this.isAdmin() ? [] : v.headquarterIds,
          });
          this.showForm.set(false);
          this.cargar();
        },
        error: (err: unknown) => this.submitError.set(parseApiError(err)),
      });
  }
}
