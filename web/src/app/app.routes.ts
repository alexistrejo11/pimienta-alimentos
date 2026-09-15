import { Routes } from '@angular/router';

import { AvisoPrivacidadPage } from './pages/home/aviso-privacidad-page/aviso-privacidad-page';
import { CalidadHigienicaPage } from './pages/home/calidad-higienica-page/calidad-higienica-page';
import { Home } from './pages/home/home/home';
import { TerminosServicioPage } from './pages/home/terminos-servicio-page/terminos-servicio-page';
import { ClientsPageComponent } from './pages/clients/clients-page';
import { DashboardPageComponent } from './pages/dashboard/dashboard-page';
import { TasksPageComponent } from './pages/app/tasks/tasks-page';
import { WorkspaceShellComponent } from './shared/workspace/workspace-shell/workspace-shell';
import { Login } from './pages/auth/login/login';
import { Register } from './pages/auth/register/register';
import { workspaceAuthGuard } from './core/auth/workspace-auth.guard';
import { roleGuard } from './core/auth/role.guard';

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
import { SedeFormPageComponent } from './pages/app/sedes/sede-form-page/sede-form-page';
import { SedePosPageComponent } from './pages/app/sedes/sede-pos-page/sede-pos-page';
import { PosConfigPageComponent } from './pages/app/pos/configuracion/pos-config-page';
import { ContratosPageComponent } from './pages/app/contratos/contratos-page';
import { NominaPageComponent } from './pages/app/nomina/nomina-page';
import { ArchivosPageComponent } from './pages/app/archivos/archivos-page';
import { CatalogoPageComponent } from './pages/app/catalogo/catalogo-page';
import { CatalogoFormPageComponent } from './pages/app/catalogo/catalogo-form-page/catalogo-form-page';
import { InventarioPageComponent } from './pages/app/inventario/inventario-page';
import { DispositivosPageComponent } from './pages/app/pos/dispositivos/dispositivos-page';
import { DispositivoDetailPageComponent } from './pages/app/pos/dispositivos/dispositivo-detail-page';
import { EnrolamientoPageComponent } from './pages/app/pos/enrolamiento/enrolamiento-page';
import { OperadoresPageComponent } from './pages/app/pos/operadores/operadores-page';
import { VentasPageComponent } from './pages/app/pos/ventas/ventas-page';
import { CortesRedirectComponent } from './pages/app/pos/cortes/cortes-redirect';
import { TurnosPageComponent } from './pages/app/pos/turnos/turnos-page';
import { IncidenciasPageComponent } from './pages/app/pos/incidencias/incidencias-page';
import { AccessRestrictedPageComponent } from './pages/app/access-restricted/access-restricted-page';
import { PendingApprovalPageComponent } from './pages/app/pending-approval/pending-approval-page';
import { PendingApprovalPage } from './pages/auth/pending-approval/pending-approval';
import { UsuariosPageComponent } from './pages/app/usuarios/usuarios-page';
import { AsistenciaPageComponent } from './pages/app/asistencia/asistencia-page';
import { MiAsistenciaPageComponent } from './pages/app/mi-asistencia/mi-asistencia-page';
import { AppRole } from './core/model/account/enums';
import { CountSessionListPageComponent } from './pages/app/inventario/counts/count-session-list-page';
import { CountSessionCreatePageComponent } from './pages/app/inventario/counts/count-session-create-page';
import { CountSessionDetailPageComponent } from './pages/app/inventario/counts/count-session-detail-page';
import { InventarioEntradasPageComponent } from './pages/app/inventario/entradas/entradas-page';
import { InventarioMermasPageComponent } from './pages/app/inventario/mermas/mermas-hq-page';
import { InventarioAjustesPageComponent } from './pages/app/inventario/ajustes/ajustes-page';
import { InventarioLedgerPageComponent } from './pages/app/inventario/ledger/inventario-ledger-page';
import { InventarioTransferPageComponent } from './pages/app/inventario/transferencias/inventario-transfer-page';

const ADMIN = [AppRole.ADMIN];
const ADMIN_MANAGER = [AppRole.ADMIN, AppRole.MANAGER];
const INVENTORY_READ = [AppRole.ADMIN, AppRole.MANAGER, AppRole.POS_OPERATOR];
const POS_STAFF = [AppRole.ADMIN, AppRole.MANAGER, AppRole.POS_OPERATOR];

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
    path: 'auth/pendiente-aprobacion',
    component: PendingApprovalPage,
  },
  {
    path: 'app',
    component: WorkspaceShellComponent,
    canActivate: [workspaceAuthGuard],
    children: [
      { path: '', pathMatch: 'full', redirectTo: 'dashboard' },
      { path: 'pendiente-aprobacion', component: PendingApprovalPageComponent },
      { path: 'acceso-restringido', component: AccessRestrictedPageComponent },
      { path: 'usuarios', component: UsuariosPageComponent, canActivate: [roleGuard], data: { access: { roles: ADMIN } } },
      { path: 'asistencia', component: AsistenciaPageComponent, canActivate: [roleGuard], data: { access: { roles: [AppRole.ADMIN, AppRole.MANAGER] } } },
      { path: 'mi-asistencia', component: MiAsistenciaPageComponent, canActivate: [roleGuard], data: { access: { roles: [AppRole.EMPLOYEE] } } },
      { path: 'dashboard', component: DashboardPageComponent },

      // Módulos originales (se mantienen para no romper referencias existentes)
      { path: 'clients', component: ClientsPageComponent },
      { path: 'inventory', redirectTo: 'inventario', pathMatch: 'full' },
      { path: 'tasks', component: TasksPageComponent },

      // ── Empleados ────────────────────────────────────────────────────────
      { path: 'empleados/nuevo', component: EmpleadoFormPageComponent, canActivate: [roleGuard], data: { access: { roles: ADMIN } } },
      { path: 'empleados/:id/editar', component: EmpleadoFormPageComponent, canActivate: [roleGuard], data: { access: { roles: ADMIN } } },
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
      {
        path: 'sedes/nueva',
        component: SedeFormPageComponent,
        canActivate: [roleGuard],
        data: { access: { roles: ADMIN } },
      },
      {
        path: 'sedes/:id/editar',
        component: SedeFormPageComponent,
        canActivate: [roleGuard],
        data: { access: { roles: ADMIN } },
      },
      {
        path: 'pos/catalogo',
        component: SedePosPageComponent,
        canActivate: [roleGuard],
        data: { access: { roles: POS_STAFF } },
      },
      {
        path: 'pos/configuracion',
        component: PosConfigPageComponent,
        canActivate: [roleGuard],
        data: { access: { roles: ADMIN_MANAGER } },
      },
      { path: 'sedes/:id', component: SedeDetailPageComponent },

      // ── Contratos ─────────────────────────────────────────────────────────
      { path: 'contratos', component: ContratosPageComponent },

      // ── Archivos ──────────────────────────────────────────────────────────
      { path: 'archivos', component: ArchivosPageComponent },

      // ── Nómina ────────────────────────────────────────────────────────────
      { path: 'nomina', component: NominaPageComponent },

      // ── POS: Catálogo maestro (admin) ─────────────────────────────────────
      {
        path: 'catalogo/nuevo',
        component: CatalogoFormPageComponent,
        canActivate: [roleGuard],
        data: { access: { roles: ADMIN } },
      },
      {
        path: 'catalogo/:id/editar',
        component: CatalogoFormPageComponent,
        canActivate: [roleGuard],
        data: { access: { roles: ADMIN } },
      },
      {
        path: 'catalogo',
        component: CatalogoPageComponent,
        canActivate: [roleGuard],
        data: { access: { roles: ADMIN } },
      },

      // ── Inventario HQ ───────────────────────────────────────────────────
      {
        path: 'inventario',
        component: InventarioPageComponent,
        canActivate: [roleGuard],
        data: { access: { roles: INVENTORY_READ } },
      },
      {
        path: 'inventario/entradas',
        component: InventarioEntradasPageComponent,
        canActivate: [roleGuard],
        data: { access: { roles: ADMIN_MANAGER } },
      },
      {
        path: 'inventario/ledger',
        component: InventarioLedgerPageComponent,
        canActivate: [roleGuard],
        data: { access: { roles: INVENTORY_READ } },
      },
      {
        path: 'inventario/transferencias',
        component: InventarioTransferPageComponent,
        canActivate: [roleGuard],
        data: { access: { roles: ADMIN_MANAGER } },
      },
      {
        path: 'inventario/mermas',
        component: InventarioMermasPageComponent,
        canActivate: [roleGuard],
        data: { access: { roles: ADMIN_MANAGER } },
      },
      {
        path: 'inventario/ajustes',
        component: InventarioAjustesPageComponent,
        canActivate: [roleGuard],
        data: { access: { roles: ADMIN } },
      },
      {
        path: 'inventario/conteos/nuevo',
        component: CountSessionCreatePageComponent,
        canActivate: [roleGuard],
        data: { access: { roles: ADMIN_MANAGER } },
      },
      {
        path: 'inventario/conteos/:id/revision',
        component: CountSessionDetailPageComponent,
        canActivate: [roleGuard],
        data: { access: { roles: ADMIN_MANAGER, review: true } },
      },
      {
        path: 'inventario/conteos/:id',
        component: CountSessionDetailPageComponent,
        canActivate: [roleGuard],
        data: { access: { roles: ADMIN_MANAGER } },
      },
      {
        path: 'inventario/conteos',
        component: CountSessionListPageComponent,
        canActivate: [roleGuard],
        data: { access: { roles: ADMIN_MANAGER } },
      },

      // ── POS: Dispositivos y operación ─────────────────────────────────────
      {
        path: 'pos/dispositivos',
        component: DispositivosPageComponent,
        canActivate: [roleGuard],
        data: { access: { roles: POS_STAFF } },
      },
      {
        path: 'pos/dispositivos/:id',
        component: DispositivoDetailPageComponent,
        canActivate: [roleGuard],
        data: { access: { roles: POS_STAFF } },
      },
      {
        path: 'pos/enrolamiento',
        component: EnrolamientoPageComponent,
        canActivate: [roleGuard],
        data: { access: { roles: POS_STAFF } },
      },
      {
        path: 'pos/operadores',
        component: OperadoresPageComponent,
        canActivate: [roleGuard],
        data: { access: { roles: POS_STAFF } },
      },
      {
        path: 'pos/ventas',
        component: VentasPageComponent,
        canActivate: [roleGuard],
        data: { access: { roles: POS_STAFF } },
      },
      {
        path: 'pos/turnos',
        component: TurnosPageComponent,
        canActivate: [roleGuard],
        data: { access: { roles: POS_STAFF } },
      },
      {
        path: 'pos/cortes',
        component: CortesRedirectComponent,
        canActivate: [roleGuard],
        data: { access: { roles: POS_STAFF } },
      },
      {
        path: 'pos/incidencias',
        component: IncidenciasPageComponent,
        canActivate: [roleGuard],
        data: { access: { roles: ADMIN } },
      },
    ],
  },
  { path: '', component: Home },
  { path: 'aviso-privacidad', component: AvisoPrivacidadPage },
  { path: 'terminos-servicio', component: TerminosServicioPage },
  { path: 'calidad-higienica', component: CalidadHigienicaPage },
];
