import { Component, inject, OnInit, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { finalize } from 'rxjs';

import { SessionContextService } from '../../../../core/auth/session-context.service';
import { HeadquarterLookupService } from '../../../../core/headquarters/headquarter-lookup.service';
import { PosAdminService } from '../../../../core/pos/pos-admin.service';
import { parseApiError, type ParsedApiError } from '../../../../core/http/parse-api-error';
import { posRoleLabel } from '../../../../core/i18n/enum-labels';
import type { PosOperatorResponse } from '../../../../core/model/pos/pos.dto';
import type { PageMetadata } from '../../../../core/model/common/pagination';
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
  readonly metadata = signal<PageMetadata | null>(null);
  readonly page = signal(0);
  readonly editorState = signal<'closed' | 'create' | 'edit'>('closed');
  readonly editing = signal<PosOperatorResponse | null>(null);
  readonly saving = signal(false);
  readonly showForm = () => this.editorState() !== 'closed';

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

  cargar(page = this.page()): void {
    this.page.set(page);
    this.loadError.set(null);
    this.loading.set(true);
    this.posAdmin
       .listOperators({ page, size: 20 })
      .pipe(finalize(() => this.loading.set(false)))
      .subscribe({
        next: (result) => { this.operators.set(result.items); this.metadata.set(result.metadata); },
        error: (err: unknown) => this.loadError.set(parseApiError(err)),
      });
  }

  siguiente(): void { if (this.metadata()?.hasNext) this.cargar(this.page() + 1); }
  anterior(): void { if (this.metadata()?.hasPrevious) this.cargar(this.page() - 1); }

  openCreate(): void {
    this.editing.set(null);
    this.editorState.set('create');
    this.resetForm();
    this.submitError.set(null);
    this.form.controls.pin.setValidators([Validators.required, Validators.minLength(4)]);
    this.form.controls.pin.updateValueAndValidity();
  }

  cancelEditor(): void {
    if (this.saving()) return;
    this.editorState.set('closed');
    this.editing.set(null);
    this.resetForm();
    this.submitError.set(null);
  }

  private resetForm(): void {
    this.form.reset({
      displayName: '',
      posRole: 'CASHIER',
      pin: '',
      headquarterIds: this.isAdmin() ? [] : this.session.assignedHeadquarterIds().slice(0, 1),
    });
    this.form.markAsPristine();
    this.form.markAsUntouched();
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
    if (this.saving()) return;
    this.saving.set(true);
    const editing = this.editing();
    const request = editing
      ? this.posAdmin.updateOperator(editing.id, {
          displayName: v.displayName,
          posRole: v.posRole,
          pin: v.pin || undefined,
           headquarterIds: v.headquarterIds,
        })
      : this.posAdmin.createOperator({
          displayName: v.displayName,
          posRole: v.posRole,
          pin: v.pin,
          headquarterIds: v.headquarterIds,
        });
    request
       .pipe(finalize(() => this.saving.set(false)))
      .subscribe({
        next: () => {
          this.form.reset({
            displayName: '',
            posRole: 'CASHIER',
            pin: '',
            headquarterIds: this.isAdmin() ? [] : v.headquarterIds,
          });
          this.editorState.set('closed');
          this.editing.set(null);
          this.resetForm();
          this.submitError.set(null);
          this.cargar();
        },
        error: (err: unknown) => {
          this.submitError.set(parseApiError(err));
        },
      });
  }

  editar(operator: PosOperatorResponse): void {
    this.editing.set(operator);
    this.editorState.set('edit');
    this.submitError.set(null);
    this.form.reset({
      displayName: operator.displayName,
      posRole: operator.posRole,
      pin: '',
      headquarterIds: operator.headquarterIds,
    });
    this.form.controls.pin.clearValidators();
    this.form.controls.pin.addValidators(Validators.minLength(4));
    this.form.controls.pin.updateValueAndValidity();
    this.form.markAsPristine();
    this.form.markAsUntouched();
  }

  desactivar(operator: PosOperatorResponse): void {
    if (operator.active && !confirm(`¿Desactivar a ${operator.displayName}?`)) return;
    this.posAdmin.updateOperator(operator.id, { active: !operator.active }).subscribe({
      next: () => this.cargar(),
      error: (err: unknown) => this.loadError.set(parseApiError(err)),
    });
  }

  eliminar(operator: PosOperatorResponse): void {
    if (!confirm(`¿Retirar a ${operator.displayName} de los operadores POS?`)) return;
    this.posAdmin.deleteOperator(operator.id).subscribe({
      next: () => this.cargar(),
      error: (err: unknown) => this.loadError.set(parseApiError(err)),
    });
  }
}
