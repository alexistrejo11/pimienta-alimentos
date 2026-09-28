import { Component, inject, OnInit, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { finalize } from 'rxjs';

import { SessionContextService } from '../../../../core/auth/session-context.service';
import { markFormPristine } from '../../../../core/forms/mark-form-pristine';
import { HeadquarterLookupService } from '../../../../core/headquarters/headquarter-lookup.service';
import {
  fieldMessage,
  parseApiError,
  type ParsedApiError,
} from '../../../../core/http/parse-api-error';
import type {
  SupplierHeadquarterAssignment,
  UpsertSupplierRequest,
} from '../../../../core/model/supplier/supplier.dto';
import { SupplierService } from '../../../../core/suppliers/supplier.service';
import { ApiFormErrorComponent } from '../../../../shared/ui/api-form-error/api-form-error';
import { HeadquarterSelectComponent } from '../../../../shared/ui/headquarter-select/headquarter-select';
import { PageHeaderComponent } from '../../../../shared/ui/page-header/page-header';

@Component({
  selector: 'app-proveedor-form-page',
  imports: [
    ReactiveFormsModule,
    RouterLink,
    PageHeaderComponent,
    ApiFormErrorComponent,
    HeadquarterSelectComponent,
  ],
  templateUrl: './proveedor-form-page.html',
})
export class ProveedorFormPageComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly service = inject(SupplierService);
  private readonly session = inject(SessionContextService);
  private readonly hqLookup = inject(HeadquarterLookupService);

  readonly loading = signal(false);
  readonly loadingExisting = signal(false);
  readonly apiError = signal<ParsedApiError | null>(null);
  readonly supplierId = signal<number | null>(null);
  readonly headquarters = signal<SupplierHeadquarterAssignment[]>([]);

  readonly form = this.fb.nonNullable.group({
    name: ['', [Validators.required, Validators.maxLength(200)]],
    phone: ['', [Validators.required, Validators.maxLength(40)]],
    brand: ['', [Validators.required, Validators.maxLength(120)]],
  });

  get isEdit(): boolean {
    return this.supplierId() != null;
  }

  ngOnInit(): void {
    void this.hqLookup.ensureLoaded();
    const isEditRoute = this.route.snapshot.url.some((u) => u.path === 'editar');
    const idRaw = isEditRoute ? this.route.snapshot.paramMap.get('id') : null;

    this.session.ensureLoaded().subscribe({
      next: () => {
        if (!idRaw) {
          this.prefillHeadquartersForCreate();
          return;
        }
        const id = Number(idRaw);
        if (!Number.isFinite(id)) return;
        this.supplierId.set(id);
        this.loadSupplier(id);
      },
    });
  }

  headquarterIds(): number[] {
    return this.headquarters().map((row) => row.headquarterId);
  }

  headquarterLabel(id: number): string {
    return this.hqLookup.name(id);
  }

  private prefillHeadquartersForCreate(): void {
    if (this.session.isAdmin()) {
      const active = this.session.activeHeadquarterId();
      if (active != null) {
        this.headquarters.set([{ headquarterId: active, active: true }]);
      }
      return;
    }
    const assigned = this.session.assignedHeadquarterIds();
    const active = this.session.activeHeadquarterId();
    if (active != null && assigned.includes(active)) {
      this.headquarters.set([{ headquarterId: active, active: true }]);
    } else if (assigned.length > 0) {
      this.headquarters.set([{ headquarterId: assigned[0], active: true }]);
    }
  }

  private loadSupplier(id: number): void {
    this.loadingExisting.set(true);
    this.service
      .getById(id)
      .pipe(finalize(() => this.loadingExisting.set(false)))
      .subscribe({
        next: (supplier) => {
          this.form.patchValue({
            name: supplier.name,
            phone: supplier.phone,
            brand: supplier.brand,
          });
          this.headquarters.set(supplier.headquarters.map((row) => ({ ...row })));
          markFormPristine(this.form);
        },
        error: (err: unknown) => this.apiError.set(parseApiError(err)),
      });
  }

  onHeadquartersChange(ids: number | number[] | null): void {
    const nextIds = Array.isArray(ids) ? ids : ids != null ? [ids] : [];
    const previous = new Map(this.headquarters().map((row) => [row.headquarterId, row.active]));
    this.headquarters.set(
      nextIds.map((headquarterId) => ({
        headquarterId,
        active: previous.get(headquarterId) ?? true,
      })),
    );
  }

  setSupplyActive(headquarterId: number, active: boolean): void {
    this.headquarters.update((rows) =>
      rows.map((row) => (row.headquarterId === headquarterId ? { ...row, active } : row)),
    );
  }

  apiFieldMessage(field: string): string | undefined {
    const p = this.apiError();
    if (!p) return undefined;
    return fieldMessage(p, field);
  }

  submit(): void {
    this.apiError.set(null);
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const headquarters = this.headquarters();
    if (headquarters.length === 0) {
      this.apiError.set({
        message: 'Selecciona al menos una sede.',
        httpStatus: 400,
        traceId: null,
        errorCode: 'VALIDATION',
        fieldErrors: [{ field: 'headquarters', message: 'Requerido.' }],
        context: null,
        rawBody: null,
      });
      return;
    }

    const v = this.form.getRawValue();
    const body: UpsertSupplierRequest = {
      name: v.name.trim(),
      phone: v.phone.trim(),
      brand: v.brand.trim(),
      headquarters,
    };

    const id = this.supplierId();
    this.loading.set(true);
    const request$ = id != null ? this.service.update(id, body) : this.service.create(body);

    request$.pipe(finalize(() => this.loading.set(false))).subscribe({
      next: () => {
        markFormPristine(this.form);
        void this.router.navigate(['/app/ops/proveedores']);
      },
      error: (err: unknown) => this.apiError.set(parseApiError(err)),
    });
  }
}
