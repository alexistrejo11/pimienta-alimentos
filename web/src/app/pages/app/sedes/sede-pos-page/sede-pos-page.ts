import { Component, inject, OnInit, signal } from '@angular/core';
import { FormBuilder, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';
import { finalize } from 'rxjs';

import { SessionContextService } from '../../../../core/auth/session-context.service';
import { HeadquarterService } from '../../../../core/headquarters/headquarter.service';
import { PosCatalogService } from '../../../../core/headquarters/pos-catalog.service';
import { InventoryService } from '../../../../core/inventory/inventory.service';
import { PosLabelPrintService } from '../../../../core/pos/pos-label-print.service';
import { parseApiError, type ParsedApiError } from '../../../../core/http/parse-api-error';
import { stockPolicyLabel } from '../../../../core/i18n/enum-labels';
import type { ItemResponse } from '../../../../core/model/inventory/inventory.dto';
import type {
  HeadquarterPosCatalogItemResponse,
  PosSaleCategoryResponse,
} from '../../../../core/model/pos/pos.dto';
import type { StockPolicy } from '../../../../core/model/pos/pos.enums';
import { PageHeaderComponent } from '../../../../shared/ui/page-header/page-header';
import { DataStateComponent } from '../../../../shared/ui/data-state/data-state';
import { HeadquarterSelectComponent } from '../../../../shared/ui/headquarter-select/headquarter-select';

@Component({
  selector: 'app-sede-pos-page',
  imports: [PageHeaderComponent, DataStateComponent, ReactiveFormsModule, FormsModule, HeadquarterSelectComponent],
  templateUrl: './sede-pos-page.html',
})
export class SedePosPageComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly session = inject(SessionContextService);
  private readonly hqService = inject(HeadquarterService);
  private readonly posCatalog = inject(PosCatalogService);
  private readonly inventory = inject(InventoryService);
  private readonly fb = inject(FormBuilder);
  private readonly labelPrint = inject(PosLabelPrintService);

  readonly headquarterId = signal(0);
  readonly globalMode = signal(false);
  readonly sedeName = signal('');
  readonly loading = signal(true);
  readonly error = signal<ParsedApiError | null>(null);
  readonly catalog = signal<HeadquarterPosCatalogItemResponse[]>([]);
  readonly masterItems = signal<ItemResponse[]>([]);
  readonly posCandidates = signal<ItemResponse[]>([]);
  readonly saleCategories = signal<PosSaleCategoryResponse[]>([]);
  readonly savingCatalogId = signal<number | null>(null);
  readonly creatingCategory = signal(false);

  readonly stockPolicies: StockPolicy[] = ['CONTROLLED', 'NOT_CONTROLLED'];
  readonly stockPolicyLabel = stockPolicyLabel;

  lookupSku = '';
  candidateId: number | null = null;
  newCategoryName = '';
  readonly editingItemId = signal<number | null>(null);

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
      .listCatalog(id, 0, 100)
      .pipe(finalize(() => this.loading.set(false)))
      .subscribe({
        next: (page) => this.catalog.set(page.items),
        error: (err: unknown) => this.error.set(parseApiError(err)),
      });

    this.posCatalog.listCategories(id).subscribe({
      next: (categories) => this.saleCategories.set(categories),
      error: () => {},
    });

    this.inventory.searchItems({ page: 0, size: 100 }).subscribe({
      next: (page) => this.masterItems.set(page.items),
      error: () => {},
    });

    this.posCatalog.listCandidates(id).subscribe({
      next: (items) => this.posCandidates.set(items),
      error: () => {},
    });
  }

  startEditCatalog(row: HeadquarterPosCatalogItemResponse): void {
    this.editingItemId.set(row.itemId);
    this.catalogForm.patchValue({
      saleCategory: row.saleCategory,
      salePrice: row.salePrice,
      available: row.available,
      stockPolicy: row.stockPolicy,
      negativeStockLimit: row.negativeStockLimit,
    });
  }

  saveCatalogItem(itemId: number): void {
    if (this.catalogForm.invalid) return;
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
          this.editingItemId.set(null);
          this.posCatalog.listCatalog(this.headquarterId(), 0, 100).subscribe({
            next: (page) => this.catalog.set(page.items),
          });
          this.posCatalog.listCandidates(this.headquarterId()).subscribe({
            next: (items) => this.posCandidates.set(items),
          });
        },
        error: (err: unknown) => this.error.set(parseApiError(err)),
      });
  }

  configureCandidate(): void {
    const item = this.posCandidates().find((candidate) => candidate.id === Number(this.candidateId));
    if (!item) return;
    this.editingItemId.set(item.id);
    this.catalogForm.reset({
      saleCategory: this.saleCategories()[0]?.name ?? '',
      salePrice: 0,
      available: true,
      stockPolicy: 'CONTROLLED',
      negativeStockLimit: null,
    });
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

  addFromLookup(): void {
    const q = this.lookupSku.trim();
    if (!q) return;
    this.inventory.lookupItem(q).subscribe({
      next: (item) => {
        this.editingItemId.set(item.id);
        this.catalogForm.reset({
          saleCategory: 'GENERAL',
          salePrice: 0,
          available: true,
          stockPolicy: 'CONTROLLED',
          negativeStockLimit: null,
        });
      },
      error: (err: unknown) => this.error.set(parseApiError(err)),
    });
  }

  itemName(itemId: number): string {
    return this.masterItems().find((i) => i.id === itemId)?.name ?? `Ítem #${itemId}`;
  }

  itemSku(itemId: number): string {
    return this.masterItems().find((i) => i.id === itemId)?.sku ?? '';
  }

  printCatalogLabels(): void {
    this.labelPrint.print(
      this.catalog()
        .map((row) => ({
          sku: this.itemSku(row.itemId),
          name: this.itemName(row.itemId),
          price: row.salePrice,
        }))
        .filter((label) => label.sku),
    );
  }

  printItemLabel(row: HeadquarterPosCatalogItemResponse): void {
    const sku = this.itemSku(row.itemId);
    if (sku) this.labelPrint.print([{ sku, name: this.itemName(row.itemId), price: row.salePrice }]);
  }

  canAccess(): boolean {
    const id = this.headquarterId();
    if (this.session.isAdmin()) return true;
    return this.session.assignedHeadquarterIds().includes(id);
  }
}
