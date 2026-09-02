import { Component, input, output } from '@angular/core';

import type { ParsedApiError } from '../../../core/http/parse-api-error';

/**
 * Componente contenedor que gestiona los cuatro estados comunes de carga de datos:
 *  1. Cargando  → muestra spinner con mensaje
 *  2. Error     → muestra banner de error con botón de reintento
 *  3. Vacío     → muestra mensaje de lista vacía
 *  4. Con datos → proyecta el contenido real con <ng-content>
 *
 * Esto evita repetir el mismo bloque @if/@else en cada página.
 *
 * Uso:
 *   <app-data-state [loading]="loading()" [error]="error()" [empty]="items().length === 0" (retry)="load()">
 *     <!-- contenido real va aquí -->
 *   </app-data-state>
 */
@Component({
  selector: 'app-data-state',
  
  templateUrl: './data-state.html',
})
export class DataStateComponent {
  /** Indica si la petición HTTP está en curso. */
  readonly loading = input.required<boolean>();

  /** Error parseado de la API; null si no hay error. */
  readonly error = input<ParsedApiError | null>(null);

  /**
   * Indica si la lista de resultados está vacía.
   * Solo se evalúa cuando loading=false y error=null.
   */
  readonly empty = input<boolean>(false);

  /** Mensaje personalizable para el estado vacío. */
  readonly emptyMessage = input<string>('No hay registros que mostrar.');

  /** Se emite cuando el usuario pulsa "Reintentar". */
  readonly retry = output<void>();
}
