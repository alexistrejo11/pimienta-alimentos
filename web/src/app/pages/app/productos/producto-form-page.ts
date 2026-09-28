import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { finalize } from 'rxjs';

import { markFormPristine } from '../../../core/forms/mark-form-pristine';
import { parseApiError, type ParsedApiError } from '../../../core/http/parse-api-error';
import { itemStatusLabel, itemUnitLabel } from '../../../core/i18n/enum-labels';
import type { ItemUnit } from '../../../core/model/inventory/inventory.enums';
import type { ProductStatus } from '../../../core/model/product/product.dto';
import { ProductService } from '../../../core/product/product.service';
import { PageHeaderComponent } from '../../../shared/ui/page-header/page-header';

const UNITS: ItemUnit[] = ['PIECE', 'KG', 'GRAM', 'LITER', 'ML', 'BOX', 'DOZEN', 'METER', 'SQUARE_METER'];
const STATUSES: ProductStatus[] = ['ACTIVE', 'DISCONTINUED'];

@Component({
  selector: 'app-producto-form-page',
  imports: [ReactiveFormsModule, RouterLink, PageHeaderComponent],
  templateUrl: './producto-form-page.html',
})
export class ProductoFormPageComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly products = inject(ProductService);

  readonly loading = signal(false);
  readonly loadingExisting = signal(false);
  readonly apiError = signal<ParsedApiError | null>(null);
  readonly productId = signal<number | null>(null);
  readonly sku = signal('');

  readonly units = UNITS;
  readonly statuses = STATUSES;
  readonly itemStatusLabel = itemStatusLabel;
  readonly itemUnitLabel = itemUnitLabel;
  readonly isEdit = computed(() => this.productId() != null);

  readonly form = this.fb.nonNullable.group({
    name: ['', Validators.required],
    barcode: [''],
    unit: ['PIECE' as ItemUnit, Validators.required],
    description: [''],
    trackStock: [false],
    status: ['ACTIVE' as ProductStatus],
  });

  ngOnInit(): void {
    const idParam = this.route.snapshot.paramMap.get('id');
    if (!idParam) return;
    const id = Number(idParam);
    this.productId.set(id);
    this.loadingExisting.set(true);
    this.products
      .get(id)
      .pipe(finalize(() => this.loadingExisting.set(false)))
      .subscribe({
        next: (product) => {
          this.sku.set(product.sku);
          this.form.patchValue({
            name: product.name,
            barcode: product.barcode ?? '',
            unit: product.unit,
            description: product.description ?? '',
            trackStock: product.trackStock,
            status: product.status,
          });
          markFormPristine(this.form);
        },
        error: (err: unknown) => this.apiError.set(parseApiError(err)),
      });
  }

  submit(): void {
    this.apiError.set(null);
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.loading.set(true);
    const v = this.form.getRawValue();
    const id = this.productId();
    const barcode = v.barcode.trim() || undefined;
    const description = v.description.trim() || undefined;
    const request$ = id
      ? this.products.update(id, {
          name: v.name,
          description,
          unit: v.unit,
          barcode,
          status: v.status,
          trackStock: v.trackStock,
        })
      : this.products.create({
          name: v.name,
          description,
          unit: v.unit,
          barcode,
          trackStock: v.trackStock,
        });

    request$.pipe(finalize(() => this.loading.set(false))).subscribe({
      next: () => {
        markFormPristine(this.form);
        void this.router.navigate(['/app/ops/productos']);
      },
      error: (err: unknown) => this.apiError.set(parseApiError(err)),
    });
  }
}
