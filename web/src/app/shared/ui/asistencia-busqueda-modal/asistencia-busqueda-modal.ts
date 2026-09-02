import { Component, inject, output, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { finalize } from 'rxjs';

import { AttendanceService } from '../../../core/employees/attendance.service';
import { parseApiError, type ParsedApiError } from '../../../core/http/parse-api-error';
import type { AttendanceResponse, AttendanceStatus } from '../../../core/model/employee/attendance.dto';
import type { PagedResponse } from '../../../core/model/common/pagination';

const STATUS_LABELS: Record<AttendanceStatus, string> = {
  UNDEFINED: 'Indefinido',
  CHECKED_IN: 'En turno',
  CHECKED_OUT: 'Salió',
  AUTO_CLOSED_EXCEEDED_MAX_SHIFT_HOURS: 'Cierre auto (horas)',
  AUTO_CLOSED_ASSUMED_CONTRACT_DAY: 'Cierre auto (jornada)',
};

const ALL_STATUSES: AttendanceStatus[] = [
  'CHECKED_IN',
  'CHECKED_OUT',
  'AUTO_CLOSED_EXCEEDED_MAX_SHIFT_HOURS',
  'AUTO_CLOSED_ASSUMED_CONTRACT_DAY',
];

/**
 * Modal de búsqueda libre de asistencias usando todos los filtros disponibles del API.
 */
@Component({
  selector: 'app-asistencia-busqueda-modal',
  
  imports: [FormsModule],
  templateUrl: './asistencia-busqueda-modal.html',
})
export class AsistenciaBusquedaModalComponent {
  readonly cerrar = output<void>();

  private readonly service = inject(AttendanceService);

  // Filtros
  empleadoId = '';
  fechaExacta = '';
  fechaDesde = '';
  fechaHasta = '';
  statusFilter: AttendanceStatus | '' = '';
  soloAbiertos = false;

  readonly loading = signal(false);
  readonly error = signal<ParsedApiError | null>(null);
  readonly page = signal<PagedResponse<AttendanceResponse> | null>(null);
  readonly searched = signal(false);

  readonly allStatuses = ALL_STATUSES;

  get records(): AttendanceResponse[] {
    return this.page()?.items ?? [];
  }

  buscar(): void {
    this.error.set(null);
    this.loading.set(true);
    this.searched.set(true);

    this.service
      .search({
        employeeId: this.empleadoId ? Number(this.empleadoId) : undefined,
        workDate: this.fechaExacta || undefined,
        workDateFrom: this.fechaDesde || undefined,
        workDateTo: this.fechaHasta || undefined,
        status: this.statusFilter || undefined,
        onlyOpen: this.soloAbiertos || undefined,
        size: 50,
      })
      .pipe(finalize(() => this.loading.set(false)))
      .subscribe({
        next: (r) => this.page.set(r),
        error: (err: unknown) => this.error.set(parseApiError(err)),
      });
  }

  limpiar(): void {
    this.empleadoId = '';
    this.fechaExacta = '';
    this.fechaDesde = '';
    this.fechaHasta = '';
    this.statusFilter = '';
    this.soloAbiertos = false;
    this.page.set(null);
    this.searched.set(false);
  }

  statusLabel(s: AttendanceStatus): string {
    return STATUS_LABELS[s] ?? s;
  }

  statusClasses(s: AttendanceStatus): string {
    if (s === 'CHECKED_IN') return 'bg-green-100 text-green-800 dark:bg-green-900/30 dark:text-green-300';
    if (s === 'CHECKED_OUT') return 'bg-blue-100 text-blue-800 dark:bg-blue-900/30 dark:text-blue-300';
    return 'bg-stone-100 text-stone-600 dark:bg-stone-800 dark:text-stone-400';
  }

  formatDateTime(iso: string | null): string {
    if (!iso) return '—';
    return new Date(iso).toLocaleString('es-MX', {
      day: '2-digit', month: 'short', year: 'numeric',
      hour: '2-digit', minute: '2-digit',
    });
  }

  formatMinutes(m: number): string {
    if (!m) return '—';
    const h = Math.floor(m / 60);
    const min = m % 60;
    return h > 0 ? `${h}h ${min}m` : `${min}m`;
  }
}
