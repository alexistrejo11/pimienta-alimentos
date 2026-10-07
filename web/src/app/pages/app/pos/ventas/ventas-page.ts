import { DatePipe } from '@angular/common';
import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { forkJoin, finalize, map, Observable, of, switchMap } from 'rxjs';

import { SessionContextService } from '../../../../core/auth/session-context.service';
import { PosAdminService } from '../../../../core/pos/pos-admin.service';
import {
  formatCentavos,
  localDateTimeToExclusiveEndInstant,
  localDateTimeToInstant,
  todayDateTimeLocalEnd,
  todayDateTimeLocalStart,
} from '../../../../core/pos/pos-date.util';
import { parseApiError, type ParsedApiError } from '../../../../core/http/parse-api-error';
import type {
  PosProductReportResponse,
  PosReportFilterParams,
  PosSaleReportResponse,
} from '../../../../core/model/pos/pos.dto';
import type { PageMetadata } from '../../../../core/model/common/pagination';
import { HeadquarterSelectComponent } from '../../../../shared/ui/headquarter-select/headquarter-select';
import { ListSearchFieldComponent } from '../../../../shared/ui/list-search-field/list-search-field';
import { PageHeaderComponent } from '../../../../shared/ui/page-header/page-header';
import { ProductSelectComponent } from '../../../../shared/ui/product-select/product-select';
import { DataStateComponent } from '../../../../shared/ui/data-state/data-state';

@Component({
  selector: 'app-ventas-page',
  imports: [
    PageHeaderComponent,
    DataStateComponent,
    FormsModule,
    DatePipe,
    HeadquarterSelectComponent,
    ProductSelectComponent,
    ListSearchFieldComponent,
  ],
  templateUrl: './ventas-page.html',
})
export class VentasPageComponent implements OnInit {
  private readonly posAdmin = inject(PosAdminService);
  private readonly session = inject(SessionContextService);

  readonly loading = signal(true);
  readonly error = signal<ParsedApiError | null>(null);
  readonly sales = signal<PosSaleReportResponse[]>([]);
  readonly metadata = signal<PageMetadata | null>(null);
  readonly page = signal(0);
  readonly formatCentavos = formatCentavos;

  readonly productRows = signal<PosProductReportResponse[]>([]);
  /** El último Consultar usó un producto del catálogo, montos abiertos, sin catalogar o cortesías. */
  readonly narrowed = signal(false);
  /** Borrador del buscador del resumen (Enter / Filtrar aplican). */
  productSummaryDraft = '';
  /** Nombre aplicado solo al resumen, coincidencia por contiene. */
  readonly productSummaryQuery = signal('');

  readonly productRowsView = computed(() => {
    const q = this.productSummaryQuery().trim().toLocaleLowerCase('es');
    const rows = this.productRows();
    const matched = q
      ? rows.filter((r) => r.productName.toLocaleLowerCase('es').includes(q))
      : rows;
    return [...matched].sort((a, b) => b.quantitySum - a.quantitySum);
  });

  readonly productTotals = computed(() => {
    const rows = this.productRowsView();
    return {
      quantity: rows.reduce((sum, r) => sum + r.quantitySum, 0),
      subtotalCentavos: rows.reduce((sum, r) => sum + r.subtotalCentavosSum, 0),
    };
  });

  selectedHeadquarterId: number | null = null;
  /** Valores `datetime-local` (hora local del navegador). */
  dateTimeFrom = '';
  dateTimeTo = '';
  /** Un producto del catálogo. Acota tickets y resumen al pulsar Consultar. */
  filterProductId: number | null = null;
  /** `all` muestra todo. Las otras opciones se excluyen entre sí. */
  includeKind: 'all' | 'open' | 'pending' | 'courtesy' = 'all';
  /** `true` = más recientes primero (API `recent`). */
  readonly salesNewestFirst = signal(true);
  readonly expandedSaleId = signal<string | null>(null);

  toggleDetails(saleId: string): void {
    this.expandedSaleId.update((current) => (current === saleId ? null : saleId));
  }

  toggleSalesOrder(): void {
    this.salesNewestFirst.update((current) => !current);
    this.cargar();
  }

  onProductSummarySearch(term: string): void {
    this.productSummaryQuery.set(term);
  }

  private initialLoad = true;
  private operatorNames = new Map<number, string>();

  ngOnInit(): void {
    this.dateTimeFrom = todayDateTimeLocalStart();
    this.dateTimeTo = todayDateTimeLocalEnd();

    if (!this.session.isAdmin()) {
      this.selectedHeadquarterId = this.session.activeHeadquarterId();
      this.cargar();
    } else {
      this.loading.set(false);
    }
  }

  onHeadquarterChange(value: number | number[] | null): void {
    this.selectedHeadquarterId = typeof value === 'number' ? value : null;
    this.session.selectHeadquarter(this.selectedHeadquarterId);
    if (this.initialLoad && this.selectedHeadquarterId != null) {
      this.initialLoad = false;
      this.cargar();
    }
  }

  operatorLabel(operatorId: number | null | undefined): string {
    if (operatorId == null) return '—';
    return this.operatorNames.get(operatorId) ?? `Operador #${operatorId}`;
  }

  cargar(): void {
    const hqId = this.selectedHeadquarterId;
    if (hqId == null) {
      this.loading.set(false);
      return;
    }

    this.error.set(null);
    this.page.set(0);
    this.loading.set(true);
    const narrowing = this.isNarrowed();
    const from = localDateTimeToInstant(this.dateTimeFrom);
    const to = localDateTimeToExclusiveEndInstant(this.dateTimeTo);

    this.posAdmin.listOperators({ headquarterId: hqId, page: 0, size: 200 }).subscribe({
      next: (ops) => {
        this.operatorNames = new Map(ops.items.map((op) => [op.id, op.displayName]));
      },
      error: () => {
        this.operatorNames = new Map();
      },
    });

    forkJoin({
      sales: this.posAdmin.reportSales(this.salesQueryParams(hqId, from, to, this.page())),
      products: this.fetchAllProductRows(hqId, from, to),
    })
      .pipe(finalize(() => this.loading.set(false)))
      .subscribe({
        next: ({ sales, products }) => {
          this.sales.set(sales.items);
          this.metadata.set(sales.metadata);
          this.productRows.set(products);
          this.narrowed.set(narrowing);
        },
        error: (err: unknown) => this.error.set(parseApiError(err)),
      });
  }

  siguiente(): void {
    if (this.metadata()?.hasNext) {
      this.page.update((p) => p + 1);
      this.cargarPagina();
    }
  }

  anterior(): void {
    if (this.metadata()?.hasPrevious) {
      this.page.update((p) => p - 1);
      this.cargarPagina();
    }
  }

  private cargarPagina(): void {
    const hqId = this.selectedHeadquarterId;
    if (hqId == null) return;
    const from = localDateTimeToInstant(this.dateTimeFrom);
    const to = localDateTimeToExclusiveEndInstant(this.dateTimeTo);
    this.posAdmin.reportSales(this.salesQueryParams(hqId, from, to, this.page())).subscribe({
      next: (result) => {
        this.sales.set(result.items);
        this.metadata.set(result.metadata);
      },
      error: (err: unknown) => this.error.set(parseApiError(err)),
    });
  }

  private isNarrowed(): boolean {
    return this.filterProductId != null || this.includeKind !== 'all';
  }

  private salesQueryParams(
    headquarterId: number,
    from: string,
    to: string,
    page: number,
  ): PosReportFilterParams {
    return {
      ...this.reportBaseParams(headquarterId, from, to),
      salesOrder: this.salesNewestFirst() ? 'recent' : 'oldest',
      page,
      size: 20,
    };
  }

  private reportBaseParams(headquarterId: number, from: string, to: string): PosReportFilterParams {
    return {
      headquarterId,
      from,
      to,
      productId: this.filterProductId ?? undefined,
      openProductsOnly: this.includeKind === 'open',
      lineType: this.includeKind === 'pending' ? 'PENDING_CATALOG' : undefined,
      courtesyOnly: this.includeKind === 'courtesy',
    };
  }

  private fetchAllProductRows(
    headquarterId: number,
    from: string,
    to: string,
  ): Observable<PosProductReportResponse[]> {
    const base = this.reportBaseParams(headquarterId, from, to);
    return this.posAdmin.reportProducts({ ...base, page: 0, size: 100 }).pipe(
      switchMap((first) => {
        const totalPages = first.metadata.totalPages;
        if (totalPages <= 1) {
          return of(first.items);
        }
        const rest = Array.from({ length: totalPages - 1 }, (_, index) =>
          this.posAdmin.reportProducts({ ...base, page: index + 1, size: 100 }),
        );
        return forkJoin(rest).pipe(
          map((pages) => [...first.items, ...pages.flatMap((page) => page.items)]),
        );
      }),
    );
  }
}
