import { Component, input } from '@angular/core';

import { formatDateOnlyFullYear } from '../../../../../../core/i18n/format-date';
import type { ProjectMilestoneResponse } from '../../../../../../core/model/crm/project-milestone.dto';
import { StatusBadgeComponent } from '../../../../../../shared/ui/status-badge/status-badge';

/** Fila de un hito dentro de la lista de hitos de un proyecto. */
@Component({
  selector: 'app-hito-item',
  
  imports: [StatusBadgeComponent],
  templateUrl: './hito-item.html',
})
export class HitoItemComponent {
  readonly hito = input.required<ProjectMilestoneResponse>();

  formatDate(iso: string | null): string {
    if (!iso) return '—';
    return formatDateOnlyFullYear(iso);
  }

  formatMXN(value: number): string {
    return new Intl.NumberFormat('es-MX', { style: 'currency', currency: 'MXN', maximumFractionDigits: 0 }).format(value);
  }
}
