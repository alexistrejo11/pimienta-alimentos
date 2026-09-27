import { DatePipe } from '@angular/common';
import { Component, DestroyRef, inject, OnInit, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { catchError, finalize, merge, of, Subject, switchMap, timer } from 'rxjs';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

import { SessionContextService } from '../../core/auth/session-context.service';
import { InventoryService } from '../../core/inventory/inventory.service';
import type { InventoryDashboardResponse } from '../../core/model/inventory/inventory.dto';
import {
  parseApiError,
  type ParsedApiError,
} from '../../core/http/parse-api-error';
import type { PosReportSummaryItemResponse, PosSaleReportResponse } from '../../core/model/pos/pos.dto';
import { formatCentavos, todayInstantRange } from '../../core/pos/pos-date.util';
import { PosAdminService } from '../../core/pos/pos-admin.service';
import { HeadquarterSelectComponent } from '../../shared/ui/headquarter-select/headquarter-select';

interface PosTotals {
  salesCentavos: number;
  ticketCount: number;
  wasteCount: number;
  cancellationCount: number;
  openIncidentCount: number;
  deviceCount: number;
  openProductCentavos: number;
  openProductLineCount: number;
  openProductPendingReviewCount: number;
}

const EMPTY_POS: PosTotals = {
  salesCentavos: 0,
  ticketCount: 0,
  wasteCount: 0,
  cancellationCount: 0,
  openIncidentCount: 0,
  deviceCount: 0,
  openProductCentavos: 0,
  openProductLineCount: 0,
  openProductPendingReviewCount: 0,
};

@Component({
  selector: 'app-dashboard-page',
  imports: [HeadquarterSelectComponent, DatePipe, RouterLink],
  templateUrl: './dashboard-page.html',
  styleUrl: './dashboard-page.css',
})
export class DashboardPageComponent implements OnInit {
  private readonly posAdmin = inject(PosAdminService);
  private readonly inventory = inject(InventoryService);
  readonly session = inject(SessionContextService);

  readonly posLoading = signal(false);
  readonly posError = signal<ParsedApiError | null>(null);
  readonly posTotals = signal<PosTotals>(EMPTY_POS);
  readonly hasPosRows = signal(false);
  readonly activity = signal<PosSaleReportResponse[]>([]);
  readonly activityLoading = signal(false);
  readonly activityError = signal<ParsedApiError | null>(null);

  readonly inventoryLoading = signal(false);
  readonly inventoryError = signal<ParsedApiError | null>(null);
  readonly inventoryDash = signal<InventoryDashboardResponse | null>(null);

  readonly isAdmin = this.session.isAdmin;
  readonly canViewPosFinancials = this.session.canViewPosFinancials;
  readonly formatCentavos = formatCentavos;
  private readonly destroyRef = inject(DestroyRef);
  private readonly refresh$ = new Subject<void>();
  private pollingStarted = false;

  ngOnInit(): void {
    this.startPolling();
  }

  reload(): void {
    this.refresh$.next();
  }

  formatStockValue(value: number): string {
    return new Intl.NumberFormat('es-MX', { style: 'currency', currency: 'MXN' }).format(value);
  }

  onHeadquarterChange(id: number | number[] | null): void {
    this.session.selectHeadquarter(typeof id === 'number' ? id : null);
    this.refresh$.next();
  }

  private startPolling(): void {
    if (this.pollingStarted) {
      this.refresh$.next();
      return;
    }
    this.pollingStarted = true;
    const ticks$ = merge(timer(0, 30_000), this.refresh$).pipe(takeUntilDestroyed(this.destroyRef));

    ticks$
      .pipe(
        switchMap(() => {
          if (!this.session.canViewPosFinancials()) {
            this.posLoading.set(false);
            this.posTotals.set(EMPTY_POS);
            this.hasPosRows.set(false);
            return of(null);
          }
          this.posError.set(null);
          this.posLoading.set(true);
          const range = todayInstantRange();
          return this.posAdmin
            .reportSummary(this.session.activeHeadquarterId() ?? undefined, range.from, range.to)
            .pipe(
              catchError((err: unknown) => {
                this.posError.set(parseApiError(err));
                return of(null);
              }),
            );
        }),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe((res) => {
        this.posLoading.set(false);
        const rows = res?.rows ?? [];
        this.hasPosRows.set(rows.length > 0);
        this.posTotals.set(sumPosRows(rows));
      });

    ticks$
      .pipe(
        switchMap(() => {
          this.inventoryError.set(null);
          this.inventoryLoading.set(true);
          return this.inventory.getDashboard(this.session.activeHeadquarterId()).pipe(
            catchError((err: unknown) => {
              this.inventoryError.set(parseApiError(err));
              return of(null);
            }),
            finalize(() => this.inventoryLoading.set(false)),
          );
        }),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe((dash) => {
        if (dash) this.inventoryDash.set(dash);
      });

    ticks$
      .pipe(
        switchMap(() => {
          const hqId = this.session.activeHeadquarterId();
          if (!this.session.canViewPosFinancials() || hqId == null) {
            this.activity.set([]);
            return of(null);
          }
          this.activityError.set(null);
          this.activityLoading.set(true);
          const range = todayInstantRange();
          return this.posAdmin.reportSales({ headquarterId: hqId, from: range.from, to: range.to, page: 0, size: 8 }).pipe(
            catchError((err: unknown) => {
              this.activityError.set(parseApiError(err));
              return of(null);
            }),
          );
        }),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe((res) => {
        this.activityLoading.set(false);
        if (res) this.activity.set(res.items);
      });
  }
}

function sumPosRows(rows: PosReportSummaryItemResponse[]): PosTotals {
  return rows.reduce(
    (acc, row) => ({
      salesCentavos: acc.salesCentavos + row.salesCentavos,
      ticketCount: acc.ticketCount + row.ticketCount,
      wasteCount: acc.wasteCount + row.wasteCount,
      cancellationCount: acc.cancellationCount + row.cancellationCount,
      openIncidentCount: acc.openIncidentCount + row.openIncidentCount,
      deviceCount: acc.deviceCount + row.deviceCount,
      openProductCentavos: acc.openProductCentavos + row.openProductCentavos,
      openProductLineCount: acc.openProductLineCount + row.openProductLineCount,
      openProductPendingReviewCount:
        acc.openProductPendingReviewCount + row.openProductPendingReviewCount,
    }),
    EMPTY_POS,
  );
}
