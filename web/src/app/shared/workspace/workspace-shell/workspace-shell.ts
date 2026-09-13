import { Component, HostListener, inject, OnInit, signal } from '@angular/core';
import { NavigationEnd, Router, RouterOutlet } from '@angular/router';
import { filter } from 'rxjs';

import { SessionContextService } from '../../../core/auth/session-context.service';
import { WorkspaceFooterComponent } from '../workspace-footer/workspace-footer';
import { WorkspaceSidebarComponent } from '../workspace-sidebar/workspace-sidebar';
import { AsistenciaHoyModalComponent } from '../../ui/asistencia-hoy-modal/asistencia-hoy-modal';
import { AsistenciaBusquedaModalComponent } from '../../ui/asistencia-busqueda-modal/asistencia-busqueda-modal';
import { UserProfileModalComponent } from '../../ui/user-profile-modal/user-profile-modal';
import { BRAND_LOGO_URL } from '../../../pages/home/brand';

@Component({
  selector: 'app-workspace-shell',

  imports: [
    RouterOutlet,
    WorkspaceSidebarComponent,
    WorkspaceFooterComponent,
    AsistenciaHoyModalComponent,
    AsistenciaBusquedaModalComponent,
    UserProfileModalComponent,
  ],
  templateUrl: './workspace-shell.html',
})
export class WorkspaceShellComponent implements OnInit {
  private readonly session = inject(SessionContextService);
  private readonly router = inject(Router);

  readonly logoUrl = BRAND_LOGO_URL;
  readonly mobileNavOpen = signal(false);
  readonly mostrarAsistenciaHoy = signal(false);
  readonly mostrarAsistenciaBusqueda = signal(false);
  readonly mostrarPerfil = signal(false);

  ngOnInit(): void {
    this.session.load();
    this.router.events
      .pipe(filter((event) => event instanceof NavigationEnd))
      .subscribe(() => this.mobileNavOpen.set(false));
  }

  @HostListener('document:keydown.escape')
  closeMobileNav(): void {
    this.mobileNavOpen.set(false);
  }
}
