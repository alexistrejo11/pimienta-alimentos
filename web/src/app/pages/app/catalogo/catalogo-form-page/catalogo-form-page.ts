import { Component, inject, OnInit, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { finalize } from 'rxjs';

import { InventoryService } from '../../../../core/inventory/inventory.service';
import { parseApiError, type ParsedApiError } from '../../../../core/http/parse-api-error';
import type { ItemCategory, ItemStatus, ItemUnit } from '../../../../core/model/inventory/inventory.enums';
import { PageHeaderComponent } from '../../../../shared/ui/page-header/page-header';

const CATEGORIES: ItemCategory[] = [
  'RAW_MATERIAL',
  'FINISHED_GOOD',
  'CONSUMABLE',
  'SPARE_PART',
  'PACKAGING',
  'TOOL',
  'MACHINE',
  'FURNITURE',
  'OTHER',
];

const UNITS: ItemUnit[] = ['PIECE', 'KG', 'GRAM', 'LITER', 'ML', 'BOX', 'DOZEN', 'METER', 'SQUARE_METER'];

const STATUSES: ItemStatus[] = ['ACTIVE', 'DISCONTINUED', 'OUT_OF_STOCK', 'PENDING_APPROVAL'];

@Component({
  selector: 'app-catalogo-form-page',
  imports: [ReactiveFormsModule, RouterLink, PageHeaderComponent],
  templateUrl: './catalogo-form-page.html',
})
export class CatalogoFormPageComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly inventory = inject(InventoryService);

  readonly loading = signal(false);
  readonly loadingExisting = signal(false);
  readonly apiError = signal<ParsedApiError | null>(null);
  readonly itemId = signal<number | null>(null);

  readonly categories = CATEGORIES;
  readonly units = UNITS;
  readonly statuses = STATUSES;

  readonly form = this.fb.nonNullable.group({
    sku: ['', Validators.required],
    name: ['', Validators.required],
    description: [''],
    costPrice: [0, [Validators.required, Validators.min(0)]],
    salePrice: [0, [Validators.required, Validators.min(0)]],
    category: ['FINISHED_GOOD' as ItemCategory, Validators.required],
    unit: ['PIECE' as ItemUnit, Validators.required],
    reorderPoint: [0, [Validators.required, Validators.min(0)]],
    reorderQuantity: [0, [Validators.required, Validators.min(0)]],
    brand: [''],
    barcode: [''],
    status: ['ACTIVE' as ItemStatus],
  });

  ngOnInit(): void {
    const idParam = this.route.snapshot.paramMap.get('id');
    if (idParam) {
      const id = Number(idParam);
      this.itemId.set(id);
      this.loadingExisting.set(true);
      this.inventory
        .getItem(id)
        .pipe(finalize(() => this.loadingExisting.set(false)))
        .subscribe({
          next: (item) => {
            this.form.patchValue({
              sku: item.sku,
              name: item.name,
              description: item.description ?? '',
              costPrice: item.costPrice,
              salePrice: item.salePrice,
              category: item.category,
              unit: item.unit,
              reorderPoint: item.reorderPoint,
              reorderQuantity: item.reorderQuantity,
              brand: item.brand ?? '',
              barcode: item.barcode ?? '',
              status: item.status,
            });
          },
          error: (err: unknown) => this.apiError.set(parseApiError(err)),
        });
    }
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

    const request$ = id
      ? this.inventory.updateItem(id, {
          sku: v.sku,
          name: v.name,
          description: v.description || undefined,
          costPrice: v.costPrice,
          salePrice: v.salePrice,
          category: v.category,
          unit: v.unit,
          reorderPoint: v.reorderPoint,
          reorderQuantity: v.reorderQuantity,
          brand: v.brand || undefined,
          barcode: v.barcode || undefined,
          status: v.status,
        })
      : this.inventory.createItem({
          sku: v.sku,
          name: v.name,
          description: v.description || undefined,
          costPrice: v.costPrice,
          salePrice: v.salePrice,
          category: v.category,
          unit: v.unit,
          reorderPoint: v.reorderPoint,
          reorderQuantity: v.reorderQuantity,
          brand: v.brand || undefined,
          barcode: v.barcode || undefined,
        });

    request$.pipe(finalize(() => this.loading.set(false))).subscribe({
      next: () => this.router.navigate(['/app/catalogo']),
      error: (err: unknown) => this.apiError.set(parseApiError(err)),
    });
  }

}
