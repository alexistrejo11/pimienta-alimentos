import { Component, inject, OnInit, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { finalize } from 'rxjs';

import { SessionContextService } from '../../../../core/auth/session-context.service';
import { PosAdminService } from '../../../../core/pos/pos-admin.service';
import {
  todayInstantRange,
  formatCentavos,
  localDateStartInstant,
  localDateEndInstant,
} from '../../../../core/pos/pos-date.util';
import { parseApiError, type ParsedApiError } from '../../../../core/http/parse-api-error';
import type { PosShiftResponse } from '../../../../core/model/pos/pos.dto';
import { posShiftStatusLabel } from '../../../../core/i18n/enum-labels';
import { HeadquarterSelectComponent } from '../../../../shared/ui/headquarter-select/headquarter-select';
import { PageHeaderComponent } from '../../../../shared/ui/page-header/page-header';
import { DataStateComponent } from '../../../../shared/ui/data-state/data-state';

@Component({
  selector: 'app-cortes-page',
  imports: [PageHeaderComponent, DataStateComponent, FormsModule, DatePipe, HeadquarterSelectComponent],
  templateUrl: './cortes-page.html',
})
export class CortesPageComponent implements OnInit {
  private readonly posAdmin = inject(PosAdminService);
  private readonly session = inject(SessionContextService);

  readonly loading = signal(true);
  readonly error = signal<ParsedApiError | null>(null);
  readonly shifts = signal<PosShiftResponse[]>([]);
  readonly isAdmin = this.session.isAdmin;
  readonly formatCentavos = formatCentavos;
  readonly shiftStatusLabel = posShiftStatusLabel;

  selectedHeadquarterId: number | null = null;
  dateFrom = '';
  dateTo = '';
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

  operatorLabel(operatorId: number | null): string {
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
    this.loading.set(true);
    const from = localDateStartInstant(this.dateFrom);
    const to = localDateEndInstant(this.dateTo);

    this.posAdmin
      .listOperators({ headquarterId: hqId, page: 0, size: 200 })
      .pipe(finalize(() => {}))
      .subscribe({
        next: (page) => {
          this.operatorNames = new Map(page.items.map((op) => [op.id, op.displayName]));
          this.loadShifts(hqId, from, to);
        },
        error: () => {
          this.operatorNames = new Map();
          this.loadShifts(hqId, from, to);
        },
      });
  }

  private loadShifts(hqId: number, from: string, to: string): void {
    this.posAdmin
      .listShifts({
        headquarterId: hqId,
        from,
        to,
        status: 'CLOSED',
        page: 0,
        size: 50,
      })
      .pipe(finalize(() => this.loading.set(false)))
      .subscribe({
        next: (page) => this.shifts.set(page.items),
        error: (err: unknown) => this.error.set(parseApiError(err)),
      });
  }
}
