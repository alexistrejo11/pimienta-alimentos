import { Component, inject, OnInit, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { finalize } from 'rxjs';

import { InventoryService } from '../../../core/inventory/inventory.service';
import { parseApiError, type ParsedApiError } from '../../../core/http/parse-api-error';
import { itemCategoryLabel, itemUnitLabel } from '../../../core/i18n/enum-labels';
import type { ItemResponse } from '../../../core/model/inventory/inventory.dto';
import type { ItemCategory } from '../../../core/model/inventory/inventory.enums';
import type { PageMetadata } from '../../../core/model/common/pagination';
import { PageHeaderComponent } from '../../../shared/ui/page-header/page-header';
import { DataStateComponent } from '../../../shared/ui/data-state/data-state';
import { ListSearchFieldComponent } from '../../../shared/ui/list-search-field/list-search-field';

@Component({
  selector: 'app-bodega-page',
  imports: [PageHeaderComponent, DataStateComponent, ListSearchFieldComponent, RouterLink],
  templateUrl: './bodega-page.html',
})
export class BodegaPageComponent implements OnInit {
  private readonly inventory = inject(InventoryService);

  readonly loading = signal(true);
  readonly error = signal<ParsedApiError | null>(null);
  readonly items = signal<ItemResponse[]>([]);
  readonly metadata = signal<PageMetadata | null>(null);
  readonly page = signal(0);
  readonly searchDraft = signal('');
  readonly searchApplied = signal('');
  readonly category = signal<ItemCategory | ''>('');
  readonly categories: ItemCategory[] = ['RAW_MATERIAL', 'CONSUMABLE', 'SPARE_PART', 'PACKAGING', 'TOOL', 'MACHINE', 'FURNITURE', 'FINISHED_GOOD', 'OTHER'];

  readonly itemCategoryLabel = itemCategoryLabel;
  readonly itemUnitLabel = itemUnitLabel;

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
        kind: 'WAREHOUSE',
        search: this.searchApplied() || undefined,
        category: this.category() || undefined,
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

  cambiarCategoria(value: string): void { this.category.set(value as ItemCategory | ''); this.cargar(0); }
}
