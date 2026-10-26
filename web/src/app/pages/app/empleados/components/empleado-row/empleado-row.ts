import { Component, input } from '@angular/core';
import { RouterLink } from '@angular/router';

import { formatDateOnlyFullYear } from '../../../../../core/i18n/format-date';
import type { EmployeeListItemResponse } from '../../../../../core/model/employee/employee.dto';
import { StatusBadgeComponent } from '../../../../../shared/ui/status-badge/status-badge';

/**
 * Fila de la tabla de empleados.
 * Recibe un único empleado y renderiza sus datos como <tr>.
 * Separarlo aquí evita que la plantilla de la página crezca con lógica de presentación.
 */
@Component({
  selector: '[app-empleado-row]', // selector de atributo para poder usarlo dentro de <tbody>
  
  imports: [RouterLink, StatusBadgeComponent],
  templateUrl: './empleado-row.html',
})
export class EmpleadoRowComponent {
  /** Datos resumidos del empleado que provienen de la lista paginada. */
  readonly empleado = input.required<EmployeeListItemResponse>();

  /** Fecha de ingreso: dd/MM/yyyy, sin interpretar el día como instante UTC. */
  formatDate(iso: string): string {
    if (!iso) return '—';
    return formatDateOnlyFullYear(iso);
  }

  onPhotoError(event: Event): void {
    const img = event.target as HTMLImageElement;
    img.style.visibility = 'hidden';
  }
}
