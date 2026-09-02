import { Component, signal } from '@angular/core';
import { RouterOutlet } from '@angular/router';

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
export class WorkspaceShellComponent {
  readonly mostrarAsistenciaHoy = signal(false);
  readonly mostrarAsistenciaBusqueda = signal(false);
}
