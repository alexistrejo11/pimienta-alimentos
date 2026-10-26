import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { finalize } from 'rxjs';
import { SessionContextService } from '../../../../core/auth/session-context.service';
import { formatDateTime } from '../../../../core/i18n/format-date';
import { InventoryService } from '../../../../core/inventory/inventory.service';
import { parseApiError, type ParsedApiError } from '../../../../core/http/parse-api-error';
import {
  itemCategoryLabel,
  inventoryExitReasonLabel,
  inventoryMovementTypeLabel,
  movementDirectionLabel,
} from '../../../../core/i18n/enum-labels';
import type { InventoryMovementResponse } from '../../../../core/model/inventory/inventory.dto';
import type {
  InventoryExitReason,
  InventoryMovementType,
  ItemCategory,
} from '../../../../core/model/inventory/inventory.enums';
import type { PageMetadata } from '../../../../core/model/common/pagination';
import { PageHeaderComponent } from '../../../../shared/ui/page-header/page-header';
import { DataStateComponent } from '../../../../shared/ui/data-state/data-state';
import { ListSearchFieldComponent } from '../../../../shared/ui/list-search-field/list-search-field';
import { RegistrarMovimientoModalComponent } from './registrar-movimiento-modal';

@Component({ selector: 'app-inventario-ledger-page', imports: [FormsModule, PageHeaderComponent, DataStateComponent, ListSearchFieldComponent, RegistrarMovimientoModalComponent], templateUrl: './inventario-ledger-page.html' })
export class InventarioLedgerPageComponent implements OnInit {
  private readonly inventory = inject(InventoryService);
  private readonly session = inject(SessionContextService);
  readonly rows = signal<InventoryMovementResponse[]>([]);
  readonly metadata = signal<PageMetadata | null>(null);
  readonly loading = signal(true);
  readonly error = signal<ParsedApiError | null>(null);
  readonly registrarAbierto = signal(false);
  readonly notice = signal('');
  readonly canRegister = computed(() => this.session.canOperateOps());
  readonly page = signal(0);
  searchDraft = '';
  searchApplied = '';
  type = '';
  direction = '';
  exitReason = '';
  category = '';
  fromDate = '';
  toDate = '';
  readonly categories: ItemCategory[] = ['RAW_MATERIAL', 'FINISHED_GOOD', 'CONSUMABLE', 'SPARE_PART', 'PACKAGING', 'TOOL', 'MACHINE', 'FURNITURE', 'OTHER'];
  readonly movementTypes: InventoryMovementType[] = ['IN', 'OUT', 'ADJUSTMENT_PLUS', 'ADJUSTMENT_MINUS'];
  readonly exitReasons: InventoryExitReason[] = ['SALE', 'WASTE', 'WITHDRAWAL', 'RETURN_FROM_CLIENT'];
  readonly itemCategoryLabel = itemCategoryLabel;
  readonly movementTypeLabel = inventoryMovementTypeLabel;
  readonly directionLabel = movementDirectionLabel;
  readonly exitReasonLabel = inventoryExitReasonLabel;
  readonly formatDateTime = formatDateTime;

  ngOnInit(): void { this.load(); }
  load(page = this.page()): void {
    this.page.set(page); this.loading.set(true); this.error.set(null);
    this.inventory
      .searchMovements({
        page,
        size: 20,
        search: this.searchApplied || undefined,
        type: (this.type || undefined) as InventoryMovementType | undefined,
        direction: (this.direction || undefined) as 'IN' | 'OUT' | undefined,
        exitReason: (this.exitReason || undefined) as InventoryExitReason | undefined,
        category: (this.category || undefined) as ItemCategory | undefined,
        fromDate: this.fromDate ? `${this.fromDate}T00:00:00` : undefined,
        toDate: this.toDate ? `${this.toDate}T23:59:59` : undefined,
      })
      .pipe(finalize(() => this.loading.set(false)))
      .subscribe({
        next: (result) => {
          this.rows.set(result.items);
          this.metadata.set(result.metadata);
        },
        error: (err: unknown) => this.error.set(parseApiError(err)),
      });
  }
  applySearch(term: string): void {
    const normalized = term.trim();
    if (normalized === this.searchApplied) {
      return;
    }
    this.searchApplied = normalized;
    this.searchDraft = normalized;
    this.load(0);
  }
  filterChanged(): void { this.load(0); }
  previous(): void { if (this.metadata()?.hasPrevious) this.load(this.page() - 1); }
  next(): void { if (this.metadata()?.hasNext) this.load(this.page() + 1); }
  abrirRegistro(): void { this.notice.set(''); this.registrarAbierto.set(true); }
  cerrarRegistro(): void { this.registrarAbierto.set(false); }
  onRegistrado(message: string): void { this.registrarAbierto.set(false); this.notice.set(message); this.load(0); }

  movementDetail(row: InventoryMovementResponse): string {
    return row.description || row.referenceNumber || '';
  }
}
