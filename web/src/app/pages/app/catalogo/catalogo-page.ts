import { Component, inject, OnInit, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { finalize } from 'rxjs';

import { InventoryService } from '../../../core/inventory/inventory.service';
import { parseApiError, type ParsedApiError } from '../../../core/http/parse-api-error';
import { itemCategoryLabel, itemStatusLabel } from '../../../core/i18n/enum-labels';
import type { ItemResponse } from '../../../core/model/inventory/inventory.dto';
import type { CatalogRole, ItemCategory, ItemStatus } from '../../../core/model/inventory/inventory.enums';
import type { PageMetadata } from '../../../core/model/common/pagination';
import { PageHeaderComponent } from '../../../shared/ui/page-header/page-header';
import { DataStateComponent } from '../../../shared/ui/data-state/data-state';

@Component({
  selector: 'app-catalogo-page',
  imports: [PageHeaderComponent, DataStateComponent, RouterLink],
  templateUrl: './catalogo-page.html',
})
export class CatalogoPageComponent implements OnInit {
  private readonly inventory = inject(InventoryService);

  readonly loading = signal(true);
  readonly error = signal<ParsedApiError | null>(null);
  readonly items = signal<ItemResponse[]>([]);
  readonly metadata = signal<PageMetadata | null>(null);
  readonly page = signal(0);
  readonly search = signal('');
  readonly category = signal<ItemCategory | ''>('');
  readonly status = signal<ItemStatus | ''>('');
  readonly categories: ItemCategory[] = ['RAW_MATERIAL', 'FINISHED_GOOD', 'CONSUMABLE', 'SPARE_PART', 'PACKAGING', 'TOOL', 'MACHINE', 'FURNITURE', 'OTHER'];
  readonly statuses: ItemStatus[] = ['ACTIVE', 'DISCONTINUED', 'OUT_OF_STOCK', 'PENDING_APPROVAL'];
  readonly roles: CatalogRole[] = ['INVENTORY_ONLY', 'POS_SELLABLE'];
  readonly role = signal<CatalogRole | ''>('');
  private searchTimer: ReturnType<typeof setTimeout> | undefined;

  readonly itemStatusLabel = itemStatusLabel;
  readonly itemCategoryLabel = itemCategoryLabel;

  ngOnInit(): void {
    this.cargar();
  }

  cargar(page = this.page()): void {
    this.page.set(page);
    this.error.set(null);
    this.loading.set(true);
    this.inventory
       .searchItems({ page, size: 20, search: this.search() || undefined, category: this.category() || undefined, status: this.status() || undefined, catalogRole: this.role() || undefined })
      .pipe(finalize(() => this.loading.set(false)))
      .subscribe({
        next: (result) => { this.items.set(result.items); this.metadata.set(result.metadata); },
        error: (err: unknown) => this.error.set(parseApiError(err)),
      });
  }

  buscar(value: string): void { this.search.set(value); clearTimeout(this.searchTimer); this.searchTimer = setTimeout(() => this.cargar(0), 300); }

  siguiente(): void { if (this.metadata()?.hasNext) this.cargar(this.page() + 1); }

  anterior(): void { if (this.metadata()?.hasPrevious) this.cargar(this.page() - 1); }

  cambiarCategoria(value: string): void { this.category.set(value as ItemCategory | ''); this.cargar(0); }

  cambiarEstado(value: string): void { this.status.set(value as ItemStatus | ''); this.cargar(0); }
  cambiarRol(value: string): void { this.role.set(value as CatalogRole | ''); this.cargar(0); }

  cambiarEstadoItem(item: ItemResponse): void {
    const request = item.status === 'ACTIVE' ? this.inventory.discontinueItem(item.id) : this.inventory.activateItem(item.id);
    request.subscribe({ next: () => this.cargar(), error: (err: unknown) => this.error.set(parseApiError(err)) });
  }

  formatMoney(value: number): string {
    return new Intl.NumberFormat('es-MX', { style: 'currency', currency: 'MXN' }).format(value);
  }
}
