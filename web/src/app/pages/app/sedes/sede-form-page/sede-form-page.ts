import { Component, inject, OnInit, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { finalize } from 'rxjs';

import { HeadquarterService } from '../../../../core/headquarters/headquarter.service';
import { markFormPristine } from '../../../../core/forms/mark-form-pristine';
import {
  fieldMessage,
  parseApiError,
  type ParsedApiError,
} from '../../../../core/http/parse-api-error';
import type { HeadQuarterRequest } from '../../../../core/model/headquarter/headquarter.dto';
import { PageHeaderComponent } from '../../../../shared/ui/page-header/page-header';

/**
 * Formulario único create/edit de sede.
 * Rutas: `/app/sedes/nueva` y `/app/sedes/:id/editar` (ROLE_ADMIN).
 */
@Component({
  selector: 'app-sede-form-page',
  imports: [ReactiveFormsModule, RouterLink, PageHeaderComponent],
  templateUrl: './sede-form-page.html',
})
export class SedeFormPageComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly service = inject(HeadquarterService);

  readonly loading = signal(false);
  readonly loadingExisting = signal(false);
  readonly apiError = signal<ParsedApiError | null>(null);
  readonly sedeId = signal<number | null>(null);

  readonly form = this.fb.nonNullable.group({
    name: ['', [Validators.required, Validators.maxLength(512)]],
    address: ['', [Validators.maxLength(1024)]],
    description: ['', [Validators.maxLength(4096)]],
  });

  get isEdit(): boolean {
    return this.sedeId() != null;
  }

  ngOnInit(): void {
    const isEditRoute = this.route.snapshot.url.some((u) => u.path === 'editar');
    const idRaw = isEditRoute ? this.route.snapshot.paramMap.get('id') : null;
    if (!idRaw) return;

    const id = Number(idRaw);
    if (!Number.isFinite(id)) return;

    this.sedeId.set(id);
    this.loadingExisting.set(true);
    this.service
      .getById(id)
      .pipe(finalize(() => this.loadingExisting.set(false)))
      .subscribe({
        next: (sede) => {
          this.form.patchValue({
            name: sede.name ?? '',
            address: sede.address ?? '',
            description: sede.description ?? '',
          });
          markFormPristine(this.form);
        },
        error: (err: unknown) => this.apiError.set(parseApiError(err)),
      });
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

    const v = this.form.getRawValue();
    const body: HeadQuarterRequest = {
      name: v.name.trim(),
      address: v.address.trim() || null,
      description: v.description.trim() || null,
    };

    const id = this.sedeId();
    this.loading.set(true);
    const request$ = id != null ? this.service.update(id, body) : this.service.create(body);

    request$.pipe(finalize(() => this.loading.set(false))).subscribe({
      next: (sede) => {
        markFormPristine(this.form);
        void this.router.navigate(['/app/sedes', sede.id]);
      },
      error: (err: unknown) => this.apiError.set(parseApiError(err)),
    });
  }
}
