import { Component, inject, OnInit, signal } from '@angular/core';
import { DatePipe, JsonPipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { finalize } from 'rxjs';

import { SessionContextService } from '../../../../core/auth/session-context.service';
import { PosAdminService } from '../../../../core/pos/pos-admin.service';
import { todayInstantRange } from '../../../../core/pos/pos-date.util';
import { parseApiError, type ParsedApiError } from '../../../../core/http/parse-api-error';
import type { PosLedgerEventReportResponse } from '../../../../core/model/pos/pos.dto';
import { HeadquarterSelectComponent } from '../../../../shared/ui/headquarter-select/headquarter-select';
import { PageHeaderComponent } from '../../../../shared/ui/page-header/page-header';
import { DataStateComponent } from '../../../../shared/ui/data-state/data-state';

@Component({
  selector: 'app-cortes-page',
  imports: [PageHeaderComponent, DataStateComponent, FormsModule, DatePipe, JsonPipe, HeadquarterSelectComponent],
  templateUrl: './cortes-page.html',
})
export class CortesPageComponent implements OnInit {
  private readonly posAdmin = inject(PosAdminService);
  private readonly session = inject(SessionContextService);

  readonly loading = signal(true);
  readonly error = signal<ParsedApiError | null>(null);
  readonly events = signal<PosLedgerEventReportResponse[]>([]);
  readonly isAdmin = this.session.isAdmin;

  selectedHeadquarterId: number | null = null;
  dateFrom = '';
  dateTo = '';
  private initialLoad = true;

  ngOnInit(): void {
    const range = todayInstantRange();
    this.dateFrom = range.from.slice(0, 10);
    this.dateTo = range.to.slice(0, 10);

    if (!this.session.isAdmin()) {
      this.selectedHeadquarterId = this.session.managerHeadquarterId();
      this.cargar();
    } else {
      this.loading.set(false);
    }
  }

  onHeadquarterChange(value: number | number[] | null): void {
    this.selectedHeadquarterId = typeof value === 'number' ? value : null;
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
    this.posAdmin
      .reportShiftCloses({
        headquarterId: hqId,
        from: `${this.dateFrom}T00:00:00.000Z`,
        to: `${this.dateTo}T23:59:59.999Z`,
        page: 0,
        size: 50,
      })
      .pipe(finalize(() => this.loading.set(false)))
      .subscribe({
        next: (page) => this.events.set(page.items),
        error: (err: unknown) => this.error.set(parseApiError(err)),
      });
  }
}
