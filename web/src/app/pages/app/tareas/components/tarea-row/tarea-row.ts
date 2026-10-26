import { Component, input } from '@angular/core';
import { RouterLink } from '@angular/router';

import { formatDateOnlyFullYear, formatDateTime } from '../../../../../core/i18n/format-date';
import type { TaskListItemResponse } from '../../../../../core/model/task/task.dto';
import { StatusBadgeComponent } from '../../../../../shared/ui/status-badge/status-badge';

/** Fila de la tabla de tareas. */
@Component({
  selector: '[app-tarea-row]',
  
  imports: [RouterLink, StatusBadgeComponent],
  templateUrl: './tarea-row.html',
})
export class TareaRowComponent {
  readonly tarea = input.required<TaskListItemResponse>();

  formatDate(iso: string | null): string {
    if (!iso) return '—';
    return iso.includes('T') ? formatDateTime(iso) : formatDateOnlyFullYear(iso);
  }

  get prioridadLabel(): string {
    const map: Record<string, string> = { LOW: 'Baja', MEDIUM: 'Media', HIGH: 'Alta', URGENT: 'Urgente' };
    return map[this.tarea().priority] ?? this.tarea().priority;
  }

  get prioridadColor(): string {
    const map: Record<string, string> = {
      LOW: 'text-on-surface-variant',
      MEDIUM: 'text-on-surface',
      HIGH: 'text-orange-600',
      URGENT: 'text-primary font-bold',
    };
    return map[this.tarea().priority] ?? '';
  }
}
