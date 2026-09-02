import { Component, output } from '@angular/core';
import { RouterLink, RouterLinkActive } from '@angular/router';

import { BRAND_LOGO_URL } from '../../../pages/home/brand';

@Component({
  selector: 'app-workspace-sidebar',
  
  imports: [RouterLink, RouterLinkActive],
  templateUrl: './workspace-sidebar.html',
})
export class WorkspaceSidebarComponent {
  readonly logoUrl = BRAND_LOGO_URL;

  readonly abrirAsistenciaHoy = output<void>();
  readonly abrirAsistenciaBusqueda = output<void>();

  /** Base styles; active state is layered via {@link RouterLinkActive}. */
  readonly navLinkInactive =
    'flex items-center gap-3 rounded-lg px-4 py-3 text-sm font-bold tracking-tight text-stone-600 transition-colors hover:bg-stone-100 dark:text-stone-400 dark:hover:bg-stone-900';
}
