import { Component, inject, OnInit, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { finalize } from 'rxjs';

import { InventoryService } from '../../../core/inventory/inventory.service';
import { parseApiError, type ParsedApiError } from '../../../core/http/parse-api-error';
import { itemCategoryLabel, itemKindLabel, itemStatusLabel } from '../../../core/i18n/enum-labels';
import type { ItemResponse } from '../../../core/model/inventory/inventory.dto';
import type { ItemCategory, ItemKind, ItemStatus } from '../../../core/model/inventory/inventory.enums';
import type { PageMetadata } from '../../../core/model/common/pagination';
import { PageHeaderComponent } from '../../../shared/ui/page-header/page-header';
import { DataStateComponent } from '../../../shared/ui/data-state/data-state';
import { ListSearchFieldComponent } from '../../../shared/ui/list-search-field/list-search-field';
import { StockSettingsModalComponent } from './stock-settings-modal';

@Component({
  selector: 'app-catalogo-page',
  imports: [PageHeaderComponent, DataStateComponent, ListSearchFieldComponent, RouterLink, StockSettingsModalComponent],
  templateUrl: './catalogo-page.html',
})
export class CatalogoPageComponent implements OnInit {
  private readonly inventory = inject(InventoryService);

  readonly loading = signal(true);
  readonly error = signal<ParsedApiError | null>(null);
  readonly items = signal<ItemResponse[]>([]);
  readonly metadata = signal<PageMetadata | null>(null);
  readonly page = signal(0);
  readonly searchDraft = signal('');
  readonly searchApplied = signal('');
  readonly kind = signal<ItemKind | ''>('');
  readonly category = signal<ItemCategory | ''>('');
  readonly status = signal<ItemStatus | ''>('');
  readonly editing = signal<ItemResponse | null>(null);

  readonly kinds: ItemKind[] = ['PRODUCT', 'WAREHOUSE'];
  readonly categories: ItemCategory[] = ['RAW_MATERIAL', 'CONSUMABLE', 'SPARE_PART', 'PACKAGING', 'TOOL', 'MACHINE', 'FURNITURE', 'FINISHED_GOOD', 'OTHER'];
  readonly statuses: ItemStatus[] = ['ACTIVE', 'DISCONTINUED', 'OUT_OF_STOCK', 'PENDING_APPROVAL'];

  readonly itemStatusLabel = itemStatusLabel;
  readonly itemCategoryLabel = itemCategoryLabel;
  readonly itemKindLabel = itemKindLabel;

  ngOnInit(): void {
    this.cargar();
  }

  cargar(page = this.page()): void {
    this.page.set(page);
    this.error.set(null);
    this.loading.set(true);
    this.inventory
      .searchItems({
        page,
        size: 20,
        search: this.searchApplied() || undefined,
        kind: this.kind() || undefined,
        category: this.kind() === 'WAREHOUSE' ? this.category() || undefined : undefined,
        status: this.status() || undefined,
      })
      .pipe(finalize(() => this.loading.set(false)))
      .subscribe({
        next: (result) => { this.items.set(result.items); this.metadata.set(result.metadata); },
        error: (err: unknown) => this.error.set(parseApiError(err)),
      });
  }

  applySearch(term: string): void {
    const normalized = term.trim();
    if (normalized === this.searchApplied()) {
      return;
    }
    this.searchApplied.set(normalized);
    this.searchDraft.set(normalized);
    this.cargar(0);
  }

  siguiente(): void { if (this.metadata()?.hasNext) this.cargar(this.page() + 1); }

  anterior(): void { if (this.metadata()?.hasPrevious) this.cargar(this.page() - 1); }

  cambiarTipo(value: string): void {
    this.kind.set(value as ItemKind | '');
    if (value !== 'WAREHOUSE') this.category.set('');
    this.cargar(0);
  }

  cambiarCategoria(value: string): void { this.category.set(value as ItemCategory | ''); this.cargar(0); }

  cambiarEstado(value: string): void { this.status.set(value as ItemStatus | ''); this.cargar(0); }

  cambiarEstadoItem(item: ItemResponse): void {
    const request = item.status === 'ACTIVE' ? this.inventory.discontinueItem(item.id) : this.inventory.activateItem(item.id);
    request.subscribe({ next: () => this.cargar(), error: (err: unknown) => this.error.set(parseApiError(err)) });
  }

  onStockSettingsSaved(): void {
    this.editing.set(null);
    this.cargar();
  }

  identityLink(item: ItemResponse): (string | number)[] {
    return item.kind === 'PRODUCT' && item.productId != null
      ? ['/app/ops/productos', item.productId, 'editar']
      : ['/app/ops/bodega', item.id, 'editar'];
  }

  formatMoney(value: number): string {
    return new Intl.NumberFormat('es-MX', { style: 'currency', currency: 'MXN' }).format(value);
  }
}
