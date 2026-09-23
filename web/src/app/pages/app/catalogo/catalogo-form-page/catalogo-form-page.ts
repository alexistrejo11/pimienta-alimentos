import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { finalize } from 'rxjs';

import { markFormPristine } from '../../../../core/forms/mark-form-pristine';
import { InventoryService } from '../../../../core/inventory/inventory.service';
import { parseApiError, type ParsedApiError } from '../../../../core/http/parse-api-error';
import { itemCategoryLabel, itemStatusLabel, itemUnitLabel, catalogRoleLabel } from '../../../../core/i18n/enum-labels';
import type { CatalogRole, ItemCategory, ItemStatus, ItemUnit } from '../../../../core/model/inventory/inventory.enums';
import { PageHeaderComponent } from '../../../../shared/ui/page-header/page-header';

export type ItemCreateKind = 'pos' | 'warehouse';

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
  readonly createKind = signal<ItemCreateKind>('pos');
  readonly detailsOpen = signal(false);
  readonly editSku = signal('');
  readonly editCatalogRole = signal<CatalogRole>('INVENTORY_ONLY');

  readonly categories = CATEGORIES;
  readonly units = UNITS;
  readonly statuses = STATUSES;
  readonly itemStatusLabel = itemStatusLabel;
  readonly itemCategoryLabel = itemCategoryLabel;
  readonly itemUnitLabel = itemUnitLabel;
  readonly catalogRoleLabel = catalogRoleLabel;

  readonly isEdit = computed(() => this.itemId() != null);
  readonly isPosCreate = computed(() => !this.isEdit() && this.createKind() === 'pos');
  readonly isWarehouseCreate = computed(() => !this.isEdit() && this.createKind() === 'warehouse');

  readonly pageTitle = computed(() => {
    if (this.isEdit()) return 'Editar producto';
    return this.isPosCreate() ? 'Nuevo producto para POS' : 'Nuevo producto de almacén';
  });

  readonly pageSubtitle = computed(() => {
    if (this.isEdit()) return 'Catálogo maestro de inventario';
    if (this.isPosCreate()) {
      return 'El precio de venta se configura en el surtido de cada sede, no aquí.';
    }
    return 'Para existencias, entradas y movimientos de almacén.';
  });

  readonly form = this.fb.nonNullable.group({
    name: ['', Validators.required],
    brand: [''],
    barcode: [''],
    costPrice: [0, [Validators.required, Validators.min(0)]],
    category: ['FINISHED_GOOD' as ItemCategory, Validators.required],
    unit: ['PIECE' as ItemUnit, Validators.required],
    reorderPoint: [0, [Validators.required, Validators.min(0)]],
    reorderQuantity: [0, [Validators.required, Validators.min(0)]],
    description: [''],
    status: ['ACTIVE' as ItemStatus],
  });

  ngOnInit(): void {
    const kind = this.route.snapshot.data['itemKind'] as ItemCreateKind | undefined;
    if (kind === 'pos' || kind === 'warehouse') {
      this.createKind.set(kind);
      if (kind === 'warehouse') {
        this.form.patchValue({ category: 'RAW_MATERIAL' });
      }
    }

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
            this.editSku.set(item.sku);
            this.editCatalogRole.set(item.catalogRole);
            this.form.patchValue({
              name: item.name,
              description: item.description ?? '',
              costPrice: item.costPrice,
              category: item.category,
              unit: item.unit,
              reorderPoint: item.reorderPoint,
              reorderQuantity: item.reorderQuantity,
              brand: item.brand ?? '',
              barcode: item.barcode ?? '',
              status: item.status,
            });
            const hasDetails =
              item.costPrice !== 0 ||
              item.reorderPoint !== 0 ||
              item.reorderQuantity !== 0 ||
              !!(item.description?.trim());
            this.detailsOpen.set(hasDetails);
            markFormPristine(this.form);
          },
          error: (err: unknown) => this.apiError.set(parseApiError(err)),
        });
    }
  }

  toggleDetails(): void {
    this.detailsOpen.update((open) => !open);
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
    const catalogRole: CatalogRole = id
      ? this.editCatalogRole()
      : this.isPosCreate()
        ? 'POS_SELLABLE'
        : 'INVENTORY_ONLY';

    const request$ = id
      ? this.inventory.updateItem(id, {
          sku: this.editSku(),
          name: v.name,
          description: v.description.trim() || undefined,
          costPrice: v.costPrice,
          category: v.category,
          unit: v.unit,
          reorderPoint: v.reorderPoint,
          reorderQuantity: v.reorderQuantity,
          brand: v.brand.trim() || undefined,
          barcode: v.barcode.trim() || undefined,
          status: v.status,
          catalogRole,
        })
      : this.inventory.createItem({
          name: v.name,
          costPrice: v.costPrice,
          category: v.category,
          unit: v.unit,
          reorderPoint: v.reorderPoint,
          reorderQuantity: v.reorderQuantity,
          brand: v.brand.trim() || undefined,
          barcode: v.barcode.trim() || undefined,
          catalogRole,
          ...(this.isWarehouseCreate() && v.description.trim() ? { description: v.description.trim() } : {}),
        });

    request$.pipe(finalize(() => this.loading.set(false))).subscribe({
      next: () => {
        markFormPristine(this.form);
        void this.router.navigate(['/app/ops/catalogo']);
      },
      error: (err: unknown) => this.apiError.set(parseApiError(err)),
    });
  }
}
