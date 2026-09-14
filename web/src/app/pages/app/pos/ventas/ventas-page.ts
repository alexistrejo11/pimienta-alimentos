import { DatePipe } from '@angular/common';
import { Component, inject, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { finalize } from 'rxjs';

import { SessionContextService } from '../../../../core/auth/session-context.service';
import { PosAdminService } from '../../../../core/pos/pos-admin.service';
import { todayInstantRange, formatCentavos } from '../../../../core/pos/pos-date.util';
import { parseApiError, type ParsedApiError } from '../../../../core/http/parse-api-error';
import type { PosSaleReportResponse } from '../../../../core/model/pos/pos.dto';
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
  readonly formatCentavos = formatCentavos;

  selectedHeadquarterId: number | null = null;
  dateFrom = '';
  dateTo = '';
  openProductsOnly = false;
  readonly expandedSaleId = signal<string | null>(null);

  toggleDetails(saleId: string): void {
    this.expandedSaleId.update(current => current === saleId ? null : saleId);
  }
  private initialLoad = true;

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

  cargar(): void {
    const hqId = this.selectedHeadquarterId;
    if (hqId == null) {
      this.loading.set(false);
      return;
    }

    this.error.set(null);
    this.loading.set(true);
    const from = `${this.dateFrom}T00:00:00.000Z`;
    const to = `${this.dateTo}T23:59:59.999Z`;

    this.posAdmin
       .reportSales({ headquarterId: hqId, from, to, openProductsOnly: this.openProductsOnly, page: 0, size: 50 })
      .pipe(finalize(() => this.loading.set(false)))
      .subscribe({
        next: (page) => this.sales.set(page.items),
        error: (err: unknown) => this.error.set(parseApiError(err)),
      });
  }
}
