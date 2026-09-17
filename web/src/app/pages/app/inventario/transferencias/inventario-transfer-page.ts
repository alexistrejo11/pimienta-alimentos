import { Component, inject, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { finalize } from 'rxjs';

import { InventoryService } from '../../../../core/inventory/inventory.service';
import { SessionContextService } from '../../../../core/auth/session-context.service';
import { parseApiError, type ParsedApiError } from '../../../../core/http/parse-api-error';
import type { ItemResponse, StorageLocationResponse } from '../../../../core/model/inventory/inventory.dto';
import { HeadquarterSelectComponent } from '../../../../shared/ui/headquarter-select/headquarter-select';
import { PageHeaderComponent } from '../../../../shared/ui/page-header/page-header';
import { ApiFormErrorComponent } from '../../../../shared/ui/api-form-error/api-form-error';
import { ItemSelectComponent } from '../../../../shared/ui/item-select/item-select';

@Component({
  selector: 'app-inventario-transfer-page',
  imports: [FormsModule, HeadquarterSelectComponent, PageHeaderComponent, ApiFormErrorComponent, ItemSelectComponent],
  templateUrl: './inventario-transfer-page.html',
})
export class InventarioTransferPageComponent implements OnInit {
  private readonly inventory = inject(InventoryService);
  private readonly session = inject(SessionContextService);

  readonly headquarterId = signal(0);
  readonly locations = signal<StorageLocationResponse[]>([]);
  readonly error = signal<ParsedApiError | null>(null);
  readonly saving = signal(false);
  readonly success = signal('');

  itemId: number | null = null;
  fromLocationId = 0;
  toLocationId = 0;
  quantity = 1;
  unitCost = 0;
  externalReference = '';
  notes = '';

  ngOnInit(): void {
    const id = Number(this.session.activeHeadquarterId() ?? 0);
    if (id) {
      this.headquarterId.set(id);
      this.load(id);
    }
  }

  load(id: number): void {
    this.inventory.searchLocations({ headquarterId: id, page: 0, size: 100 }).subscribe({
      next: (page) => this.locations.set(page.items),
      error: (err) => this.error.set(parseApiError(err)),
    });
  }

  onHeadquarterChange(value: number | number[] | null): void {
    if (typeof value !== 'number' || !value) return;
    this.headquarterId.set(value);
    this.fromLocationId = 0;
    this.toLocationId = 0;
    this.load(value);
  }

  onItemChange(id: number | null): void {
    this.itemId = id;
  }

  onItemPicked(item: ItemResponse | null): void {
    if (item && this.unitCost === 0) this.unitCost = item.costPrice;
  }

  submit(): void {
    if (this.saving()) return;
    this.error.set(null);
    this.success.set('');
    if (
      this.itemId == null ||
      !this.fromLocationId ||
      !this.toLocationId ||
      this.fromLocationId === this.toLocationId ||
      this.quantity < 1
    ) {
      this.error.set({ message: 'Selecciona ubicaciones distintas y una cantidad válida.' } as ParsedApiError);
      return;
    }
    this.saving.set(true);
    this.inventory
      .transfer({
        externalReference: this.externalReference.trim() || undefined,
        notes: this.notes.trim() || undefined,
        lines: [
          {
            itemId: this.itemId,
            fromLocationId: this.fromLocationId,
            toLocationId: this.toLocationId,
            quantity: this.quantity,
            unitCost: this.unitCost,
          },
        ],
      })
      .pipe(finalize(() => this.saving.set(false)))
      .subscribe({
        next: () => {
          this.success.set('Transferencia registrada correctamente.');
          this.itemId = null;
          this.fromLocationId = 0;
          this.toLocationId = 0;
          this.quantity = 1;
          this.unitCost = 0;
          this.externalReference = '';
          this.notes = '';
        },
        error: (err) => this.error.set(parseApiError(err)),
      });
  }
}
