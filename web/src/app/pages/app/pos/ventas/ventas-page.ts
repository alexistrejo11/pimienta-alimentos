import { DatePipe } from '@angular/common';
import { Component, inject, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { finalize } from 'rxjs';

import { SessionContextService } from '../../../../core/auth/session-context.service';
import { HeadquarterService } from '../../../../core/headquarters/headquarter.service';
import { PosAdminService } from '../../../../core/pos/pos-admin.service';
import { todayInstantRange, formatCentavos } from '../../../../core/pos/pos-date.util';
import { parseApiError, type ParsedApiError } from '../../../../core/http/parse-api-error';
import type { HeadQuarterResponse } from '../../../../core/model/headquarter/headquarter.dto';
import type { PosSaleReportResponse } from '../../../../core/model/pos/pos.dto';
import { PageHeaderComponent } from '../../../../shared/ui/page-header/page-header';
import { DataStateComponent } from '../../../../shared/ui/data-state/data-state';

@Component({
  selector: 'app-ventas-page',
  imports: [PageHeaderComponent, DataStateComponent, FormsModule, DatePipe],
  templateUrl: './ventas-page.html',
})
export class VentasPageComponent implements OnInit {
  private readonly posAdmin = inject(PosAdminService);
  private readonly session = inject(SessionContextService);
  private readonly hqService = inject(HeadquarterService);

  readonly loading = signal(true);
  readonly error = signal<ParsedApiError | null>(null);
  readonly sales = signal<PosSaleReportResponse[]>([]);
  readonly sedes = signal<HeadQuarterResponse[]>([]);
  readonly isAdmin = this.session.isAdmin;
  readonly formatCentavos = formatCentavos;

  selectedHeadquarterId: number | null = null;
  dateFrom = '';
  dateTo = '';

  ngOnInit(): void {
    const range = todayInstantRange();
    this.dateFrom = range.from.slice(0, 10);
    this.dateTo = range.to.slice(0, 10);

    if (this.session.isAdmin()) {
      this.hqService.list(0, 100).subscribe({
        next: (page) => {
          this.sedes.set(page.content);
          if (page.content.length > 0) {
            this.selectedHeadquarterId = page.content[0].id;
            this.cargar();
          } else {
            this.loading.set(false);
          }
        },
      });
    } else {
      this.selectedHeadquarterId = this.session.managerHeadquarterId();
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
      .reportSales({ headquarterId: hqId, from, to, page: 0, size: 50 })
      .pipe(finalize(() => this.loading.set(false)))
      .subscribe({
        next: (page) => this.sales.set(page.items),
        error: (err: unknown) => this.error.set(parseApiError(err)),
      });
  }
}
