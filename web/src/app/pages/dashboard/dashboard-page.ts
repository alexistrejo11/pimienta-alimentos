import { DatePipe } from '@angular/common';
import { Component, DestroyRef, inject, OnInit, signal } from '@angular/core';
import { catchError, finalize, merge, of, Subject, switchMap, timer } from 'rxjs';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

import { SessionContextService } from '../../core/auth/session-context.service';
import { HeadquarterLookupService } from '../../core/headquarters/headquarter-lookup.service';
import {
  parseApiError,
  type ParsedApiError,
} from '../../core/http/parse-api-error';
import type { UserDashboardResponse } from '../../core/model/account/user.dto';
import type { PosReportSummaryItemResponse, PosSaleReportResponse } from '../../core/model/pos/pos.dto';
import { formatCentavos, todayInstantRange } from '../../core/pos/pos-date.util';
import { PosAdminService } from '../../core/pos/pos-admin.service';
import { UserProfileService } from '../../core/user/user-profile.service';
import { HeadquarterSelectComponent } from '../../shared/ui/headquarter-select/headquarter-select';

type MetricKey = keyof UserDashboardResponse;

@Component({
  selector: 'app-dashboard-page',
  imports: [HeadquarterSelectComponent, DatePipe],
  templateUrl: './dashboard-page.html',
  styleUrl: './dashboard-page.css',
})
export class DashboardPageComponent implements OnInit {
  private readonly profile = inject(UserProfileService);
  private readonly posAdmin = inject(PosAdminService);
  private readonly lookup = inject(HeadquarterLookupService);
  readonly session = inject(SessionContextService);

  readonly hqName = this.lookup.name.bind(this.lookup);

  readonly loading = signal(true);
  readonly error = signal<ParsedApiError | null>(null);
  readonly dashboard = signal<UserDashboardResponse | null>(null);
  readonly posLoading = signal(false);
  readonly posError = signal<ParsedApiError | null>(null);
  readonly posSummary = signal<PosReportSummaryItemResponse[]>([]);
  readonly activity = signal<PosSaleReportResponse[]>([]);
  readonly activityLoading = signal(false);
  readonly activityError = signal<ParsedApiError | null>(null);

  readonly isAdmin = this.session.isAdmin;
  readonly isManager = this.session.isManager;
  readonly formatCentavos = formatCentavos;
  private readonly destroyRef = inject(DestroyRef);
  private readonly refreshPos$ = new Subject<void>();
  private posPollingStarted = false;

  readonly metrics: { key: MetricKey; label: string; hint: string }[] = [
    {
      key: 'totalActiveEmployees',
      label: 'Empleados activos',
      hint: 'Contratos con estado activo',
    },
    {
      key: 'totalActiveProjects',
      label: 'Proyectos activos',
      hint: 'CRM en ejecución',
    },
    {
      key: 'totalActiveHeadquarters',
      label: 'Sedes activas',
      hint: 'Sedes no dadas de baja',
    },
    {
      key: 'totalActivePersonalTasks',
      label: 'Tareas personales abiertas',
      hint: 'Sin proyecto ni oportunidad, pendientes de cierre',
    },
    {
      key: 'totalPendingPersonalTasks',
      label: 'Tareas personales pendientes',
      hint: 'Estado PENDING',
    },
    {
      key: 'totalActiveEmployeesTasks',
      label: 'Tareas de trabajo abiertas',
      hint: 'Ligadas a proyecto u oportunidad',
    },
    {
      key: 'totalEmployeePending',
      label: 'Trabajo pendiente (PENDING)',
      hint: 'Tareas de equipo en espera',
    },
  ];

  ngOnInit(): void {
    void this.lookup.ensureLoaded();
    this.load();
  }

  reload(): void {
    this.load();
  }

  valueFor(d: UserDashboardResponse, key: MetricKey): number {
    return d[key];
  }

  onHeadquarterChange(id: number | number[] | null): void {
    this.session.selectHeadquarter(typeof id === 'number' ? id : null);
    this.refreshPos$.next();
  }

  private load(): void {
    this.error.set(null);
    this.loading.set(true);
    this.profile
      .getDashboard()
      .pipe(finalize(() => this.loading.set(false)))
      .subscribe({
        next: (d: UserDashboardResponse) => this.dashboard.set(d),
        error: (err: unknown) => this.error.set(parseApiError(err)),
      });

    if (this.session.canAccessPos()) this.startPosPolling();
  }

  private startPosPolling(): void {
    if (this.posPollingStarted) {
      this.refreshPos$.next();
      return;
    }
    this.posPollingStarted = true;
    const ticks$ = merge(timer(0, 30_000), this.refreshPos$).pipe(takeUntilDestroyed(this.destroyRef));

    ticks$
      .pipe(
        switchMap(() => {
          this.posError.set(null);
          this.posLoading.set(true);
          const range = todayInstantRange();
          return this.posAdmin.reportSummary(this.session.activeHeadquarterId() ?? undefined, range.from, range.to).pipe(
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
        if (res) this.posSummary.set(res.rows);
      });

    ticks$
      .pipe(
        switchMap(() => {
          const hqId = this.session.activeHeadquarterId();
          if (hqId == null) {
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
