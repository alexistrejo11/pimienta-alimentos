import { Component, input } from '@angular/core';
import { EmployeeResponse } from '../../../../../core/model';


/** Tarjeta de nómina e IMSS del empleado: salario, bonos, vales, tipo IMSS, factor de integración. */
@Component({
  selector: 'app-empleado-nomina-card',
  
  templateUrl: './empleado-nomina-card.html',
})
export class EmpleadoNominaCardComponent {
  readonly empleado = input.required<EmployeeResponse>();

  /** Formatea un número como pesos mexicanos con dos decimales. */
  formatMXN(value: number): string {
    return new Intl.NumberFormat('es-MX', { style: 'currency', currency: 'MXN' }).format(value);
  }

  get tipoTrabajadorImss(): string {
    const map: Record<string, string> = {
      PERMANENT_URBAN: 'Permanente urbano',
      EVENTUAL_URBAN: 'Eventual urbano',
      PERMANENT_RURAL: 'Permanente rural',
      EVENTUAL_RURAL: 'Eventual rural',
    };
    return map[this.empleado().imssWorkerType] ?? this.empleado().imssWorkerType;
  }

  get tipoSalarioImss(): string {
    const map: Record<string, string> = {
      FIXED: 'Fijo',
      VARIABLE: 'Variable',
      MIXED: 'Mixto',
    };
    return map[this.empleado().imssSalaryType] ?? this.empleado().imssSalaryType;
  }
}
