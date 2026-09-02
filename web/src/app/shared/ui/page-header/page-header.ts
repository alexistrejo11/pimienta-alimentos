import { Component, input } from '@angular/core';
import { RouterLink } from '@angular/router';

/**
 * Cabecera de página reutilizable.
 *
 * Muestra el título principal, un subtítulo opcional y,
 * cuando se proporciona `backLink`, una flecha de regreso
 * con la etiqueta `backLabel`.
 *
 * Uso en una página de detalle:
 *   <app-page-header
 *     title="Detalle de empleado"
 *     subtitle="Información completa del colaborador"
 *     backLink="/app/empleados"
 *     backLabel="Empleados"
 *   />
 */
@Component({
  selector: 'app-page-header',
  
  imports: [RouterLink],
  templateUrl: './page-header.html',
})
export class PageHeaderComponent {
  /** Título principal de la sección (obligatorio). */
  readonly title = input.required<string>();

  /** Descripción o subtítulo debajo del título (opcional). */
  readonly subtitle = input<string>();

  /** Ruta de regreso; si se omite, no se muestra la flecha (opcional). */
  readonly backLink = input<string>();

  /** Texto del enlace de regreso (por defecto "Regresar"). */
  readonly backLabel = input<string>('Regresar');
}
