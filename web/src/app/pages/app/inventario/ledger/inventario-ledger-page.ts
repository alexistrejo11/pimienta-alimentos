import { Component, inject, OnInit, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { finalize } from 'rxjs';
import { InventoryService } from '../../../../core/inventory/inventory.service';
import { parseApiError, type ParsedApiError } from '../../../../core/http/parse-api-error';
import {
  itemCategoryLabel,
  inventoryExitReasonLabel,
  inventoryMovementTypeLabel,
  movementDirectionLabel,
} from '../../../../core/i18n/enum-labels';
import type { InventoryMovementResponse } from '../../../../core/model/inventory/inventory.dto';
import type { ItemCategory } from '../../../../core/model/inventory/inventory.enums';
import type { PageMetadata } from '../../../../core/model/common/pagination';
import { PageHeaderComponent } from '../../../../shared/ui/page-header/page-header';
import { DataStateComponent } from '../../../../shared/ui/data-state/data-state';

@Component({ selector: 'app-inventario-ledger-page', imports: [FormsModule, DatePipe, PageHeaderComponent, DataStateComponent], templateUrl: './inventario-ledger-page.html' })
export class InventarioLedgerPageComponent implements OnInit {
  private readonly inventory = inject(InventoryService);
  readonly rows = signal<InventoryMovementResponse[]>([]);
  readonly metadata = signal<PageMetadata | null>(null);
  readonly loading = signal(true);
  readonly error = signal<ParsedApiError | null>(null);
  readonly page = signal(0);
  search = ''; type = ''; direction = ''; category = ''; fromDate = ''; toDate = '';
  readonly categories: ItemCategory[] = ['RAW_MATERIAL', 'FINISHED_GOOD', 'CONSUMABLE', 'SPARE_PART', 'PACKAGING', 'TOOL', 'MACHINE', 'FURNITURE', 'OTHER'];
  readonly itemCategoryLabel = itemCategoryLabel;
  readonly movementTypeLabel = inventoryMovementTypeLabel;
  readonly directionLabel = movementDirectionLabel;

  ngOnInit(): void { this.load(); }
  load(page = this.page()): void {
    this.page.set(page); this.loading.set(true); this.error.set(null);
    this.inventory.searchMovements({ page, size: 20, search: this.search || undefined, type: this.type as never || undefined, direction: this.direction as never || undefined, category: this.category as ItemCategory || undefined, fromDate: this.fromDate ? `${this.fromDate}T00:00:00` : undefined, toDate: this.toDate ? `${this.toDate}T23:59:59` : undefined }).pipe(finalize(() => this.loading.set(false))).subscribe({ next: result => { this.rows.set(result.items); this.metadata.set(result.metadata); }, error: err => this.error.set(parseApiError(err)) });
  }
  filterChanged(): void { this.load(0); }
  previous(): void { if (this.metadata()?.hasPrevious) this.load(this.page() - 1); }
  next(): void { if (this.metadata()?.hasNext) this.load(this.page() + 1); }

  movementReason(row: InventoryMovementResponse): string {
    if (row.type === 'SCRAP' && row.description) {
      return inventoryExitReasonLabel(row.description);
    }
    return row.description || row.referenceNumber || 'Sin motivo';
  }
}
