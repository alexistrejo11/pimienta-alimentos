import { Component, inject, OnInit, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { finalize } from 'rxjs';

import { InventoryService } from '../../../core/inventory/inventory.service';
import { parseApiError, type ParsedApiError } from '../../../core/http/parse-api-error';
import { itemStatusLabel } from '../../../core/i18n/enum-labels';
import type { ItemResponse } from '../../../core/model/inventory/inventory.dto';
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

  readonly itemStatusLabel = itemStatusLabel;

  ngOnInit(): void {
    this.cargar();
  }

  cargar(): void {
    this.error.set(null);
    this.loading.set(true);
    this.inventory
      .searchItems({ page: 0, size: 100 })
      .pipe(finalize(() => this.loading.set(false)))
      .subscribe({
        next: (page) => this.items.set(page.items),
        error: (err: unknown) => this.error.set(parseApiError(err)),
      });
  }

  formatMoney(value: number): string {
    return new Intl.NumberFormat('es-MX', { style: 'currency', currency: 'MXN' }).format(value);
  }
}
