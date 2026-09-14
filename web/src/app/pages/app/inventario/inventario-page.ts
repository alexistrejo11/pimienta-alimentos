import { Component, computed, inject, OnInit, signal } from '@angular/core';

import { SessionContextService } from '../../../core/auth/session-context.service';
import { HeadquarterLookupService } from '../../../core/headquarters/headquarter-lookup.service';
import { InventoryService } from '../../../core/inventory/inventory.service';
import { parseApiError, type ParsedApiError } from '../../../core/http/parse-api-error';
import type { InventoryStockResponse, StorageLocationResponse } from '../../../core/model/inventory/inventory.dto';
import { PageHeaderComponent } from '../../../shared/ui/page-header/page-header';
import { DataStateComponent } from '../../../shared/ui/data-state/data-state';
import { HeadquarterSelectComponent } from '../../../shared/ui/headquarter-select/headquarter-select';

@Component({
  selector: 'app-inventario-page',
  imports: [PageHeaderComponent, DataStateComponent, HeadquarterSelectComponent],
  templateUrl: './inventario-page.html',
})
export class InventarioPageComponent implements OnInit {
  private readonly session = inject(SessionContextService);
  private readonly inventory = inject(InventoryService);
  private readonly hqLookup = inject(HeadquarterLookupService);

  readonly loading = signal(true);
  readonly error = signal<ParsedApiError | null>(null);
  readonly stock = signal<InventoryStockResponse[]>([]);
  readonly locations = signal<StorageLocationResponse[]>([]);

  selectedHeadquarterId: number | null = null;
  selectedLocationId: number | null = null;
  private initialLoad = true;

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
      this.cargarLocations();
      this.cargarStock();
    } else {
      this.loading.set(false);
    }
  }

  onHeadquarterChange(id: number | number[] | null): void {
    const hqId = Array.isArray(id) ? (id[0] ?? null) : id;
    this.selectedHeadquarterId = hqId;
    this.selectedLocationId = null;
    this.session.selectHeadquarter(hqId);
    if (hqId == null) {
      this.stock.set([]);
      this.locations.set([]);
      this.loading.set(false);
      return;
    }
    if (this.initialLoad) {
      this.initialLoad = false;
    }
    this.cargarLocations();
    this.cargarStock();
  }

  onLocationChange(event: Event): void {
    const value = (event.target as HTMLSelectElement).value;
    this.selectedLocationId = value ? Number(value) : null;
    this.cargarStock();
  }

  cargarLocations(): void {
    const hqId = this.effectiveHeadquarterId();
    if (hqId == null) {
      this.locations.set([]);
      return;
    }
    this.inventory.searchLocations({ headquarterId: hqId, page: 0, size: 100 }).subscribe({
      next: (page) => this.locations.set(page.items),
      error: () => this.locations.set([]),
    });
  }

  cargarStock(): void {
    const hqId = this.effectiveHeadquarterId();
    if (hqId == null) {
      this.stock.set([]);
      this.loading.set(false);
      return;
    }

    this.error.set(null);
    this.loading.set(true);
    this.inventory
      .searchStock({
        headquarterId: hqId,
        locationId: this.selectedLocationId ?? undefined,
        page: 0,
        size: 100,
      })
      .subscribe({
        next: (page) => {
          this.stock.set(page.items);
          this.loading.set(false);
        },
        error: (err: unknown) => {
          this.error.set(parseApiError(err));
          this.loading.set(false);
        },
      });
  }
}
