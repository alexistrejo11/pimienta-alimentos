import { Component, input } from '@angular/core';
import { RouterLink } from '@angular/router';

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

  /** Formatea la fecha ISO a formato legible en español (dd/mm/aaaa). */
  formatDate(iso: string): string {
    if (!iso) return '—';
    const [year, month, day] = iso.split('-');
    return `${day}/${month}/${year}`;
  }

  onPhotoError(event: Event): void {
    const img = event.target as HTMLImageElement;
    img.style.visibility = 'hidden';
  }
}
