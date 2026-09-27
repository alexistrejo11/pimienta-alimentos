import { DatePipe } from '@angular/common';
import { Component, inject, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { finalize } from 'rxjs';

import { SessionContextService } from '../../../../core/auth/session-context.service';
import { PosAdminService } from '../../../../core/pos/pos-admin.service';
import { todayInstantRange, formatCentavos, localDateStartInstant, localDateEndInstant } from '../../../../core/pos/pos-date.util';
import { parseApiError, type ParsedApiError } from '../../../../core/http/parse-api-error';
import type { PosSaleReportResponse } from '../../../../core/model/pos/pos.dto';
import type { PageMetadata } from '../../../../core/model/common/pagination';
import { posSaleTicketStatusLabel, posSyncEventStatusLabel } from '../../../../core/i18n/enum-labels';
import { HeadquarterSelectComponent } from '../../../../shared/ui/headquarter-select/headquarter-select';
import { PageHeaderComponent } from '../../../../shared/ui/page-header/page-header';
import { DataStateComponent } from '../../../../shared/ui/data-state/data-state';

@Component({
  selector: 'app-ventas-page',
  imports: [PageHeaderComponent, DataStateComponent, FormsModule, DatePipe, HeadquarterSelectComponent],
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
  readonly ticketStatusLabel = posSaleTicketStatusLabel;
  readonly syncStatusLabel = posSyncEventStatusLabel;

  selectedHeadquarterId: number | null = null;
  dateFrom = '';
  dateTo = '';
  openProductsOnly = false;
  readonly expandedSaleId = signal<string | null>(null);

  toggleDetails(saleId: string): void {
    this.expandedSaleId.update(current => current === saleId ? null : saleId);
  }
  private initialLoad = true;
  private operatorNames = new Map<number, string>();

  ngOnInit(): void {
    const range = todayInstantRange();
    this.dateFrom = range.from.slice(0, 10);
    this.dateTo = range.to.slice(0, 10);

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
    const from = localDateStartInstant(this.dateFrom);
    const to = localDateEndInstant(this.dateTo);

    this.posAdmin.listOperators({ headquarterId: hqId, page: 0, size: 200 }).subscribe({
      next: (ops) => {
        this.operatorNames = new Map(ops.items.map((op) => [op.id, op.displayName]));
      },
      error: () => {
        this.operatorNames = new Map();
      },
    });

    this.posAdmin
       .reportSales({ headquarterId: hqId, from, to, openProductsOnly: this.openProductsOnly, page: this.page(), size: 20 })
      .pipe(finalize(() => this.loading.set(false)))
      .subscribe({
         next: (result) => { this.sales.set(result.items); this.metadata.set(result.metadata); },
        error: (err: unknown) => this.error.set(parseApiError(err)),
      });
  }

  siguiente(): void { if (this.metadata()?.hasNext) { this.page.update((p) => p + 1); this.cargarPagina(); } }
  anterior(): void { if (this.metadata()?.hasPrevious) { this.page.update((p) => p - 1); this.cargarPagina(); } }
  private cargarPagina(): void { const current = this.page(); this.page.set(current); this.cargarSinReset(); }
  private cargarSinReset(): void {
    const hqId = this.selectedHeadquarterId; if (hqId == null) return;
    this.posAdmin.reportSales({ headquarterId: hqId, from: localDateStartInstant(this.dateFrom), to: localDateEndInstant(this.dateTo), openProductsOnly: this.openProductsOnly, page: this.page(), size: 20 }).subscribe({ next: (result) => { this.sales.set(result.items); this.metadata.set(result.metadata); }, error: (err: unknown) => this.error.set(parseApiError(err)) });
  }
}
