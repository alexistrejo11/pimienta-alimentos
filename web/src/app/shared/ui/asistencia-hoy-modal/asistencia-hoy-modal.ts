import { Component, inject, output, signal } from '@angular/core';
import { finalize } from 'rxjs';

import { AttendanceService } from '../../../core/employees/attendance.service';
import { parseApiError, type ParsedApiError } from '../../../core/http/parse-api-error';
import { attendanceStatusLabel } from '../../../core/i18n/enum-labels';
import type { AttendanceResponse, AttendanceStatus } from '../../../core/model/employee/attendance.dto';
import type { PagedResponse } from '../../../core/model/common/pagination';

/**
 * Panel modal global que muestra las asistencias del día en curso
 * para todos los empleados de la organización.
 * Se renderiza desde el workspace shell y se controla con una señal booleana.
 */
@Component({
  selector: 'app-asistencia-hoy-modal',
  
  templateUrl: './asistencia-hoy-modal.html',
})
export class AsistenciaHoyModalComponent {
  readonly cerrar = output<void>();

  private readonly service = inject(AttendanceService);

  readonly loading = signal(false);
  readonly error = signal<ParsedApiError | null>(null);
  readonly page = signal<PagedResponse<AttendanceResponse> | null>(null);

  constructor() {
    this.cargar();
  }

  cargar(): void {
    this.error.set(null);
    this.loading.set(true);
    this.service
      .forToday(undefined, 0, 50)
      .pipe(finalize(() => this.loading.set(false)))
      .subscribe({
        next: (r) => this.page.set(r),
        error: (err: unknown) => this.error.set(parseApiError(err)),
      });
  }

  get records(): AttendanceResponse[] {
    return this.page()?.items ?? [];
  }

  get totalHoy(): number {
    return this.page()?.metadata?.totalElements ?? 0;
  }

  get enTurno(): number {
    return this.records.filter((r) => r.status === 'CHECKED_IN').length;
  }

  statusLabel(s: AttendanceStatus): string {
    return attendanceStatusLabel(s);
  }

  statusClasses(s: AttendanceStatus): string {
    if (s === 'CHECKED_IN') return 'ui-badge-success';
    if (s === 'CHECKED_OUT') return 'ui-badge-info';
    return 'bg-surface-container text-on-surface-variant';
  }

  formatTime(iso: string | null): string {
    if (!iso) return '—';
    return new Date(iso).toLocaleTimeString('es-MX', { hour: '2-digit', minute: '2-digit' });
  }

  formatMinutes(m: number): string {
    if (!m) return '—';
    const h = Math.floor(m / 60);
    const min = m % 60;
    return h > 0 ? `${h}h ${min}m` : `${min}m`;
  }
}
