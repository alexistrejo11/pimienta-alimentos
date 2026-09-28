import { Component, inject, OnInit, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { finalize } from 'rxjs';

import { parseApiError, type ParsedApiError } from '../../../core/http/parse-api-error';
import { itemStatusLabel, itemUnitLabel } from '../../../core/i18n/enum-labels';
import type { PageMetadata } from '../../../core/model/common/pagination';
import type { ProductResponse, ProductStatus } from '../../../core/model/product/product.dto';
import { ProductService } from '../../../core/product/product.service';
import { DataStateComponent } from '../../../shared/ui/data-state/data-state';
import { ListSearchFieldComponent } from '../../../shared/ui/list-search-field/list-search-field';
import { PageHeaderComponent } from '../../../shared/ui/page-header/page-header';

@Component({
  selector: 'app-productos-page',
  imports: [PageHeaderComponent, DataStateComponent, ListSearchFieldComponent, RouterLink],
  templateUrl: './productos-page.html',
})
export class ProductosPageComponent implements OnInit {
  private readonly products = inject(ProductService);

  readonly loading = signal(true);
  readonly error = signal<ParsedApiError | null>(null);
  readonly items = signal<ProductResponse[]>([]);
  readonly metadata = signal<PageMetadata | null>(null);
  readonly page = signal(0);
  readonly searchDraft = signal('');
  readonly searchApplied = signal('');
  readonly status = signal<ProductStatus | ''>('');
  readonly statuses: ProductStatus[] = ['ACTIVE', 'DISCONTINUED'];
  readonly itemStatusLabel = itemStatusLabel;
  readonly itemUnitLabel = itemUnitLabel;

  ngOnInit(): void {
    this.cargar();
  }

  cargar(page = this.page()): void {
    this.page.set(page);
    this.error.set(null);
    this.loading.set(true);
    this.products
      .search({
        page,
        size: 20,
        search: this.searchApplied() || undefined,
        status: this.status() || undefined,
      })
      .pipe(finalize(() => this.loading.set(false)))
      .subscribe({
        next: (result) => {
          this.items.set(result.items);
          this.metadata.set(result.metadata);
        },
        error: (err: unknown) => this.error.set(parseApiError(err)),
      });
  }

  applySearch(term: string): void {
    const normalized = term.trim();
    if (normalized === this.searchApplied()) return;
    this.searchApplied.set(normalized);
    this.searchDraft.set(normalized);
    this.cargar(0);
  }

  siguiente(): void {
    if (this.metadata()?.hasNext) this.cargar(this.page() + 1);
  }

  anterior(): void {
    if (this.metadata()?.hasPrevious) this.cargar(this.page() - 1);
  }

  cambiarEstado(value: string): void {
    this.status.set(value as ProductStatus | '');
    this.cargar(0);
  }
}
