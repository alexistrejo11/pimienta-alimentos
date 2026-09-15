import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { finalize } from 'rxjs';

import { SessionContextService } from '../../../../core/auth/session-context.service';
import { HeadquarterLookupService } from '../../../../core/headquarters/headquarter-lookup.service';
import { InventoryService } from '../../../../core/inventory/inventory.service';
import { parseApiError, type ParsedApiError } from '../../../../core/http/parse-api-error';
import type { ItemResponse, StorageLocationResponse } from '../../../../core/model/inventory/inventory.dto';
import { inventoryExitReasonLabel } from '../../../../core/i18n/enum-labels';
import type { InventoryExitReason } from '../../../../core/model/inventory/inventory.enums';
import { PageHeaderComponent } from '../../../../shared/ui/page-header/page-header';
import { HeadquarterSelectComponent } from '../../../../shared/ui/headquarter-select/headquarter-select';

const EXIT_REASONS: InventoryExitReason[] = [
  'SCRAP',
  'DAMAGED',
  'EXPIRED',
  'INTERNAL_USE',
  'INVENTORY_ADJUSTMENT',
];

@Component({
  selector: 'app-inventario-mermas-page',
  imports: [PageHeaderComponent, HeadquarterSelectComponent, FormsModule],
  templateUrl: './mermas-hq-page.html',
})
export class InventarioMermasPageComponent implements OnInit {
  private readonly session = inject(SessionContextService);
  private readonly inventory = inject(InventoryService);
  private readonly hqLookup = inject(HeadquarterLookupService);

  readonly loading = signal(false);
  readonly error = signal<ParsedApiError | null>(null);
  readonly success = signal('');
  readonly items = signal<ItemResponse[]>([]);
  readonly locations = signal<StorageLocationResponse[]>([]);
  readonly exitReasons = EXIT_REASONS;
  readonly exitReasonLabel = inventoryExitReasonLabel;

  selectedHeadquarterId: number | null = null;
  itemId: number | null = null;
  locationId: number | null = null;
  quantity = 1;
  unitCost = 0;
  exitReason: InventoryExitReason = 'SCRAP';
  detailNotes = '';

  readonly effectiveHeadquarterId = computed(() =>
    this.session.isAdmin() ? this.selectedHeadquarterId : this.session.activeHeadquarterId(),
  );

  ngOnInit(): void {
    void this.hqLookup.ensureLoaded();
    if (!this.session.isAdmin()) {
      this.selectedHeadquarterId = this.session.activeHeadquarterId();
      this.loadCatalog();
    }
  }

  onHeadquarterChange(id: number | number[] | null): void {
    this.selectedHeadquarterId = Array.isArray(id) ? (id[0] ?? null) : id;
    this.session.selectHeadquarter(this.selectedHeadquarterId);
    this.loadCatalog();
  }

  loadCatalog(): void {
    const hqId = this.effectiveHeadquarterId();
    if (hqId == null) return;
    this.inventory.searchItems({ page: 0, size: 100 }).subscribe((page) => this.items.set(page.items));
    this.inventory
      .searchLocations({ headquarterId: hqId, page: 0, size: 100 })
      .subscribe((page) => this.locations.set(page.items));
  }

  registrar(): void {
    if (this.itemId == null || this.locationId == null || this.quantity <= 0 || !this.exitReason) return;
    this.loading.set(true);
    this.error.set(null);
    this.success.set('');
    this.inventory
      .scrap({
        exitReason: this.exitReason,
        notes: this.detailNotes.trim() || undefined,
        lines: [{ itemId: this.itemId, locationId: this.locationId, quantity: this.quantity, unitCost: this.unitCost }],
      })
      .pipe(finalize(() => this.loading.set(false)))
      .subscribe({
        next: () => {
          this.success.set('Merma registrada en inventario HQ.');
          this.detailNotes = '';
          this.quantity = 1;
        },
        error: (err: unknown) => this.error.set(parseApiError(err)),
      });
  }
}
