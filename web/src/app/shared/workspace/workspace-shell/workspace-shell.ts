import { Component, inject, OnInit, signal } from '@angular/core';
import { RouterOutlet } from '@angular/router';

import { SessionContextService } from '../../../core/auth/session-context.service';
import { WorkspaceFooterComponent } from '../workspace-footer/workspace-footer';
import { WorkspaceSidebarComponent } from '../workspace-sidebar/workspace-sidebar';
import { AsistenciaHoyModalComponent } from '../../ui/asistencia-hoy-modal/asistencia-hoy-modal';
import { AsistenciaBusquedaModalComponent } from '../../ui/asistencia-busqueda-modal/asistencia-busqueda-modal';

@Component({
  selector: 'app-workspace-shell',
  
  imports: [
    RouterOutlet,
    WorkspaceSidebarComponent,
    WorkspaceFooterComponent,
    AsistenciaHoyModalComponent,
    AsistenciaBusquedaModalComponent,
  ],
  templateUrl: './workspace-shell.html',
})
export class WorkspaceShellComponent implements OnInit {
  private readonly session = inject(SessionContextService);

  readonly mostrarAsistenciaHoy = signal(false);
  readonly mostrarAsistenciaBusqueda = signal(false);

  ngOnInit(): void {
    this.session.load();
  }
}
