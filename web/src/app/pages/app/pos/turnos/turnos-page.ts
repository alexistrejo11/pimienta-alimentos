import { Component, DestroyRef, inject, OnInit, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { finalize, interval } from 'rxjs';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

import { SessionContextService } from '../../../../core/auth/session-context.service';
import { PosAdminService } from '../../../../core/pos/pos-admin.service';
import {
  todayInstantRange,
  formatCentavos,
  localDateStartInstant,
  localDateEndInstant,
} from '../../../../core/pos/pos-date.util';
import { parseApiError, type ParsedApiError } from '../../../../core/http/parse-api-error';
import type {
  PosShiftListItemResponse,
  PosShiftReconciliationResponse,
} from '../../../../core/model/pos/pos.dto';
import { posShiftStatusLabel } from '../../../../core/i18n/enum-labels';
import { HeadquarterSelectComponent } from '../../../../shared/ui/headquarter-select/headquarter-select';
import { PageHeaderComponent } from '../../../../shared/ui/page-header/page-header';
import { DataStateComponent } from '../../../../shared/ui/data-state/data-state';

type TurnosTab = 'active' | 'history';

@Component({
  selector: 'app-turnos-page',
  imports: [PageHeaderComponent, DataStateComponent, FormsModule, DatePipe, HeadquarterSelectComponent],
  templateUrl: './turnos-page.html',
})
export class TurnosPageComponent implements OnInit {
  private readonly posAdmin = inject(PosAdminService);
  private readonly session = inject(SessionContextService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly destroyRef = inject(DestroyRef);

  readonly loading = signal(true);
  readonly detailLoading = signal(false);
  readonly error = signal<ParsedApiError | null>(null);
  readonly shifts = signal<PosShiftListItemResponse[]>([]);
  readonly reconciliation = signal<PosShiftReconciliationResponse | null>(null);
  readonly selectedShiftId = signal<string | null>(null);
  readonly isAdmin = this.session.isAdmin;
  readonly canViewPosFinancials = this.session.canViewPosFinancials;
  readonly formatCentavos = formatCentavos;
  readonly shiftStatusLabel = posShiftStatusLabel;

  tab: TurnosTab = 'active';
  selectedHeadquarterId: number | null = null;
  dateFrom = '';
  dateTo = '';
  selectedOperatorId: number | null = null;
  operators: { id: number; displayName: string }[] = [];

  private initialLoad = true;
  private pollStarted = false;

  ngOnInit(): void {
    const range = todayInstantRange();
    this.dateFrom = range.from.slice(0, 10);
    this.dateTo = range.to.slice(0, 10);

    const tabParam = this.route.snapshot.queryParamMap.get('tab');
    if (tabParam === 'history' && this.session.canViewPosFinancials()) {
      this.tab = 'history';
    }

    if (!this.session.isAdmin()) {
      this.selectedHeadquarterId = this.session.activeHeadquarterId();
      this.cargar();
    } else {
      this.loading.set(false);
    }

    this.route.queryParamMap.pipe(takeUntilDestroyed(this.destroyRef)).subscribe((params) => {
      const t = params.get('tab');
      if (t === 'history' && this.session.canViewPosFinancials() && this.tab !== 'history') {
        this.tab = 'history';
        this.cargar();
      }
    });
  }

  setTab(tab: TurnosTab): void {
    if (tab === 'history' && !this.session.canViewPosFinancials()) return;
    this.tab = tab;
    this.reconciliation.set(null);
    this.selectedShiftId.set(null);
    void this.router.navigate([], {
      relativeTo: this.route,
      queryParams: { tab: tab === 'history' ? 'history' : null },
      queryParamsHandling: 'merge',
      replaceUrl: true,
    });
    this.cargar();
    if (tab === 'active') {
      this.startPolling();
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

  operatorLabel(row: PosShiftListItemResponse): string {
    if (row.cashierDisplayName?.trim()) return row.cashierDisplayName;
    if (row.cashierOperatorId != null) return `Operador #${row.cashierOperatorId}`;
    return '—';
  }

  deviceLabel(row: PosShiftListItemResponse): string {
    if (row.deviceName?.trim()) return row.deviceName;
    if (row.deviceVisibleCode?.trim()) return row.deviceVisibleCode;
    return row.deviceId.slice(0, 8);
  }

  elapsed(openedAt: string): string {
    const ms = Math.max(0, Date.now() - new Date(openedAt).getTime());
    const mins = Math.floor(ms / 60_000);
    if (mins < 60) return `${mins} min`;
    const hours = Math.floor(mins / 60);
    const rem = mins % 60;
    return `${hours} h ${rem} min`;
  }

  cargar(): void {
    const hqId = this.selectedHeadquarterId;
    if (hqId == null) {
      this.loading.set(false);
      return;
    }

    this.error.set(null);
    this.loading.set(true);
    this.loadOperators(hqId, () => this.fetchShifts(hqId));
    if (this.tab === 'active') {
      this.startPolling();
    }
  }

  selectShift(row: PosShiftListItemResponse): void {
    if (!this.session.canViewPosFinancials()) return;
    const hqId = this.selectedHeadquarterId;
    if (hqId == null) return;
    this.selectedShiftId.set(row.shiftId);
    this.detailLoading.set(true);
    this.reconciliation.set(null);
    this.posAdmin
      .getShiftReconciliation(row.shiftId, hqId)
      .pipe(finalize(() => this.detailLoading.set(false)))
      .subscribe({
        next: (detail) => this.reconciliation.set(detail),
        error: (err: unknown) => this.error.set(parseApiError(err)),
      });
  }

  private loadOperators(hqId: number, done: () => void): void {
    this.posAdmin.listOperators({ headquarterId: hqId, page: 0, size: 200 }).subscribe({
      next: (page) => {
        this.operators = page.items.map((op) => ({ id: op.id, displayName: op.displayName }));
        done();
      },
      error: () => {
        this.operators = [];
        done();
      },
    });
  }

  private fetchShifts(hqId: number): void {
    const status = this.tab === 'active' ? 'OPEN' : 'CLOSED';
    const from =
      this.tab === 'history' && this.dateFrom ? localDateStartInstant(this.dateFrom) : undefined;
    const to = this.tab === 'history' && this.dateTo ? localDateEndInstant(this.dateTo) : undefined;

    this.posAdmin
      .listShifts({
        headquarterId: hqId,
        from,
        to,
        status,
        cashierOperatorId: this.selectedOperatorId ?? undefined,
        page: 0,
        size: 50,
      })
      .pipe(finalize(() => this.loading.set(false)))
      .subscribe({
        next: (page) => this.shifts.set(page.items),
        error: (err: unknown) => this.error.set(parseApiError(err)),
      });
  }

  private startPolling(): void {
    if (this.pollStarted) return;
    this.pollStarted = true;
    interval(45_000)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(() => {
        if (this.tab !== 'active' || this.selectedHeadquarterId == null || this.loading()) return;
        this.fetchShifts(this.selectedHeadquarterId);
      });
  }
}
