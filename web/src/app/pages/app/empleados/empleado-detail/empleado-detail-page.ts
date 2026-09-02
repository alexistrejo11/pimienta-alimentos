import { Component, inject, OnInit, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { finalize } from 'rxjs';

import { EmployeeService } from '../../../../core/employees/employee.service';
import { parseApiError, type ParsedApiError } from '../../../../core/http/parse-api-error';
import type { EmployeeResponse } from '../../../../core/model/employee/employee.dto';
import { PageHeaderComponent } from '../../../../shared/ui/page-header/page-header';
import { DataStateComponent } from '../../../../shared/ui/data-state/data-state';
import { EmpleadoInfoCardComponent } from '../components/empleado-info-card/empleado-info-card';
import { EmpleadoEmpleoCardComponent } from '../components/empleado-empleo-card/empleado-empleo-card';
import { EmpleadoNominaCardComponent } from '../components/empleado-nomina-card/empleado-nomina-card';
import { EmpleadoHorarioCardComponent } from '../components/empleado-horario-card/empleado-horario-card';
import { EmpleadoAsistenciaCardComponent } from '../components/empleado-asistencia-card/empleado-asistencia-card';

/**
 * Página de detalle de un empleado.
 * Cinco secciones: información personal, laboral, nómina, horario semanal y asistencia.
 */
@Component({
  selector: 'app-empleado-detail-page',
  
  imports: [
    PageHeaderComponent,
    DataStateComponent,
    EmpleadoInfoCardComponent,
    EmpleadoEmpleoCardComponent,
    EmpleadoNominaCardComponent,
    EmpleadoHorarioCardComponent,
    EmpleadoAsistenciaCardComponent,
    RouterLink,
  ],
  templateUrl: './empleado-detail-page.html',
})
export class EmpleadoDetailPageComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly service = inject(EmployeeService);

  readonly loading = signal(true);
  readonly error = signal<ParsedApiError | null>(null);
  readonly empleado = signal<EmployeeResponse | null>(null);

  /** Controls which inner tab is active: 'info' | 'horario' | 'asistencia' */
  readonly activeTab = signal<'info' | 'horario' | 'asistencia'>('info');

  ngOnInit(): void {
    const id = Number(this.route.snapshot.paramMap.get('id'));
    this.cargar(id);
  }

  cargar(id: number): void {
    this.error.set(null);
    this.loading.set(true);
    this.service
      .getById(id)
      .pipe(finalize(() => this.loading.set(false)))
      .subscribe({
        next: (e) => this.empleado.set(e),
        error: (err: unknown) => this.error.set(parseApiError(err)),
      });
  }
}
