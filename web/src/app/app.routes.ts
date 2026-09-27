import { Routes } from '@angular/router';

import { guestAuthGuard } from './core/auth/guest-auth.guard';
import { workspaceAuthGuard } from './core/auth/workspace-auth.guard';
import { roleGuard } from './core/auth/role.guard';
import { dirtyFormGuard } from './core/auth/dirty-form.guard';
import { erpAreaGuard, opsAreaGuard, workspaceHubGuard } from './core/auth/workspace-area.guard';
import { AppRole } from './core/model/account/enums';

const ADMIN = [AppRole.ADMIN];
const OPS = [AppRole.ADMIN, AppRole.DIRECTOR, AppRole.MANAGER, AppRole.EMPLOYEE];
const POS_FINANCIAL = [AppRole.ADMIN, AppRole.DIRECTOR];

const erpChildren: Routes = [
  { path: '', pathMatch: 'full', redirectTo: 'dashboard' },
  {
    path: 'dashboard',
    loadComponent: () =>
      import('./pages/app/erp/erp-dashboard-page').then((m) => m.ErpDashboardPageComponent),
  },
  {
    path: 'usuarios',
    loadComponent: () =>
      import('./pages/app/usuarios/usuarios-page').then((m) => m.UsuariosPageComponent),
    canActivate: [roleGuard],
    data: { access: { roles: ADMIN } },
  },
  {
    path: 'asistencia',
    loadComponent: () =>
      import('./pages/app/asistencia/asistencia-page').then((m) => m.AsistenciaPageComponent),
    canActivate: [roleGuard],
    data: { access: { roles: ADMIN } },
  },
  {
    path: 'clients',
    loadComponent: () => import('./pages/clients/clients-page').then((m) => m.ClientsPageComponent),
  },
  {
    path: 'tasks',
    loadComponent: () => import('./pages/app/tasks/tasks-page').then((m) => m.TasksPageComponent),
  },
  {
    path: 'empleados/nuevo',
    loadComponent: () =>
      import('./pages/app/empleados/empleado-form-page/empleado-form-page').then(
        (m) => m.EmpleadoFormPageComponent,
      ),
    canDeactivate: [dirtyFormGuard],
    canActivate: [roleGuard],
    data: { access: { roles: ADMIN } },
  },
  {
    path: 'empleados/:id/editar',
    loadComponent: () =>
      import('./pages/app/empleados/empleado-form-page/empleado-form-page').then(
        (m) => m.EmpleadoFormPageComponent,
      ),
    canDeactivate: [dirtyFormGuard],
    canActivate: [roleGuard],
    data: { access: { roles: ADMIN } },
  },
  {
    path: 'empleados/:id',
    loadComponent: () =>
      import('./pages/app/empleados/empleado-detail/empleado-detail-page').then(
        (m) => m.EmpleadoDetailPageComponent,
      ),
  },
  {
    path: 'empleados',
    loadComponent: () =>
      import('./pages/app/empleados/empleados-page').then((m) => m.EmpleadosPageComponent),
  },
  {
    path: 'crm/oportunidades/nueva',
    loadComponent: () =>
      import('./pages/app/crm/oportunidades/oportunidad-form-page/oportunidad-form-page').then(
        (m) => m.OportunidadFormPageComponent,
      ),
    canDeactivate: [dirtyFormGuard],
  },
  {
    path: 'crm/oportunidades/:id/editar',
    loadComponent: () =>
      import('./pages/app/crm/oportunidades/oportunidad-form-page/oportunidad-form-page').then(
        (m) => m.OportunidadFormPageComponent,
      ),
    canDeactivate: [dirtyFormGuard],
  },
  {
    path: 'crm/oportunidades/:id',
    loadComponent: () =>
      import('./pages/app/crm/oportunidades/oportunidad-detail/oportunidad-detail-page').then(
        (m) => m.OportunidadDetailPageComponent,
      ),
  },
  {
    path: 'crm/oportunidades',
    loadComponent: () =>
      import('./pages/app/crm/oportunidades/oportunidades-page').then(
        (m) => m.OportunidadesPageComponent,
      ),
  },
  {
    path: 'crm/proyectos/nuevo',
    loadComponent: () =>
      import('./pages/app/crm/proyectos/proyecto-form-page/proyecto-form-page').then(
        (m) => m.ProyectoFormPageComponent,
      ),
    canDeactivate: [dirtyFormGuard],
  },
  {
    path: 'crm/proyectos/:id/editar',
    loadComponent: () =>
      import('./pages/app/crm/proyectos/proyecto-form-page/proyecto-form-page').then(
        (m) => m.ProyectoFormPageComponent,
      ),
    canDeactivate: [dirtyFormGuard],
  },
  {
    path: 'crm/proyectos/:id',
    loadComponent: () =>
      import('./pages/app/crm/proyectos/proyecto-detail/proyecto-detail-page').then(
        (m) => m.ProyectoDetailPageComponent,
      ),
  },
  {
    path: 'crm/proyectos',
    loadComponent: () =>
      import('./pages/app/crm/proyectos/proyectos-page').then((m) => m.ProyectosPageComponent),
  },
  {
    path: 'tareas/nueva',
    loadComponent: () =>
      import('./pages/app/tareas/tarea-form/tarea-form-page').then((m) => m.TareaFormPageComponent),
    canDeactivate: [dirtyFormGuard],
  },
  {
    path: 'tareas/:id',
    loadComponent: () =>
      import('./pages/app/tareas/tarea-detail/tarea-detail-page').then(
        (m) => m.TareaDetailPageComponent,
      ),
  },
  {
    path: 'tareas',
    loadComponent: () => import('./pages/app/tareas/tareas-page').then((m) => m.TareasPageComponent),
  },
  {
    path: 'contratos',
    loadComponent: () =>
      import('./pages/app/contratos/contratos-page').then((m) => m.ContratosPageComponent),
  },
  {
    path: 'archivos',
    loadComponent: () =>
      import('./pages/app/archivos/archivos-page').then((m) => m.ArchivosPageComponent),
  },
  {
    path: 'nomina',
    loadComponent: () => import('./pages/app/nomina/nomina-page').then((m) => m.NominaPageComponent),
  },
];

const opsChildren: Routes = [
  { path: '', pathMatch: 'full', redirectTo: 'dashboard' },
  {
    path: 'dashboard',
    loadComponent: () =>
      import('./pages/dashboard/dashboard-page').then((m) => m.DashboardPageComponent),
  },
  {
    path: 'sedes',
    loadComponent: () => import('./pages/app/sedes/sedes-page').then((m) => m.SedesPageComponent),
  },
  {
    path: 'proveedores/nuevo',
    loadComponent: () =>
      import('./pages/app/proveedores/proveedor-form-page/proveedor-form-page').then(
        (m) => m.ProveedorFormPageComponent,
      ),
    canDeactivate: [dirtyFormGuard],
    canActivate: [roleGuard],
    data: { access: { roles: OPS } },
  },
  {
    path: 'proveedores/:id/editar',
    loadComponent: () =>
      import('./pages/app/proveedores/proveedor-form-page/proveedor-form-page').then(
        (m) => m.ProveedorFormPageComponent,
      ),
    canDeactivate: [dirtyFormGuard],
    canActivate: [roleGuard],
    data: { access: { roles: OPS } },
  },
  {
    path: 'proveedores',
    loadComponent: () =>
      import('./pages/app/proveedores/proveedores-page').then((m) => m.ProveedoresPageComponent),
    canActivate: [roleGuard],
    data: { access: { roles: OPS } },
  },
  {
    path: 'sedes/nueva',
    loadComponent: () =>
      import('./pages/app/sedes/sede-form-page/sede-form-page').then((m) => m.SedeFormPageComponent),
    canDeactivate: [dirtyFormGuard],
    canActivate: [roleGuard],
    data: { access: { roles: ADMIN } },
  },
  {
    path: 'sedes/:id/editar',
    loadComponent: () =>
      import('./pages/app/sedes/sede-form-page/sede-form-page').then((m) => m.SedeFormPageComponent),
    canDeactivate: [dirtyFormGuard],
    canActivate: [roleGuard],
    data: { access: { roles: ADMIN } },
  },
  {
    path: 'sedes/:id',
    loadComponent: () =>
      import('./pages/app/sedes/sede-detail/sede-detail-page').then((m) => m.SedeDetailPageComponent),
  },
  {
    path: 'pos/catalogo',
    loadComponent: () =>
      import('./pages/app/sedes/sede-pos-page/sede-pos-page').then((m) => m.SedePosPageComponent),
    canActivate: [roleGuard],
    data: { access: { roles: OPS } },
  },
  {
    path: 'pos/sedes',
    redirectTo: 'pos/configuracion',
    pathMatch: 'full',
  },
  {
    path: 'pos/configuracion',
    loadComponent: () =>
      import('./pages/app/pos/configuracion/pos-config-page').then((m) => m.PosConfigPageComponent),
    canActivate: [roleGuard],
    data: { access: { roles: OPS } },
  },
  { path: 'catalogo/nuevo', redirectTo: 'catalogo' },
  {
    path: 'catalogo/nuevo/pos',
    loadComponent: () =>
      import('./pages/app/catalogo/catalogo-form-page/catalogo-form-page').then(
        (m) => m.CatalogoFormPageComponent,
      ),
    canDeactivate: [dirtyFormGuard],
    canActivate: [roleGuard],
    data: { access: { roles: OPS }, itemKind: 'pos' },
  },
  {
    path: 'catalogo/nuevo/almacen',
    loadComponent: () =>
      import('./pages/app/catalogo/catalogo-form-page/catalogo-form-page').then(
        (m) => m.CatalogoFormPageComponent,
      ),
    canDeactivate: [dirtyFormGuard],
    canActivate: [roleGuard],
    data: { access: { roles: OPS }, itemKind: 'warehouse' },
  },
  {
    path: 'catalogo/:id/editar',
    loadComponent: () =>
      import('./pages/app/catalogo/catalogo-form-page/catalogo-form-page').then(
        (m) => m.CatalogoFormPageComponent,
      ),
    canDeactivate: [dirtyFormGuard],
    canActivate: [roleGuard],
    data: { access: { roles: OPS } },
  },
  {
    path: 'catalogo',
    loadComponent: () =>
      import('./pages/app/catalogo/catalogo-page').then((m) => m.CatalogoPageComponent),
    canActivate: [roleGuard],
    data: { access: { roles: OPS } },
  },
  {
    path: 'inventario',
    loadComponent: () =>
      import('./pages/app/inventario/inventario-page').then((m) => m.InventarioPageComponent),
    canActivate: [roleGuard],
    data: { access: { roles: OPS } },
  },
  { path: 'inventario/entradas', redirectTo: 'inventario/ledger' },
  {
    path: 'inventario/ledger',
    loadComponent: () =>
      import('./pages/app/inventario/ledger/inventario-ledger-page').then(
        (m) => m.InventarioLedgerPageComponent,
      ),
    canActivate: [roleGuard],
    data: { access: { roles: OPS } },
  },
  {
    path: 'inventario/transferencias',
    loadComponent: () =>
      import('./pages/app/inventario/transferencias/inventario-transfer-page').then(
        (m) => m.InventarioTransferPageComponent,
      ),
    canActivate: [roleGuard],
    data: { access: { roles: OPS } },
  },
  { path: 'inventario/mermas', redirectTo: 'inventario/ledger' },
  { path: 'inventario/ajustes', redirectTo: 'inventario/ledger' },
  {
    path: 'inventario/conteos/nuevo',
    loadComponent: () =>
      import('./pages/app/inventario/counts/count-session-create-page').then(
        (m) => m.CountSessionCreatePageComponent,
      ),
    canActivate: [roleGuard],
    data: { access: { roles: OPS } },
  },
  {
    path: 'inventario/conteos/:id/revision',
    loadComponent: () =>
      import('./pages/app/inventario/counts/count-session-detail-page').then(
        (m) => m.CountSessionDetailPageComponent,
      ),
    canActivate: [roleGuard],
    data: { access: { roles: OPS, review: true } },
  },
  {
    path: 'inventario/conteos/:id',
    loadComponent: () =>
      import('./pages/app/inventario/counts/count-session-detail-page').then(
        (m) => m.CountSessionDetailPageComponent,
      ),
    canActivate: [roleGuard],
    data: { access: { roles: OPS } },
  },
  {
    path: 'inventario/conteos',
    loadComponent: () =>
      import('./pages/app/inventario/counts/count-session-list-page').then(
        (m) => m.CountSessionListPageComponent,
      ),
    canActivate: [roleGuard],
    data: { access: { roles: OPS } },
  },
  {
    path: 'pos/dispositivos',
    loadComponent: () =>
      import('./pages/app/pos/dispositivos/dispositivos-page').then(
        (m) => m.DispositivosPageComponent,
      ),
    canActivate: [roleGuard],
    data: { access: { roles: OPS } },
  },
  {
    path: 'pos/dispositivos/:id',
    loadComponent: () =>
      import('./pages/app/pos/dispositivos/dispositivo-detail-page').then(
        (m) => m.DispositivoDetailPageComponent,
      ),
    canActivate: [roleGuard],
    data: { access: { roles: OPS } },
  },
  {
    path: 'pos/enrolamiento',
    loadComponent: () =>
      import('./pages/app/pos/enrolamiento/enrolamiento-page').then(
        (m) => m.EnrolamientoPageComponent,
      ),
    canActivate: [roleGuard],
    data: { access: { roles: OPS } },
  },
  {
    path: 'pos/operadores',
    loadComponent: () =>
      import('./pages/app/pos/operadores/operadores-page').then((m) => m.OperadoresPageComponent),
    canActivate: [roleGuard],
    data: { access: { roles: OPS } },
  },
  {
    path: 'pos/ventas',
    loadComponent: () =>
      import('./pages/app/pos/ventas/ventas-page').then((m) => m.VentasPageComponent),
    canActivate: [roleGuard],
    data: { access: { roles: POS_FINANCIAL } },
  },
  {
    path: 'pos/turnos',
    loadComponent: () =>
      import('./pages/app/pos/turnos/turnos-page').then((m) => m.TurnosPageComponent),
    canActivate: [roleGuard],
    data: { access: { roles: OPS } },
  },
  {
    path: 'pos/cortes',
    loadComponent: () =>
      import('./pages/app/pos/cortes/cortes-redirect').then((m) => m.CortesRedirectComponent),
    canActivate: [roleGuard],
    data: { access: { roles: POS_FINANCIAL } },
  },
];

const legacyAppRedirects: Routes = [
  { path: 'dashboard', redirectTo: 'ops/dashboard' },
  { path: 'inventory', redirectTo: 'ops/inventario' },
  { path: 'inventario', redirectTo: 'ops/inventario' },
  { path: 'catalogo', redirectTo: 'ops/catalogo' },
  { path: 'sedes', redirectTo: 'ops/sedes' },
  { path: 'proveedores', redirectTo: 'ops/proveedores' },
  { path: 'pos', redirectTo: 'ops/pos' },
  { path: 'crm', redirectTo: 'erp/crm' },
  { path: 'empleados', redirectTo: 'erp/empleados' },
  { path: 'tareas', redirectTo: 'erp/tareas' },
  { path: 'nomina', redirectTo: 'erp/nomina' },
  { path: 'contratos', redirectTo: 'erp/contratos' },
  { path: 'archivos', redirectTo: 'erp/archivos' },
  { path: 'usuarios', redirectTo: 'erp/usuarios' },
  { path: 'asistencia', redirectTo: 'erp/asistencia' },
  { path: 'clients', redirectTo: 'erp/clients' },
  { path: 'tasks', redirectTo: 'erp/tasks' },
];

export const routes: Routes = [
  {
    path: 'auth/login',
    loadComponent: () => import('./pages/auth/login/login').then((m) => m.Login),
    canActivate: [guestAuthGuard],
  },
  {
    path: 'auth/register',
    loadComponent: () => import('./pages/auth/register/register').then((m) => m.Register),
    canActivate: [guestAuthGuard],
  },
  {
    path: 'auth/pendiente-aprobacion',
    loadComponent: () =>
      import('./pages/auth/pending-approval/pending-approval').then((m) => m.PendingApprovalPage),
    canActivate: [guestAuthGuard],
  },
  {
    path: 'app',
    canActivate: [workspaceAuthGuard],
    children: [
      {
        path: '',
        pathMatch: 'full',
        loadComponent: () =>
          import('./pages/app/workspace-hub/workspace-hub-page').then(
            (m) => m.WorkspaceHubPageComponent,
          ),
        canActivate: [workspaceHubGuard],
      },
      {
        path: 'pendiente-aprobacion',
        loadComponent: () =>
          import('./pages/app/pending-approval/pending-approval-page').then(
            (m) => m.PendingApprovalPageComponent,
          ),
      },
      {
        path: 'acceso-restringido',
        loadComponent: () =>
          import('./pages/app/access-restricted/access-restricted-page').then(
            (m) => m.AccessRestrictedPageComponent,
          ),
      },
      {
        path: 'mi-asistencia',
        loadComponent: () =>
          import('./pages/app/mi-asistencia/mi-asistencia-page').then(
            (m) => m.MiAsistenciaPageComponent,
          ),
        canActivate: [roleGuard],
        data: { access: { roles: [AppRole.EMPLOYEE] } },
      },
      {
        path: 'erp',
        loadComponent: () =>
          import('./shared/workspace/workspace-shell/workspace-shell').then(
            (m) => m.WorkspaceShellComponent,
          ),
        canActivate: [erpAreaGuard],
        data: { workspaceArea: 'erp' },
        children: erpChildren,
      },
      {
        path: 'ops',
        loadComponent: () =>
          import('./shared/workspace/workspace-shell/workspace-shell').then(
            (m) => m.WorkspaceShellComponent,
          ),
        canActivate: [opsAreaGuard],
        data: { workspaceArea: 'ops' },
        children: opsChildren,
      },
      ...legacyAppRedirects,
    ],
  },
  {
    path: '',
    loadComponent: () => import('./pages/home/home/home').then((m) => m.Home),
  },
  {
    path: 'aviso-privacidad',
    loadComponent: () =>
      import('./pages/home/aviso-privacidad-page/aviso-privacidad-page').then(
        (m) => m.AvisoPrivacidadPage,
      ),
  },
  {
    path: 'terminos-servicio',
    loadComponent: () =>
      import('./pages/home/terminos-servicio-page/terminos-servicio-page').then(
        (m) => m.TerminosServicioPage,
      ),
  },
  {
    path: 'calidad-higienica',
    loadComponent: () =>
      import('./pages/home/calidad-higienica-page/calidad-higienica-page').then(
        (m) => m.CalidadHigienicaPage,
      ),
  },
];
