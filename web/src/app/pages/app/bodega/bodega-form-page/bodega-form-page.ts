import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { finalize } from 'rxjs';

import { markFormPristine } from '../../../../core/forms/mark-form-pristine';
import { parseApiError, type ParsedApiError } from '../../../../core/http/parse-api-error';
import { SessionContextService } from '../../../../core/auth/session-context.service';
import { InventoryService } from '../../../../core/inventory/inventory.service';
import { itemCategoryLabel, itemUnitLabel } from '../../../../core/i18n/enum-labels';
import type { ItemCategory, ItemUnit } from '../../../../core/model/inventory/inventory.enums';
import { PageHeaderComponent } from '../../../../shared/ui/page-header/page-header';

const CATEGORIES: ItemCategory[] = [
  'RAW_MATERIAL',
  'CONSUMABLE',
  'SPARE_PART',
  'PACKAGING',
  'TOOL',
  'MACHINE',
  'FURNITURE',
  'FINISHED_GOOD',
  'OTHER',
];

const UNITS: ItemUnit[] = ['PIECE', 'KG', 'GRAM', 'LITER', 'ML', 'BOX', 'DOZEN', 'METER', 'SQUARE_METER'];

@Component({
  selector: 'app-bodega-form-page',
  imports: [ReactiveFormsModule, RouterLink, PageHeaderComponent],
  templateUrl: './bodega-form-page.html',
})
export class BodegaFormPageComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly inventory = inject(InventoryService);
  private readonly session = inject(SessionContextService);

  readonly loading = signal(false);
  readonly loadingExisting = signal(false);
  readonly apiError = signal<ParsedApiError | null>(null);
  readonly itemId = signal<number | null>(null);
  /** Set when the item backs a product; its name is edited from Productos maestros. */
  readonly linkedProductId = signal<number | null>(null);
  readonly isAdmin = this.session.isAdmin;

  readonly categories = CATEGORIES;
  readonly units = UNITS;
  readonly itemCategoryLabel = itemCategoryLabel;
  readonly itemUnitLabel = itemUnitLabel;
  readonly isEdit = computed(() => this.itemId() != null);

  readonly form = this.fb.nonNullable.group({
    name: ['', Validators.required],
    brand: [''],
    category: ['RAW_MATERIAL' as ItemCategory, Validators.required],
    unit: ['PIECE' as ItemUnit, Validators.required],
    description: [''],
  });

  ngOnInit(): void {
    const idParam = this.route.snapshot.paramMap.get('id');
    if (!idParam) return;
    const id = Number(idParam);
    this.itemId.set(id);
    this.loadingExisting.set(true);
    this.inventory
      .getItem(id)
      .pipe(finalize(() => this.loadingExisting.set(false)))
      .subscribe({
        next: (item) => {
          this.linkedProductId.set(item.kind === 'PRODUCT' ? item.productId : null);
          this.form.patchValue({
            name: item.name,
            description: item.description ?? '',
            category: item.category,
            unit: item.unit,
            brand: item.brand ?? '',
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
    const id = this.itemId();
    const body = {
      name: v.name,
      description: v.description.trim() || undefined,
      category: v.category,
      unit: v.unit,
      brand: v.brand.trim() || undefined,
    };
    const request$ = id ? this.inventory.updateItem(id, body) : this.inventory.createItem(body);

    request$.pipe(finalize(() => this.loading.set(false))).subscribe({
      next: () => {
        markFormPristine(this.form);
        void this.router.navigate(['/app/ops/bodega']);
      },
      error: (err: unknown) => this.apiError.set(parseApiError(err)),
    });
  }
}
