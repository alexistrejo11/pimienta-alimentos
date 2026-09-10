import { Component, inject, OnInit, signal } from '@angular/core';
import { finalize } from 'rxjs';

import { SessionContextService } from '../../core/auth/session-context.service';
import {
  parseApiError,
  type ParsedApiError,
} from '../../core/http/parse-api-error';
import type { UserDashboardResponse } from '../../core/model/account/user.dto';
import type { PosReportSummaryItemResponse } from '../../core/model/pos/pos.dto';
import { formatCentavos, todayInstantRange } from '../../core/pos/pos-date.util';
import { PosAdminService } from '../../core/pos/pos-admin.service';
import { UserProfileService } from '../../core/user/user-profile.service';

type MetricKey = keyof UserDashboardResponse;

@Component({
  selector: 'app-dashboard-page',
  imports: [],
  templateUrl: './dashboard-page.html',
  styleUrl: './dashboard-page.css',
})
export class DashboardPageComponent implements OnInit {
  private readonly profile = inject(UserProfileService);
  private readonly posAdmin = inject(PosAdminService);
  readonly session = inject(SessionContextService);

  readonly loading = signal(true);
  readonly error = signal<ParsedApiError | null>(null);
  readonly dashboard = signal<UserDashboardResponse | null>(null);
  readonly posLoading = signal(false);
  readonly posError = signal<ParsedApiError | null>(null);
  readonly posSummary = signal<PosReportSummaryItemResponse[]>([]);

  readonly isAdmin = this.session.isAdmin;
  readonly isManager = this.session.isManager;
  readonly formatCentavos = formatCentavos;

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
    this.load();
  }

  reload(): void {
    this.load();
  }

  valueFor(d: UserDashboardResponse, key: MetricKey): number {
    return d[key];
  }

  private load(): void {
    this.error.set(null);
    this.loading.set(true);
    this.profile
      .getDashboard()
      .pipe(finalize(() => this.loading.set(false)))
      .subscribe({
        next: (d) => this.dashboard.set(d),
        error: (err: unknown) => this.error.set(parseApiError(err)),
      });

    if (this.session.canAccessPos()) {
      this.loadPosSummary();
    }
  }

  private loadPosSummary(): void {
    this.posError.set(null);
    this.posLoading.set(true);

    const range = todayInstantRange();
    const hqId = this.session.isAdmin() ? undefined : this.session.managerHeadquarterId() ?? undefined;

    this.posAdmin
      .reportSummary(hqId, range.from, range.to)
      .pipe(finalize(() => this.posLoading.set(false)))
      .subscribe({
        next: (res) => this.posSummary.set(res.rows),
        error: (err: unknown) => this.posError.set(parseApiError(err)),
      });
  }
}
