import { Component, computed, effect, inject, input, output, signal } from '@angular/core';
import { NavigationEnd, Router, RouterLink, RouterLinkActive } from '@angular/router';
import { filter } from 'rxjs';

import { SessionContextService } from '../../../core/auth/session-context.service';
import {
  ERP_HOME,
  ERP_PATH,
  OPS_HOME,
  OPS_PATH,
  type WorkspaceArea,
} from '../../../core/auth/workspace-area';
import { AppRole } from '../../../core/model/account/enums';
import { BRAND_LOGO_URL } from '../../../pages/home/brand';

export type WorkspaceNavAction = 'attendance-today' | 'attendance-search';

export interface WorkspaceNavItem {
  readonly label: string;
  readonly icon: string;
  readonly route?: string;
  readonly action?: WorkspaceNavAction;
  readonly roles: readonly AppRole[];
  readonly children?: readonly WorkspaceNavItem[];
}

export interface WorkspaceNavSection {
  readonly id: string;
  readonly label: string;
  readonly items: readonly WorkspaceNavItem[];
  readonly roles?: readonly AppRole[];
}

const ADMIN_MANAGER = [AppRole.ADMIN, AppRole.MANAGER];
const POS_OPERATION = [AppRole.ADMIN, AppRole.MANAGER, AppRole.POS_OPERATOR];
const INVENTORY_READ = [AppRole.ADMIN, AppRole.MANAGER, AppRole.POS_OPERATOR];
const ADMIN_ONLY = [AppRole.ADMIN];

export const ERP_NAVIGATION: readonly WorkspaceNavSection[] = [
  {
    id: 'overview',
    label: 'Resumen',
    items: [
      { label: 'Resumen', icon: 'dashboard', route: `${ERP_PATH}/dashboard`, roles: ADMIN_ONLY },
    ],
  },
  {
    id: 'crm',
    label: 'CRM',
    items: [
      { label: 'Oportunidades', icon: 'trending_up', route: `${ERP_PATH}/crm/oportunidades`, roles: ADMIN_ONLY },
      { label: 'Proyectos', icon: 'folder_open', route: `${ERP_PATH}/crm/proyectos`, roles: ADMIN_ONLY },
    ],
  },
  {
    id: 'people',
    label: 'Personas',
    items: [
      { label: 'Empleados', icon: 'badge', route: `${ERP_PATH}/empleados`, roles: ADMIN_ONLY },
      { label: 'Nómina', icon: 'payments', route: `${ERP_PATH}/nomina`, roles: ADMIN_ONLY },
      { label: 'Asistencia', icon: 'today', route: `${ERP_PATH}/asistencia`, roles: ADMIN_ONLY },
      { label: 'Contratos', icon: 'contract', route: `${ERP_PATH}/contratos`, roles: ADMIN_ONLY },
    ],
  },
  {
    id: 'work',
    label: 'Trabajo',
    items: [
      { label: 'Tareas', icon: 'task_alt', route: `${ERP_PATH}/tareas`, roles: ADMIN_ONLY },
      { label: 'Archivos', icon: 'folder', route: `${ERP_PATH}/archivos`, roles: ADMIN_ONLY },
    ],
  },
  {
    id: 'administration',
    label: 'Administración',
    items: [
      { label: 'Usuarios', icon: 'manage_accounts', route: `${ERP_PATH}/usuarios`, roles: ADMIN_ONLY },
    ],
  },
];

export const OPS_NAVIGATION: readonly WorkspaceNavSection[] = [
  {
    id: 'overview',
    label: 'Resumen',
    items: [
      { label: 'Resumen', icon: 'dashboard', route: `${OPS_PATH}/dashboard`, roles: POS_OPERATION },
    ],
  },
  {
    id: 'sites',
    label: 'Sedes',
    items: [
      { label: 'Sedes', icon: 'location_on', route: `${OPS_PATH}/sedes`, roles: ADMIN_MANAGER },
    ],
  },
  {
    id: 'pos',
    label: 'Punto de venta',
    roles: POS_OPERATION,
    items: [
      { label: 'Catálogo por sede', icon: 'storefront', route: `${OPS_PATH}/pos/catalogo`, roles: ADMIN_MANAGER },
      { label: 'Ventas', icon: 'receipt_long', route: `${OPS_PATH}/pos/ventas`, roles: POS_OPERATION },
      { label: 'Turnos', icon: 'point_of_sale', route: `${OPS_PATH}/pos/turnos`, roles: ADMIN_MANAGER },
      {
        label: 'Configuración POS',
        icon: 'settings',
        roles: ADMIN_MANAGER,
        children: [
          { label: 'Políticas de caja', icon: 'tune', route: `${OPS_PATH}/pos/configuracion`, roles: ADMIN_MANAGER },
          { label: 'Dispositivos', icon: 'tablet_android', route: `${OPS_PATH}/pos/dispositivos`, roles: ADMIN_MANAGER },
          { label: 'Enrolamiento', icon: 'qr_code_2', route: `${OPS_PATH}/pos/enrolamiento`, roles: ADMIN_MANAGER },
          { label: 'Operadores', icon: 'group', route: `${OPS_PATH}/pos/operadores`, roles: ADMIN_MANAGER },
          { label: 'Incidencias', icon: 'sync_problem', route: `${OPS_PATH}/pos/incidencias`, roles: ADMIN_ONLY },
        ],
      },
    ],
  },
  {
    id: 'inventory',
    label: 'Inventario',
    roles: INVENTORY_READ,
    items: [
      { label: 'Existencias', icon: 'warehouse', route: `${OPS_PATH}/inventario`, roles: INVENTORY_READ },
      { label: 'Libro de movimientos', icon: 'receipt_long', route: `${OPS_PATH}/inventario/ledger`, roles: INVENTORY_READ },
      { label: 'Entradas (IN)', icon: 'add_shopping_cart', route: `${OPS_PATH}/inventario/entradas`, roles: ADMIN_MANAGER },
      { label: 'Transferencias', icon: 'swap_horiz', route: `${OPS_PATH}/inventario/transferencias`, roles: ADMIN_MANAGER },
      { label: 'Mermas (OUT)', icon: 'delete_sweep', route: `${OPS_PATH}/inventario/mermas`, roles: ADMIN_MANAGER },
      { label: 'Ajustes', icon: 'tune', route: `${OPS_PATH}/inventario/ajustes`, roles: ADMIN_ONLY },
      { label: 'Conteos físicos', icon: 'fact_check', route: `${OPS_PATH}/inventario/conteos`, roles: ADMIN_MANAGER },
      { label: 'Artículos maestros', icon: 'inventory_2', route: `${OPS_PATH}/catalogo`, roles: ADMIN_ONLY },
    ],
  },
];

/** @deprecated Use ERP_NAVIGATION / OPS_NAVIGATION. */
export const WORKSPACE_NAVIGATION: readonly WorkspaceNavSection[] = [
  ...ERP_NAVIGATION,
  ...OPS_NAVIGATION,
];

export const WORKSPACE_ACTIONS: readonly WorkspaceNavItem[] = [];

@Component({
  selector: 'app-workspace-sidebar',

  imports: [RouterLink, RouterLinkActive],
  templateUrl: './workspace-sidebar.html',
})
export class WorkspaceSidebarComponent {
  private readonly session = inject(SessionContextService);
  private readonly router = inject(Router);

  readonly logoUrl = BRAND_LOGO_URL;
  readonly abierta = input(false);
  readonly area = input<WorkspaceArea>('ops');
  readonly catalog = computed(() => (this.area() === 'erp' ? ERP_NAVIGATION : OPS_NAVIGATION));
  readonly navigation = computed(() => this.catalog()
    .map((section) => ({ ...section, items: this.visibleItems(section.items) }))
    .filter((section) => section.items.length > 0));
  readonly actions = computed(() => WORKSPACE_ACTIONS.filter((item) => this.canSee(item)));
  readonly currentUrl = signal(this.router.url);
  readonly expandedSections = signal<ReadonlySet<string>>(new Set());
  readonly expandedItems = signal<ReadonlySet<string>>(new Set());
  readonly isAdmin = this.session.isAdmin;
  readonly opsHome = OPS_HOME;
  readonly erpHome = ERP_HOME;

  readonly cerrar = output<void>();
  readonly abrirAsistenciaHoy = output<void>();
  readonly abrirAsistenciaBusqueda = output<void>();
  readonly abrirPerfil = output<void>();

  /** Base styles; active state is layered via {@link RouterLinkActive}. */
  readonly navLinkInactive =
    'flex items-center gap-3 rounded-lg px-4 py-3 text-sm font-bold tracking-tight text-on-surface-variant transition-colors hover:bg-surface-container';

  constructor() {
    this.router.events.pipe(filter((event) => event instanceof NavigationEnd)).subscribe((event) => {
      this.currentUrl.set(event.urlAfterRedirects);
    });
    effect(() => {
      const active = this.navigation().find((section) => this.sectionIsActive(section));
      if (active) {
        this.expandedSections.update((expanded) => {
          const next = new Set(expanded);
          next.add(active.id);
          return next;
        });
        active.items.filter((item) => item.children && this.itemIsActive(item)).forEach((item) => {
          this.expandedItems.update((expanded) => new Set(expanded).add(this.itemKey(active.id, item)));
        });
      }
    });
  }

  toggleSection(sectionId: string): void {
    this.expandedSections.update((expanded) => {
      const next = new Set(expanded);
      next.has(sectionId) ? next.delete(sectionId) : next.add(sectionId);
      return next;
    });
  }

  isExpanded(sectionId: string): boolean {
    return this.expandedSections().has(sectionId);
  }

  toggleItem(sectionId: string, item: WorkspaceNavItem): void {
    const key = this.itemKey(sectionId, item);
    this.expandedItems.update((expanded) => {
      const next = new Set(expanded);
      next.has(key) ? next.delete(key) : next.add(key);
      return next;
    });
  }

  isItemExpanded(sectionId: string, item: WorkspaceNavItem): boolean {
    return this.expandedItems().has(this.itemKey(sectionId, item));
  }

  sectionIsActive(section: WorkspaceNavSection): boolean {
    return section.items.some((item) => this.itemIsActive(item));
  }

  itemIsActive(item: WorkspaceNavItem): boolean {
    return (item.route !== undefined && this.currentUrl().startsWith(item.route))
      || (item.children?.some((child) => this.itemIsActive(child)) ?? false);
  }

  activate(item: WorkspaceNavItem): void {
    if (item.action === 'attendance-today') this.abrirAsistenciaHoy.emit();
    if (item.action === 'attendance-search') this.abrirAsistenciaBusqueda.emit();
  }

  private visibleItems(items: readonly WorkspaceNavItem[]): WorkspaceNavItem[] {
    return items
      .filter((item) => this.canSee(item))
      .map((item) => ({ ...item, children: item.children ? this.visibleItems(item.children) : undefined }))
      .filter((item) => !item.children || item.children.length > 0 || item.route || item.action);
  }

  private canSee(item: WorkspaceNavItem): boolean {
    return item.roles.some((role) => this.session.roles().includes(role));
  }

  private itemKey(sectionId: string, item: WorkspaceNavItem): string {
    return `${sectionId}:${item.label}`;
  }
}
