import { Component, input } from '@angular/core';
import { RouterLink } from '@angular/router';

import type { ProjectResponse } from '../../../../../../core/model/crm/project.dto';
import { StatusBadgeComponent } from '../../../../../../shared/ui/status-badge/status-badge';

/** Fila de la tabla de proyectos. */
@Component({
  selector: '[app-proyecto-row]',
  
  imports: [RouterLink, StatusBadgeComponent],
  templateUrl: './proyecto-row.html',
})
export class ProyectoRowComponent {
  readonly proyecto = input.required<ProjectResponse>();

  formatDate(iso: string | null): string {
    if (!iso) return '—';
    const [y, m, d] = iso.split('-');
    return `${d}/${m}/${y}`;
  }

  get prioridadLabel(): string {
    const map: Record<string, string> = {
      LOW: 'Baja',
      MEDIUM: 'Media',
      HIGH: 'Alta',
      CRITICAL: 'Crítica',
    };
    return map[this.proyecto().priority] ?? this.proyecto().priority;
  }

  get prioridadColor(): string {
    const map: Record<string, string> = {
      LOW: 'text-on-surface-variant',
      MEDIUM: 'text-on-surface',
      HIGH: 'text-orange-600',
      CRITICAL: 'text-primary font-bold',
    };
    return map[this.proyecto().priority] ?? '';
  }
}
