import { Component, inject, output } from '@angular/core';
import { RouterLink, RouterLinkActive } from '@angular/router';

import { SessionContextService } from '../../../core/auth/session-context.service';
import { BRAND_LOGO_URL } from '../../../pages/home/brand';

@Component({
  selector: 'app-workspace-sidebar',
  
  imports: [RouterLink, RouterLinkActive],
  templateUrl: './workspace-sidebar.html',
})
export class WorkspaceSidebarComponent {
  private readonly session = inject(SessionContextService);

  readonly logoUrl = BRAND_LOGO_URL;
  readonly canAccessPos = this.session.canAccessPos;
  readonly isAdmin = this.session.isAdmin;

  readonly abrirAsistenciaHoy = output<void>();
  readonly abrirAsistenciaBusqueda = output<void>();
  readonly abrirPerfil = output<void>();

  /** Base styles; active state is layered via {@link RouterLinkActive}. */
  readonly navLinkInactive =
    'flex items-center gap-3 rounded-lg px-4 py-3 text-sm font-bold tracking-tight text-on-surface-variant transition-colors hover:bg-surface-container';
}
