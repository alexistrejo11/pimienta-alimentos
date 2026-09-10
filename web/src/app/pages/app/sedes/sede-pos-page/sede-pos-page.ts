import { Component, inject, OnInit, signal } from '@angular/core';
import { FormBuilder, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';
import { finalize } from 'rxjs';

import { SessionContextService } from '../../../../core/auth/session-context.service';
import { HeadquarterService } from '../../../../core/headquarters/headquarter.service';
import { PosCatalogService } from '../../../../core/headquarters/pos-catalog.service';
import { InventoryService } from '../../../../core/inventory/inventory.service';
import { parseApiError, type ParsedApiError } from '../../../../core/http/parse-api-error';
import type { ItemResponse } from '../../../../core/model/inventory/inventory.dto';
import type {
  HeadquarterPosCatalogItemResponse,
  PosSettingsResponse,
} from '../../../../core/model/pos/pos.dto';
import type { StockPolicy } from '../../../../core/model/pos/pos.enums';
import { PageHeaderComponent } from '../../../../shared/ui/page-header/page-header';
import { DataStateComponent } from '../../../../shared/ui/data-state/data-state';

@Component({
  selector: 'app-sede-pos-page',
  imports: [PageHeaderComponent, DataStateComponent, ReactiveFormsModule, FormsModule],
  templateUrl: './sede-pos-page.html',
})
export class SedePosPageComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly session = inject(SessionContextService);
  private readonly hqService = inject(HeadquarterService);
  private readonly posCatalog = inject(PosCatalogService);
  private readonly inventory = inject(InventoryService);
  private readonly fb = inject(FormBuilder);

  readonly headquarterId = signal(0);
  readonly sedeName = signal('');
  readonly loading = signal(true);
  readonly error = signal<ParsedApiError | null>(null);
  readonly settings = signal<PosSettingsResponse | null>(null);
  readonly catalog = signal<HeadquarterPosCatalogItemResponse[]>([]);
  readonly masterItems = signal<ItemResponse[]>([]);
  readonly savingSettings = signal(false);
  readonly savingCatalogId = signal<number | null>(null);

  readonly stockPolicies: StockPolicy[] = ['CONTROLLED', 'NOT_CONTROLLED'];

  lookupSku = '';
  readonly editingItemId = signal<number | null>(null);

  readonly settingsForm = this.fb.nonNullable.group({
    currency: ['MXN', Validators.required],
    catalogStaleWarnHours: [24, [Validators.required, Validators.min(1)]],
    catalogStaleBlockHours: [72, [Validators.required, Validators.min(1)]],
    openAmountCategories: [''],
    defaultNegativeStockLimit: [null as number | null],
  });

  readonly catalogForm = this.fb.nonNullable.group({
    saleCategory: ['', Validators.required],
    salePrice: [0, [Validators.required, Validators.min(0)]],
    available: [true],
    stockPolicy: ['CONTROLLED' as StockPolicy, Validators.required],
    negativeStockLimit: [null as number | null],
  });

  ngOnInit(): void {
    const id = Number(this.route.snapshot.paramMap.get('id'));
    this.headquarterId.set(id);
    this.cargar(id);
  }

  cargar(id: number): void {
    this.error.set(null);
    this.loading.set(true);

    this.hqService.getById(id).subscribe({
      next: (hq) => this.sedeName.set(hq.name),
      error: () => {},
    });

    this.posCatalog.getSettings(id).subscribe({
      next: (s) => {
        this.settings.set(s);
        this.settingsForm.patchValue({
          currency: s.currency,
          catalogStaleWarnHours: s.catalogStaleWarnHours,
          catalogStaleBlockHours: s.catalogStaleBlockHours,
          openAmountCategories: (s.openAmountCategories ?? []).join(', '),
          defaultNegativeStockLimit: s.defaultNegativeStockLimit,
        });
      },
      error: (err: unknown) => this.error.set(parseApiError(err)),
    });

    this.posCatalog
      .listCatalog(id, 0, 100)
      .pipe(finalize(() => this.loading.set(false)))
      .subscribe({
        next: (page) => this.catalog.set(page.items),
        error: (err: unknown) => this.error.set(parseApiError(err)),
      });

    this.inventory.searchItems({ page: 0, size: 100 }).subscribe({
      next: (page) => this.masterItems.set(page.items),
      error: () => {},
    });
  }

  saveSettings(): void {
    if (this.settingsForm.invalid) return;
    const v = this.settingsForm.getRawValue();
    this.savingSettings.set(true);
    this.posCatalog
      .updateSettings(this.headquarterId(), {
        currency: v.currency,
        catalogStaleWarnHours: v.catalogStaleWarnHours,
        catalogStaleBlockHours: v.catalogStaleBlockHours,
        openAmountCategories: v.openAmountCategories
          .split(',')
          .map((s) => s.trim())
          .filter(Boolean),
        defaultNegativeStockLimit: v.defaultNegativeStockLimit,
      })
      .pipe(finalize(() => this.savingSettings.set(false)))
      .subscribe({
        next: (s) => this.settings.set(s),
        error: (err: unknown) => this.error.set(parseApiError(err)),
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
          salePrice: item.salePrice,
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

  canAccess(): boolean {
    const id = this.headquarterId();
    if (this.session.isAdmin()) return true;
    return this.session.assignedHeadquarterIds().includes(id);
  }
}
