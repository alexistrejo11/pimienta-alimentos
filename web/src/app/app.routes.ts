import { Routes } from '@angular/router';

import { AvisoPrivacidadPage } from './pages/home/aviso-privacidad-page/aviso-privacidad-page';
import { CalidadHigienicaPage } from './pages/home/calidad-higienica-page/calidad-higienica-page';
import { Home } from './pages/home/home/home';
import { TerminosServicioPage } from './pages/home/terminos-servicio-page/terminos-servicio-page';
import { ClientsPageComponent } from './pages/clients/clients-page';
import { DashboardPageComponent } from './pages/dashboard/dashboard-page';
import { InventoryPageComponent } from './pages/app/inventory/inventory-page';
import { TasksPageComponent } from './pages/app/tasks/tasks-page';
import { WorkspaceShellComponent } from './shared/workspace/workspace-shell/workspace-shell';
import { Login } from './pages/auth/login/login';
import { Register } from './pages/auth/register/register';
import { workspaceAuthGuard } from './core/auth/workspace-auth.guard';

// ── Módulos nuevos ──────────────────────────────────────────────────────────
import { EmpleadosPageComponent } from './pages/app/empleados/empleados-page';
import { EmpleadoDetailPageComponent } from './pages/app/empleados/empleado-detail/empleado-detail-page';
import { EmpleadoFormPageComponent } from './pages/app/empleados/empleado-form-page/empleado-form-page';
import { OportunidadesPageComponent } from './pages/app/crm/oportunidades/oportunidades-page';
import { OportunidadDetailPageComponent } from './pages/app/crm/oportunidades/oportunidad-detail/oportunidad-detail-page';
import { OportunidadFormPageComponent } from './pages/app/crm/oportunidades/oportunidad-form-page/oportunidad-form-page';
import { ProyectosPageComponent } from './pages/app/crm/proyectos/proyectos-page';
import { ProyectoDetailPageComponent } from './pages/app/crm/proyectos/proyecto-detail/proyecto-detail-page';
import { ProyectoFormPageComponent } from './pages/app/crm/proyectos/proyecto-form-page/proyecto-form-page';
import { TareasPageComponent } from './pages/app/tareas/tareas-page';
import { TareaDetailPageComponent } from './pages/app/tareas/tarea-detail/tarea-detail-page';
import { TareaFormPageComponent } from './pages/app/tareas/tarea-form/tarea-form-page';
import { SedesPageComponent } from './pages/app/sedes/sedes-page';
import { SedeDetailPageComponent } from './pages/app/sedes/sede-detail/sede-detail-page';
import { ContratosPageComponent } from './pages/app/contratos/contratos-page';
import { NominaPageComponent } from './pages/app/nomina/nomina-page';
import { ArchivosPageComponent } from './pages/app/archivos/archivos-page';

export const routes: Routes = [
  {
    path: 'auth/login',
    component: Login,
  },
  {
    path: 'auth/register',
    component: Register,
  },
  {
    path: 'app',
    component: WorkspaceShellComponent,
    canActivate: [workspaceAuthGuard],
    children: [
      { path: '', pathMatch: 'full', redirectTo: 'dashboard' },
      { path: 'dashboard', component: DashboardPageComponent },

      // Módulos originales (se mantienen para no romper referencias existentes)
      { path: 'clients', component: ClientsPageComponent },
      { path: 'inventory', component: InventoryPageComponent },
      { path: 'tasks', component: TasksPageComponent },

      // ── Empleados ────────────────────────────────────────────────────────
      { path: 'empleados/nuevo', component: EmpleadoFormPageComponent },
      { path: 'empleados/:id/editar', component: EmpleadoFormPageComponent },
      { path: 'empleados/:id', component: EmpleadoDetailPageComponent },
      { path: 'empleados', component: EmpleadosPageComponent },

      // ── CRM: Oportunidades (rutas estáticas y edición antes de `:id` detalle) ─
      { path: 'crm/oportunidades/nueva', component: OportunidadFormPageComponent },
      { path: 'crm/oportunidades/:id/editar', component: OportunidadFormPageComponent },
      { path: 'crm/oportunidades/:id', component: OportunidadDetailPageComponent },
      { path: 'crm/oportunidades', component: OportunidadesPageComponent },

      // ── CRM: Proyectos ───────────────────────────────────────────────────
      { path: 'crm/proyectos/nuevo', component: ProyectoFormPageComponent },
      { path: 'crm/proyectos/:id/editar', component: ProyectoFormPageComponent },
      { path: 'crm/proyectos/:id', component: ProyectoDetailPageComponent },
      { path: 'crm/proyectos', component: ProyectosPageComponent },

      // ── Tareas ───────────────────────────────────────────────────────────
      { path: 'tareas/nueva', component: TareaFormPageComponent },
      { path: 'tareas/:id', component: TareaDetailPageComponent },
      { path: 'tareas', component: TareasPageComponent },

      // ── Sedes ────────────────────────────────────────────────────────────
      { path: 'sedes', component: SedesPageComponent },
      { path: 'sedes/:id', component: SedeDetailPageComponent },

      // ── Contratos ─────────────────────────────────────────────────────────
      { path: 'contratos', component: ContratosPageComponent },

      // ── Archivos ──────────────────────────────────────────────────────────
      { path: 'archivos', component: ArchivosPageComponent },

      // ── Nómina ────────────────────────────────────────────────────────────
      { path: 'nomina', component: NominaPageComponent },
    ],
  },
  { path: '', component: Home },
  { path: 'aviso-privacidad', component: AvisoPrivacidadPage },
  { path: 'terminos-servicio', component: TerminosServicioPage },
  { path: 'calidad-higienica', component: CalidadHigienicaPage },
];
