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

  /** Iniciales a partir del nombre completo (p. ej. "María González" → "MG"). */
  initials(fullName: string): string {
    const parts = fullName.trim().split(/\s+/).filter(Boolean);
    if (parts.length === 0) return '?';
    if (parts.length === 1) return parts[0].charAt(0).toUpperCase();
    return (parts[0].charAt(0) + parts[parts.length - 1].charAt(0)).toUpperCase();
  }

  /** Formatea la fecha ISO a formato legible en español (dd/mm/aaaa). */
  formatDate(iso: string): string {
    if (!iso) return '—';
    const [year, month, day] = iso.split('-');
    return `${day}/${month}/${year}`;
  }
}
