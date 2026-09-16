import { Component, inject, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { finalize } from 'rxjs';

import { InventoryService } from '../../../../core/inventory/inventory.service';
import { parseApiError, type ParsedApiError } from '../../../../core/http/parse-api-error';
import type { ItemResponse, StorageLocationResponse } from '../../../../core/model/inventory/inventory.dto';
import { PageHeaderComponent } from '../../../../shared/ui/page-header/page-header';

@Component({
  selector: 'app-inventario-ajustes-page',
  imports: [PageHeaderComponent, FormsModule],
  templateUrl: './ajustes-page.html',
})
export class InventarioAjustesPageComponent implements OnInit {
  private readonly inventory = inject(InventoryService);

  readonly loading = signal(false);
  readonly error = signal<ParsedApiError | null>(null);
  readonly success = signal('');
  readonly items = signal<ItemResponse[]>([]);
  readonly locations = signal<StorageLocationResponse[]>([]);

  itemId: number | null = null;
  locationId: number | null = null;
  newQuantity = 0;
  reason = '';

  ngOnInit(): void {
    this.inventory.searchItems({ page: 0, size: 100 }).subscribe((page) => this.items.set(page.items));
    this.inventory.searchLocations({ page: 0, size: 100 }).subscribe((page) => this.locations.set(page.items));
  }

  ajustar(): void {
    if (this.loading()) return;
    if (this.itemId == null || this.locationId == null || !this.reason.trim()) return;
    this.loading.set(true);
    this.error.set(null);
    this.success.set('');
    this.inventory
      .adjustment({
        lines: [
          {
            itemId: this.itemId,
            locationId: this.locationId,
            newQuantity: this.newQuantity,
            reason: this.reason.trim(),
          },
        ],
      })
      .pipe(finalize(() => this.loading.set(false)))
      .subscribe({
        next: () => { this.success.set('Ajuste registrado.'); this.itemId = null; this.locationId = null; this.newQuantity = 0; this.reason = ''; },
        error: (err: unknown) => this.error.set(parseApiError(err)),
      });
  }
}
