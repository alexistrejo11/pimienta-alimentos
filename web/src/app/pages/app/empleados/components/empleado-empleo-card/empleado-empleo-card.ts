import { Component, input } from '@angular/core';

import type { EmployeeResponse } from '../../../../../core/model/employee/employee.dto';
import { StatusBadgeComponent } from '../../../../../shared/ui/status-badge/status-badge';

/** Tarjeta de información laboral del empleado: puesto, depto, contrato, turno, fechas. */
@Component({
  selector: 'app-empleado-empleo-card',
  
  imports: [StatusBadgeComponent],
  templateUrl: './empleado-empleo-card.html',
})
export class EmpleadoEmpleoCardComponent {
  readonly empleado = input.required<EmployeeResponse>();

  /** Traduce el tipo de contrato al español. */
  get contrato(): string {
    const map: Record<string, string> = {
      INDEFINITE: 'Indefinido',
      FIXED_TERM: 'Plazo fijo',
      PROJECT_BASED: 'Por proyecto',
      TEMPORARY: 'Temporal',
      FREELANCE: 'Freelance',
    };
    return map[this.empleado().contractType] ?? this.empleado().contractType;
  }

  /** Traduce el turno de trabajo al español. */
  get turno(): string {
    const map: Record<string, string> = {
      MORNING: 'Matutino',
      AFTERNOON: 'Vespertino',
      NIGHT: 'Nocturno',
      MIXED: 'Mixto',
      REMOTE: 'Remoto',
    };
    return map[this.empleado().workShift] ?? this.empleado().workShift;
  }
}
