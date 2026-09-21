import { Component, inject, OnInit, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { finalize } from 'rxjs';

import {
  parseApiError,
  type ParsedApiError,
} from '../../../core/http/parse-api-error';
import type { UserDashboardResponse } from '../../../core/model/account/user.dto';
import { UserProfileService } from '../../../core/user/user-profile.service';

type MetricKey = keyof UserDashboardResponse;

@Component({
  selector: 'app-erp-dashboard-page',
  imports: [RouterLink],
  templateUrl: './erp-dashboard-page.html',
})
export class ErpDashboardPageComponent implements OnInit {
  private readonly profile = inject(UserProfileService);

  readonly loading = signal(true);
  readonly error = signal<ParsedApiError | null>(null);
  readonly dashboard = signal<UserDashboardResponse | null>(null);

  readonly metrics: { key: MetricKey; label: string; hint: string; route: string }[] = [
    {
      key: 'totalActiveEmployees',
      label: 'Empleados activos',
      hint: 'Contratos con estado activo',
      route: '/app/erp/empleados',
    },
    {
      key: 'totalActiveProjects',
      label: 'Proyectos activos',
      hint: 'CRM en ejecución',
      route: '/app/erp/crm/proyectos',
    },
    {
      key: 'totalActiveHeadquarters',
      label: 'Sedes activas',
      hint: 'Sedes no dadas de baja',
      route: '/app/ops/sedes',
    },
    {
      key: 'totalActivePersonalTasks',
      label: 'Tareas personales abiertas',
      hint: 'Sin proyecto ni oportunidad, pendientes de cierre',
      route: '/app/erp/tareas',
    },
    {
      key: 'totalPendingPersonalTasks',
      label: 'Tareas personales pendientes',
      hint: 'Estado pendiente',
      route: '/app/erp/tareas',
    },
    {
      key: 'totalActiveEmployeesTasks',
      label: 'Tareas de trabajo abiertas',
      hint: 'Ligadas a proyecto u oportunidad',
      route: '/app/erp/tareas',
    },
    {
      key: 'totalEmployeePending',
      label: 'Trabajo pendiente',
      hint: 'Tareas de equipo en espera',
      route: '/app/erp/tareas',
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
  }
}
