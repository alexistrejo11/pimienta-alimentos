import { Component, OnInit, input, signal, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { finalize } from 'rxjs';

import { EmployeeService } from '../../../../../core/employees/employee.service';
import { parseApiError, type ParsedApiError } from '../../../../../core/http/parse-api-error';
import type { DayOfWeek, WorkDayScheduleSlotRequest, WorkDayScheduleSlotResponse } from '../../../../../core/model/employee/schedule.dto';
import { DataStateComponent } from '../../../../../shared/ui/data-state/data-state';

const DAYS: { key: DayOfWeek; label: string }[] = [
  { key: 'MONDAY', label: 'Lunes' },
  { key: 'TUESDAY', label: 'Martes' },
  { key: 'WEDNESDAY', label: 'Miércoles' },
  { key: 'THURSDAY', label: 'Jueves' },
  { key: 'FRIDAY', label: 'Viernes' },
  { key: 'SATURDAY', label: 'Sábado' },
  { key: 'SUNDAY', label: 'Domingo' },
];

/**
 * Tarjeta del horario semanal del empleado.
 * Muestra los slots configurados y permite editarlos inline.
 */
@Component({
  selector: 'app-empleado-horario-card',
  
  imports: [FormsModule, DataStateComponent],
  templateUrl: './empleado-horario-card.html',
})
export class EmpleadoHorarioCardComponent implements OnInit {
  readonly empleadoId = input.required<number>();

  private readonly service = inject(EmployeeService);

  readonly loading = signal(true);
  readonly saving = signal(false);
  readonly error = signal<ParsedApiError | null>(null);
  readonly saveError = signal<ParsedApiError | null>(null);
  readonly saveSuccess = signal(false);
  readonly editMode = signal(false);

  readonly slots = signal<WorkDayScheduleSlotResponse[]>([]);

  /** Draft slots used during editing. */
  readonly draftSlots = signal<WorkDayScheduleSlotRequest[]>([]);

  readonly days = DAYS;

  ngOnInit(): void {
    this.cargar();
  }

  cargar(): void {
    this.error.set(null);
    this.loading.set(true);
    this.service
      .getWorkSchedule(this.empleadoId())
      .pipe(finalize(() => this.loading.set(false)))
      .subscribe({
        next: (r) => this.slots.set(r.slots ?? []),
        error: (err: unknown) => this.error.set(parseApiError(err)),
      });
  }

  labelFor(key: DayOfWeek): string {
    return DAYS.find((d) => d.key === key)?.label ?? key;
  }

  startEdit(): void {
    this.saveError.set(null);
    this.saveSuccess.set(false);
    this.draftSlots.set(
      DAYS.map((d) => {
        const existing = this.slots().find((s) => s.dayOfWeek === d.key);
        return {
          dayOfWeek: d.key,
          startTime: existing?.startTime ?? '',
          endTime: existing?.endTime ?? '',
          enabled: !!existing,
        } as WorkDayScheduleSlotRequest & { enabled: boolean };
      }),
    );
    this.editMode.set(true);
  }

  cancelEdit(): void {
    this.editMode.set(false);
  }

  save(): void {
    const active = (this.draftSlots() as (WorkDayScheduleSlotRequest & { enabled: boolean })[])
      .filter((s) => s.enabled && s.startTime && s.endTime)
      .map(({ dayOfWeek, startTime, endTime }) => ({ dayOfWeek, startTime, endTime }));

    this.saving.set(true);
    this.saveError.set(null);
    this.service
      .updateWorkSchedule(this.empleadoId(), { slots: active })
      .pipe(finalize(() => this.saving.set(false)))
      .subscribe({
        next: (r) => {
          this.slots.set(r.slots ?? []);
          this.editMode.set(false);
          this.saveSuccess.set(true);
          setTimeout(() => this.saveSuccess.set(false), 3000);
        },
        error: (err: unknown) => this.saveError.set(parseApiError(err)),
      });
  }

  /** Expose drafts with enabled flag for template binding. */
  get typedDrafts(): (WorkDayScheduleSlotRequest & { enabled: boolean })[] {
    return this.draftSlots() as (WorkDayScheduleSlotRequest & { enabled: boolean })[];
  }
}
