import { Component, input } from '@angular/core';
import { EmployeeResponse } from '../../../../../core/model';


/**
 * Tarjeta de información personal del empleado.
 * Muestra: nombre, email, teléfono, dirección, RFC, CURP, NSS, CLABE bancaria.
 */
@Component({
  selector: 'app-empleado-info-card',
  templateUrl: './empleado-info-card.html',
})
export class EmpleadoInfoCardComponent {
  readonly empleado = input.required<EmployeeResponse>();
}
