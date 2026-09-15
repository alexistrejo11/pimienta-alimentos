import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { DecimalPipe } from '@angular/common';
import { FormsModule } from '@angular/forms';

import { SessionContextService } from '../../../core/auth/session-context.service';
import { HeadquarterLookupService } from '../../../core/headquarters/headquarter-lookup.service';
import { InventoryService } from '../../../core/inventory/inventory.service';
import { parseApiError, type ParsedApiError } from '../../../core/http/parse-api-error';
import { inventoryStatusLabel, itemCategoryLabel } from '../../../core/i18n/enum-labels';
import type { GlobalInventoryResponse } from '../../../core/model/inventory/inventory.dto';
import type { InventoryStatus, ItemCategory } from '../../../core/model/inventory/inventory.enums';
import { PageHeaderComponent } from '../../../shared/ui/page-header/page-header';
import { DataStateComponent } from '../../../shared/ui/data-state/data-state';
import { HeadquarterSelectComponent } from '../../../shared/ui/headquarter-select/headquarter-select';

@Component({
  selector: 'app-inventario-page',
  imports: [DecimalPipe, FormsModule, PageHeaderComponent, DataStateComponent, HeadquarterSelectComponent],
  templateUrl: './inventario-page.html',
})
export class InventarioPageComponent implements OnInit {
  private readonly session = inject(SessionContextService);
  private readonly inventory = inject(InventoryService);
  private readonly hqLookup = inject(HeadquarterLookupService);

  readonly loading = signal(true);
  readonly error = signal<ParsedApiError | null>(null);
  readonly stock = signal<GlobalInventoryResponse[]>([]);
  readonly metadata = signal<{ pageNumber: number; totalPages: number; totalElements: number; hasNext: boolean; hasPrevious: boolean } | null>(null);
  readonly categories: ItemCategory[] = ['RAW_MATERIAL', 'FINISHED_GOOD', 'CONSUMABLE', 'SPARE_PART', 'PACKAGING', 'TOOL', 'MACHINE', 'FURNITURE', 'OTHER'];
  readonly statuses: InventoryStatus[] = ['NORMAL', 'LOW_STOCK', 'OUT_OF_STOCK'];

  selectedHeadquarterId: number | null = null;
  search = '';
  selectedCategory: ItemCategory | '' = '';
  selectedStatus: InventoryStatus | '' = '';
  minCost: number | undefined;
  maxCost: number | undefined;
  page = 0;
  private initialLoad = true;

  readonly inventoryStatusLabel = inventoryStatusLabel;
  readonly itemCategoryLabel = itemCategoryLabel;

  readonly effectiveHeadquarterId = computed(() => {
    if (this.session.isAdmin()) {
      return this.selectedHeadquarterId;
    }
    return this.session.activeHeadquarterId();
  });

  ngOnInit(): void {
    void this.hqLookup.ensureLoaded();
    if (!this.session.isAdmin()) {
      this.selectedHeadquarterId = this.session.activeHeadquarterId();
      this.cargarStock();
    } else {
      this.loading.set(false);
    }
  }

  onHeadquarterChange(id: number | number[] | null): void {
    const hqId = Array.isArray(id) ? (id[0] ?? null) : id;
    this.selectedHeadquarterId = hqId;
    this.session.selectHeadquarter(hqId);
    if (hqId == null) {
      this.stock.set([]);
      this.loading.set(false);
      return;
    }
    if (this.initialLoad) {
      this.initialLoad = false;
    }
    this.cargarStock();
  }

  onSearch(event: Event): void {
    this.search = (event.target as HTMLInputElement).value;
    this.page = 0;
    this.cargarStock();
  }

  onFilterChange(): void { this.page = 0; this.cargarStock(); }
  setPage(page: number): void { this.page = page; this.cargarStock(); }

  cargarStock(): void {
    const hqId = this.effectiveHeadquarterId();
    this.error.set(null);
    this.loading.set(true);
    this.inventory
      .searchGlobalSummary({
        search: this.search,
        headquarterId: hqId ?? undefined,
        page: this.page,
        size: 20,
        category: this.selectedCategory || undefined,
        status: this.selectedStatus || undefined,
        minCost: this.minCost,
        maxCost: this.maxCost,
      })
      .subscribe({
        next: (page) => {
          this.stock.set(page.items);
          this.metadata.set(page.metadata);
          this.loading.set(false);
        },
        error: (err: unknown) => {
          this.error.set(parseApiError(err));
          this.loading.set(false);
        },
      });
  }
}
