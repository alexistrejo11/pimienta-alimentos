import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { FormBuilder, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { EMPTY, expand, finalize, reduce } from 'rxjs';

import { SessionContextService } from '../../../../core/auth/session-context.service';
import { markFormPristine } from '../../../../core/forms/mark-form-pristine';
import { HeadquarterService } from '../../../../core/headquarters/headquarter.service';
import { PosCatalogService } from '../../../../core/headquarters/pos-catalog.service';
import { PosLabelPrintService } from '../../../../core/pos/pos-label-print.service';
import { parseApiError, type ParsedApiError } from '../../../../core/http/parse-api-error';
import { stockPolicyLabel } from '../../../../core/i18n/enum-labels';
import type { ItemResponse } from '../../../../core/model/inventory/inventory.dto';
import type {
  HeadquarterPosCatalogItemResponse,
  PosSaleCategoryResponse,
} from '../../../../core/model/pos/pos.dto';
import type { StockPolicy } from '../../../../core/model/pos/pos.enums';
import type { PageMetadata } from '../../../../core/model/common/pagination';
import { PageHeaderComponent } from '../../../../shared/ui/page-header/page-header';
import { DataStateComponent } from '../../../../shared/ui/data-state/data-state';
import { HeadquarterSelectComponent } from '../../../../shared/ui/headquarter-select/headquarter-select';
import { ItemSelectComponent } from '../../../../shared/ui/item-select/item-select';

@Component({
  selector: 'app-sede-pos-page',
  imports: [
    PageHeaderComponent,
    DataStateComponent,
    ReactiveFormsModule,
    FormsModule,
    RouterLink,
    HeadquarterSelectComponent,
    ItemSelectComponent,
  ],
  templateUrl: './sede-pos-page.html',
})
export class SedePosPageComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly session = inject(SessionContextService);
  private readonly hqService = inject(HeadquarterService);
  private readonly posCatalog = inject(PosCatalogService);
  private readonly fb = inject(FormBuilder);
  private readonly labelPrint = inject(PosLabelPrintService);

  readonly headquarterId = signal(0);
  readonly globalMode = signal(false);
  readonly sedeName = signal('');
  readonly loading = signal(true);
  readonly error = signal<ParsedApiError | null>(null);
  readonly catalog = signal<HeadquarterPosCatalogItemResponse[]>([]);
  readonly catalogMetadata = signal<PageMetadata | null>(null);
  readonly catalogPage = signal(0);
  readonly saleCategories = signal<PosSaleCategoryResponse[]>([]);
  readonly savingCatalogId = signal<number | null>(null);
  readonly creatingCategory = signal(false);
  readonly catalogSearch = signal('');
  readonly catalogCategory = signal('');
  readonly catalogAvailability = signal('');
  readonly catalogStockPolicy = signal('');
  readonly printingLabels = signal(false);
  readonly printNotice = signal<string | null>(null);

  readonly stockPolicies: StockPolicy[] = ['CONTROLLED', 'NOT_CONTROLLED'];
  readonly stockPolicyLabel = stockPolicyLabel;
  readonly canEditCatalog = computed(() => this.session.isAdmin() || this.session.isManager());
  private catalogSearchTimer: ReturnType<typeof setTimeout> | undefined;

  newCategoryName = '';
  readonly editingItemId = signal<number | null>(null);
  readonly pendingItemId = signal<number | null>(null);
  readonly pendingItemLabel = signal('');

  readonly catalogForm = this.fb.nonNullable.group({
    saleCategory: ['', Validators.required],
    salePrice: [0, [Validators.required, Validators.min(0)]],
    available: [true],
    stockPolicy: ['CONTROLLED' as StockPolicy, Validators.required],
    negativeStockLimit: [null as number | null],
  });

  ngOnInit(): void {
    const routeId = this.route.snapshot.paramMap.get('id');
    const id = routeId ? Number(routeId) : Number(this.session.activeHeadquarterId() ?? 0);
    this.globalMode.set(!routeId);
    this.headquarterId.set(id);
    if (id > 0) this.cargar(id);
    else this.loading.set(false);
  }

  onHeadquarterChange(value: number | number[] | null): void {
    if (!this.globalMode() || typeof value !== 'number' || value === this.headquarterId()) return;
    this.catalogPage.set(0);
    this.cancelCatalogEdit();
    this.headquarterId.set(value);
    this.cargar(value);
  }

  cargar(id: number): void {
    this.error.set(null);
    this.loading.set(true);

    this.hqService.getById(id).subscribe({
      next: (hq) => this.sedeName.set(hq.name),
      error: () => {},
    });

    this.posCatalog
      .listCatalog(id, this.catalogPage(), 20, {
        search: this.catalogSearch(),
        saleCategory: this.catalogCategory(),
        available: this.catalogAvailability() === '' ? undefined : this.catalogAvailability() === 'true',
        stockPolicy: this.catalogStockPolicy() || undefined,
      })
      .pipe(finalize(() => this.loading.set(false)))
      .subscribe({
        next: (page) => {
          this.catalog.set(page.items);
          this.catalogMetadata.set(page.metadata);
        },
        error: (err: unknown) => this.error.set(parseApiError(err)),
      });

    this.posCatalog.listCategories(id).subscribe({
      next: (categories) => this.saleCategories.set(categories),
      error: () => {},
    });
  }

  previousCatalogPage(): void {
    if (this.catalogMetadata()?.hasPrevious) {
      this.catalogPage.update((page) => page - 1);
      this.cargar(this.headquarterId());
    }
  }

  nextCatalogPage(): void {
    if (this.catalogMetadata()?.hasNext) {
      this.catalogPage.update((page) => page + 1);
      this.cargar(this.headquarterId());
    }
  }

  onCatalogFilterChange(): void {
    this.catalogPage.set(0);
    this.cargar(this.headquarterId());
  }

  onCatalogSearchChange(value: string): void {
    this.catalogSearch.set(value);
    if (this.catalogSearchTimer) clearTimeout(this.catalogSearchTimer);
    this.catalogSearchTimer = setTimeout(() => this.onCatalogFilterChange(), 300);
  }

  cancelCatalogEdit(): void {
    this.editingItemId.set(null);
    this.pendingItemId.set(null);
    this.pendingItemLabel.set('');
    this.catalogForm.reset({
      saleCategory: '',
      salePrice: 0,
      available: true,
      stockPolicy: 'CONTROLLED',
      negativeStockLimit: null,
    });
    markFormPristine(this.catalogForm);
  }

  removeCatalogItem(row: HeadquarterPosCatalogItemResponse): void {
    const name = row.itemName || this.itemName(row.itemId);
    if (
      !confirm(
        `¿Quitar «${name}» del catálogo POS de esta sede?\n\nDejará de aparecer en el POS tras el próximo sync. El artículo maestro del inventario no se borra.`,
      )
    ) {
      return;
    }
    this.posCatalog.deleteCatalogItem(this.headquarterId(), row.itemId).subscribe({
      next: () => {
        this.printNotice.set(
          `«${name}» se quitó de la sede. El POS lo eliminará en el próximo sync.`,
        );
        this.cargar(this.headquarterId());
      },
      error: (err: unknown) => this.error.set(parseApiError(err)),
    });
  }

  startEditCatalog(row: HeadquarterPosCatalogItemResponse): void {
    this.editingItemId.set(row.itemId);
    this.pendingItemId.set(null);
    this.pendingItemLabel.set('');
    this.catalogForm.reset({
      saleCategory: row.saleCategory,
      salePrice: row.salePrice,
      available: row.available,
      stockPolicy: row.stockPolicy,
      negativeStockLimit: row.negativeStockLimit,
    });
    markFormPristine(this.catalogForm);
  }

  saveCatalogItem(itemId: number): void {
    if (this.catalogForm.invalid) {
      this.catalogForm.markAllAsTouched();
      return;
    }
    const v = this.catalogForm.getRawValue();
    this.savingCatalogId.set(itemId);
    this.posCatalog
      .upsertCatalogItem(this.headquarterId(), itemId, {
        saleCategory: v.saleCategory,
        salePrice: v.salePrice,
        available: v.available,
        stockPolicy: v.stockPolicy,
        negativeStockLimit: v.negativeStockLimit,
      })
      .pipe(finalize(() => this.savingCatalogId.set(null)))
      .subscribe({
        next: () => {
          this.cancelCatalogEdit();
          this.cargar(this.headquarterId());
        },
        error: (err: unknown) => this.error.set(parseApiError(err)),
      });
  }

  onPendingItemChange(id: number | null): void {
    this.pendingItemId.set(id);
    if (id == null) this.pendingItemLabel.set('');
  }

  onPendingItemPicked(item: ItemResponse | null): void {
    if (!item) {
      this.pendingItemLabel.set('');
      return;
    }
    this.pendingItemLabel.set(`${item.sku} · ${item.name}`);
  }

  configurePendingItem(): void {
    const itemId = this.pendingItemId();
    if (itemId == null) return;
    this.editingItemId.set(itemId);
    this.catalogForm.reset({
      saleCategory: this.saleCategories()[0]?.name ?? '',
      salePrice: 0,
      available: true,
      stockPolicy: 'CONTROLLED',
      negativeStockLimit: null,
    });
    markFormPristine(this.catalogForm);
  }

  createCategory(): void {
    const name = this.newCategoryName.trim();
    if (!name) return;
    this.creatingCategory.set(true);
    this.posCatalog
      .createCategory(this.headquarterId(), name, this.saleCategories().length)
      .pipe(finalize(() => this.creatingCategory.set(false)))
      .subscribe({
        next: (category) => {
          this.saleCategories.update((items) => [...items, category]);
          this.newCategoryName = '';
        },
        error: (err: unknown) => this.error.set(parseApiError(err)),
      });
  }

  itemName(itemId: number): string {
    const fromCatalog = this.catalog().find((row) => row.itemId === itemId)?.itemName;
    if (fromCatalog) return fromCatalog;
    if (this.editingItemId() === itemId && this.pendingItemLabel()) {
      const parts = this.pendingItemLabel().split(' · ');
      return parts.length > 1 ? parts.slice(1).join(' · ') : this.pendingItemLabel();
    }
    return `Ítem #${itemId}`;
  }

  itemSku(itemId: number): string {
    return this.catalog().find((row) => row.itemId === itemId)?.itemSku ?? '';
  }

  printCatalogLabels(): void {
    const id = this.headquarterId();
    if (id <= 0 || this.printingLabels()) return;

    this.printNotice.set(null);
    this.printingLabels.set(true);

    const filters = {
      search: this.catalogSearch(),
      saleCategory: this.catalogCategory(),
      available: this.catalogAvailability() === '' ? undefined : this.catalogAvailability() === 'true',
      stockPolicy: this.catalogStockPolicy() || undefined,
    };
    const pageSize = 100;

    this.posCatalog
      .listCatalog(id, 0, pageSize, filters)
      .pipe(
        expand((page) =>
          page.metadata.hasNext
            ? this.posCatalog.listCatalog(id, page.metadata.pageNumber + 1, pageSize, filters)
            : EMPTY,
        ),
        reduce(
          (acc, page) => acc.concat(page.items),
          [] as HeadquarterPosCatalogItemResponse[],
        ),
        finalize(() => this.printingLabels.set(false)),
      )
      .subscribe({
        next: (rows) => {
          const labels = rows
            .map((row) => ({
              sku: row.itemSku || '',
              name: row.itemName || this.itemName(row.itemId),
              price: row.salePrice,
            }))
            .filter((label) => label.sku);
          const skipped = rows.length - labels.length;
          if (!labels.length) {
            this.printNotice.set(
              skipped > 0
                ? 'Ningún producto del filtro tiene SKU para imprimir.'
                : 'No hay productos para imprimir con el filtro actual.',
            );
            return;
          }
          if (skipped > 0) {
            this.printNotice.set(
              `Se omitieron ${skipped} producto${skipped === 1 ? '' : 's'} sin SKU.`,
            );
          }
          void this.labelPrint.print(labels);
        },
        error: (err: unknown) => this.error.set(parseApiError(err)),
      });
  }

  printItemLabel(row: HeadquarterPosCatalogItemResponse): void {
    const sku = row.itemSku || this.itemSku(row.itemId);
    if (!sku) {
      this.printNotice.set('Este producto no tiene SKU; no se puede imprimir la etiqueta.');
      return;
    }
    this.printNotice.set(null);
    void this.labelPrint.print([
      { sku, name: row.itemName || this.itemName(row.itemId), price: row.salePrice },
    ]);
  }

  canAccess(): boolean {
    const id = this.headquarterId();
    if (this.session.isAdmin()) return true;
    return this.session.assignedHeadquarterIds().includes(id);
  }
}
