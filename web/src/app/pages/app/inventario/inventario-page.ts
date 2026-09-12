import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { finalize } from 'rxjs';

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
  readonly posLocations = signal<StorageLocationResponse[]>([]);

  selectedHeadquarterId: number | null = null;

  readonly effectiveHeadquarterId = computed(() => {
    if (this.session.isAdmin()) {
      return this.selectedHeadquarterId;
    }
    return this.session.managerHeadquarterId();
  });

  readonly isAdmin = this.session.isAdmin;

  ngOnInit(): void {
    void this.hqLookup.ensureLoaded();
    if (!this.session.isAdmin()) {
      this.selectedHeadquarterId = this.session.managerHeadquarterId();
      this.cargarStock();
    }
  }

  onHeadquarterChange(id: number | number[] | null): void {
    const hqId = Array.isArray(id) ? id[0] ?? null : id;
    this.selectedHeadquarterId = hqId;
    this.cargarStock();
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
      .searchLocations({ type: 'POS', page: 0, size: 100 })
      .pipe(finalize(() => this.loading.set(false)))
      .subscribe({
        next: (page) => {
          this.posLocations.set(page.items);
          const loc = page.items.find((l) => l.headquarterId === hqId);
          if (!loc) {
            this.stock.set([]);
            return;
          }
          this.inventory.searchStock({ locationId: loc.id, page: 0, size: 100 }).subscribe({
            next: (stockPage) => this.stock.set(stockPage.items),
            error: (err: unknown) => this.error.set(parseApiError(err)),
          });
        },
        error: (err: unknown) => this.error.set(parseApiError(err)),
      });
  }
}
