import { Component, OnInit, input, signal, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { finalize } from 'rxjs';

import { EmployeeService } from '../../../../../core/employees/employee.service';
import { parseApiError, type ParsedApiError } from '../../../../../core/http/parse-api-error';
import type { AttendanceResponse, AttendanceStatus } from '../../../../../core/model/employee/attendance.dto';
import type { PagedResponse } from '../../../../../core/model/common/pagination';
import { DataStateComponent } from '../../../../../shared/ui/data-state/data-state';

const STATUS_LABELS: Record<AttendanceStatus, string> = {
  UNDEFINED: 'Indefinido',
  CHECKED_IN: 'Entrada registrada',
  CHECKED_OUT: 'Salida registrada',
  AUTO_CLOSED_EXCEEDED_MAX_SHIFT_HOURS: 'Cierre automático (excedió horas)',
  AUTO_CLOSED_ASSUMED_CONTRACT_DAY: 'Cierre automático (jornada contractual)',
};

/**
 * Tarjeta de historial de asistencias de un empleado.
 * Permite registrar entrada y salida del día, y ver el historial paginado.
 */
@Component({
  selector: 'app-empleado-asistencia-card',
  
  imports: [FormsModule, DataStateComponent],
  templateUrl: './empleado-asistencia-card.html',
})
export class EmpleadoAsistenciaCardComponent implements OnInit {
  readonly empleadoId = input.required<number>();

  private readonly service = inject(EmployeeService);

  // ── Estado listado ────────────────────────────────────────────────────────
  readonly loading = signal(true);
  readonly error = signal<ParsedApiError | null>(null);
  readonly page = signal<PagedResponse<AttendanceResponse> | null>(null);

  // ── Filtros de rango de fechas ────────────────────────────────────────────
  fechaDesde = '';
  fechaHasta = '';

  // ── Check-in / Check-out ──────────────────────────────────────────────────
  readonly checkinOpen = signal(false);
  readonly checkoutOpen = signal(false);
  readonly actionLoading = signal(false);
  readonly actionError = signal<ParsedApiError | null>(null);
  readonly actionSuccess = signal('');

  /** headquarterId default (sin selector de sede en esta tarjeta). */
  headquarterId = 1;
  checkinPhoto: File | null = null;
  checkoutPhoto: File | null = null;

  ngOnInit(): void {
    this.cargar();
  }

  cargar(): void {
    this.error.set(null);
    this.loading.set(true);
    this.service
      .listAttendances(this.empleadoId(), {
        workDateFrom: this.fechaDesde || undefined,
        workDateTo: this.fechaHasta || undefined,
      })
      .pipe(finalize(() => this.loading.set(false)))
      .subscribe({
        next: (r) => this.page.set(r),
        error: (err: unknown) => this.error.set(parseApiError(err)),
      });
  }

  get records(): AttendanceResponse[] {
    return this.page()?.items ?? [];
  }

  statusLabel(s: AttendanceStatus): string {
    return STATUS_LABELS[s] ?? s;
  }

  statusClasses(s: AttendanceStatus): string {
    if (s === 'CHECKED_IN') return 'bg-green-100 text-green-800 dark:bg-green-900/30 dark:text-green-300';
    if (s === 'CHECKED_OUT') return 'bg-blue-100 text-blue-800 dark:bg-blue-900/30 dark:text-blue-300';
    return 'bg-stone-100 text-stone-700 dark:bg-stone-800 dark:text-stone-300';
  }

  formatMinutes(m: number): string {
    if (!m) return '—';
    const h = Math.floor(m / 60);
    const min = m % 60;
    return h > 0 ? `${h}h ${min}min` : `${min}min`;
  }

  formatDateTime(iso: string | null): string {
    if (!iso) return '—';
    return new Date(iso).toLocaleString('es-MX', {
      day: '2-digit', month: 'short', year: 'numeric',
      hour: '2-digit', minute: '2-digit',
    });
  }

  // ── Acciones ──────────────────────────────────────────────────────────────

  onCheckinPhotoChange(event: Event): void {
    const input = event.target as HTMLInputElement;
    this.checkinPhoto = input.files?.[0] ?? null;
  }

  onCheckoutPhotoChange(event: Event): void {
    const input = event.target as HTMLInputElement;
    this.checkoutPhoto = input.files?.[0] ?? null;
  }

  registrarEntrada(): void {
    this.actionLoading.set(true);
    this.actionError.set(null);
    this.service
      .startWorkday(
        this.empleadoId(),
        { headquarterId: this.headquarterId },
        this.checkinPhoto ?? undefined,
      )
      .pipe(finalize(() => this.actionLoading.set(false)))
      .subscribe({
        next: () => {
          this.checkinOpen.set(false);
          this.checkinPhoto = null;
          this.actionSuccess.set('Entrada registrada correctamente.');
          setTimeout(() => this.actionSuccess.set(''), 3500);
          this.cargar();
        },
        error: (err: unknown) => this.actionError.set(parseApiError(err)),
      });
  }

  registrarSalida(): void {
    this.actionLoading.set(true);
    this.actionError.set(null);
    this.service
      .endWorkday(
        this.empleadoId(),
        {},
        this.checkoutPhoto ?? undefined,
      )
      .pipe(finalize(() => this.actionLoading.set(false)))
      .subscribe({
        next: () => {
          this.checkoutOpen.set(false);
          this.checkoutPhoto = null;
          this.actionSuccess.set('Salida registrada correctamente.');
          setTimeout(() => this.actionSuccess.set(''), 3500);
          this.cargar();
        },
        error: (err: unknown) => this.actionError.set(parseApiError(err)),
      });
  }
}
