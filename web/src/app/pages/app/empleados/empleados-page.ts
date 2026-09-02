import { Component, inject, OnInit, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { finalize } from 'rxjs';

import { EmployeeService } from '../../../core/employees/employee.service';
import { parseApiError, type ParsedApiError } from '../../../core/http/parse-api-error';
import type { 
  EmployeeListItemResponse,
  EmployeeStatisticsResponse,
} from '../../../core/model/employee/employee.dto';
import { PageHeaderComponent } from '../../../shared/ui/page-header/page-header';
import { DataStateComponent } from '../../../shared/ui/data-state/data-state';
import { EmpleadoRowComponent } from './components/empleado-row/empleado-row';

/**
 * Página principal del módulo de empleados.
 * Muestra una barra de estadísticas (total/activos/inactivos) y
 * una tabla con la lista paginada de empleados.
 */
@Component({
  selector: 'app-empleados-page',
  
  imports: [PageHeaderComponent, DataStateComponent, EmpleadoRowComponent, RouterLink],
  templateUrl: './empleados-page.html',
})
export class EmpleadosPageComponent implements OnInit {
  private readonly service = inject(EmployeeService);

  // ── Estado de la vista ──────────────────────────────────────────────────
  readonly loading = signal(true);
  readonly error = signal<ParsedApiError | null>(null);
  readonly empleados = signal<EmployeeListItemResponse[]>([]);
  readonly stats = signal<EmployeeStatisticsResponse | null>(null);
  readonly exporting = signal(false);
  readonly exportError = signal<ParsedApiError | null>(null);

  ngOnInit(): void {
    this.cargar();
  }

  /** Recarga la lista y las estadísticas desde la API. */
  cargar(): void {
    this.error.set(null);
    this.loading.set(true);

    // Cargamos lista y estadísticas en paralelo con forkJoin implícito:
    // cada llamada es independiente, así que las hacemos por separado.
    this.service
      .statistics()
      .subscribe({ next: (s) => this.stats.set(s), error: () => {} });

    this.service
      .list()
      .pipe(finalize(() => this.loading.set(false)))
      .subscribe({
        next: (page) => this.empleados.set(page.items),
        error: (err: unknown) => this.error.set(parseApiError(err)),
      });
  }

  exportExcel(): void {
    this.exportError.set(null);
    this.exporting.set(true);
    this.service
      .export({ page: 0, size: 100 })
      .pipe(finalize(() => this.exporting.set(false)))
      .subscribe({
        next: (blob) => {
          const url = URL.createObjectURL(blob);
          const a = document.createElement('a');
          a.href = url;
          a.download = 'empleados_reporte.xlsx';
          a.click();
          URL.revokeObjectURL(url);
        },
        error: (err: unknown) => this.exportError.set(parseApiError(err)),
      });
  }
}
