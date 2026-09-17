import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { finalize } from 'rxjs';

import { SessionContextService } from '../../../../core/auth/session-context.service';
import { HeadquarterLookupService } from '../../../../core/headquarters/headquarter-lookup.service';
import { InventoryService } from '../../../../core/inventory/inventory.service';
import { parseApiError, type ParsedApiError } from '../../../../core/http/parse-api-error';
import type { ItemResponse, StorageLocationResponse } from '../../../../core/model/inventory/inventory.dto';
import { PageHeaderComponent } from '../../../../shared/ui/page-header/page-header';
import { HeadquarterSelectComponent } from '../../../../shared/ui/headquarter-select/headquarter-select';
import { ItemSelectComponent } from '../../../../shared/ui/item-select/item-select';

@Component({
  selector: 'app-inventario-entradas-page',
  imports: [PageHeaderComponent, HeadquarterSelectComponent, ItemSelectComponent, FormsModule],
  templateUrl: './entradas-page.html',
})
export class InventarioEntradasPageComponent implements OnInit {
  private readonly session = inject(SessionContextService);
  private readonly inventory = inject(InventoryService);
  private readonly hqLookup = inject(HeadquarterLookupService);

  readonly loading = signal(false);
  readonly error = signal<ParsedApiError | null>(null);
  readonly success = signal('');
  readonly locations = signal<StorageLocationResponse[]>([]);

  selectedHeadquarterId: number | null = null;
  mode: 'purchase' | 'initial' = 'purchase';
  itemId: number | null = null;
  locationId: number | null = null;
  quantity = 1;
  unitCost = 0;
  reference = '';
  notes = '';

  readonly effectiveHeadquarterId = computed(() =>
    this.session.isAdmin() ? this.selectedHeadquarterId : this.session.activeHeadquarterId(),
  );

  ngOnInit(): void {
    void this.hqLookup.ensureLoaded();
    if (!this.session.isAdmin()) {
      this.selectedHeadquarterId = this.session.activeHeadquarterId();
      this.loadLocations();
    }
  }

  onHeadquarterChange(id: number | number[] | null): void {
    this.selectedHeadquarterId = Array.isArray(id) ? (id[0] ?? null) : id;
    this.session.selectHeadquarter(this.selectedHeadquarterId);
    this.itemId = null;
    this.locationId = null;
    this.unitCost = 0;
    this.error.set(null);
    this.success.set('');
    this.loadLocations();
  }

  onItemChange(id: number | null): void {
    this.itemId = id;
  }

  onItemPicked(item: ItemResponse | null): void {
    if (item && this.mode === 'purchase' && this.unitCost === 0) {
      this.unitCost = item.costPrice;
    }
  }

  loadLocations(): void {
    const hqId = this.effectiveHeadquarterId();
    if (hqId == null) return;
    this.inventory
      .searchLocations({ headquarterId: hqId, page: 0, size: 100 })
      .subscribe((page) => this.locations.set(page.items));
  }

  guardar(): void {
    if (this.loading()) return;
    if (this.itemId == null || this.locationId == null || this.quantity <= 0) return;
    this.error.set(null);
    this.success.set('');
    this.loading.set(true);
    const done = () => this.loading.set(false);
    if (this.mode === 'initial') {
      this.inventory
        .createInitialStock({ itemId: this.itemId, locationId: this.locationId, initialQuantity: this.quantity })
        .pipe(finalize(done))
        .subscribe({
          next: () => {
            this.success.set('Stock inicial registrado.');
             this.resetForm();
          },
          error: (err: unknown) => this.error.set(parseApiError(err)),
        });
      return;
    }
    this.inventory
      .purchase({
        externalReference: this.reference || undefined,
        notes: this.notes || undefined,
        lines: [{ itemId: this.itemId, locationId: this.locationId, quantity: this.quantity, unitCost: this.unitCost }],
      })
      .pipe(finalize(done))
      .subscribe({
        next: () => {
          this.success.set('Entrada de compra registrada.');
             this.resetForm();
        },
        error: (err: unknown) => this.error.set(parseApiError(err)),
      });
  }

  private resetForm(): void {
    this.itemId = null;
    this.locationId = null;
    this.quantity = 1;
    this.unitCost = 0;
    this.reference = '';
    this.notes = '';
  }
}
