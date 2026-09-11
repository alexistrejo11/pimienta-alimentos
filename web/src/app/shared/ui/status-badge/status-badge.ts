import { Component, input } from '@angular/core';

import {
  employeeStatusLabel,
  milestoneStatusLabel,
  opportunityStatusLabel,
  projectStatusLabel,
  taskStatusLabel,
} from '../../../core/i18n/enum-labels';

/**
 * Tipos de entidad que tienen estado (status).
 * Cada uno tiene su propia paleta de colores en la plantilla.
 */
export type StatusBadgeKind = 'employee' | 'task' | 'opportunity' | 'project' | 'milestone';

/**
 * Componente reutilizable que muestra un estado como una pastilla de color.
 *
 * Uso:
 *   <app-status-badge status="ACTIVE" kind="employee" />
 *   <app-status-badge status="IN_PROGRESS" kind="task" />
 */
@Component({
  selector: 'app-status-badge',

  templateUrl: './status-badge.html',
})
export class StatusBadgeComponent {
  /** Valor del estado tal como llega del backend (e.g. "ACTIVE", "IN_PROGRESS"). */
  readonly status = input.required<string>();

  /** Dominio del estado; determina qué mapa de colores y etiquetas se usa. */
  readonly kind = input.required<StatusBadgeKind>();

  /** Devuelve la etiqueta en español según el dominio y el valor del estado. */
  get label(): string {
    return labelFor(this.kind(), this.status());
  }

  /** Devuelve las clases Tailwind de color según el dominio y el estado. */
  get colorClass(): string {
    return colorFor(this.kind(), this.status());
  }
}

function labelFor(kind: StatusBadgeKind, status: string): string {
  switch (kind) {
    case 'employee':
      return employeeStatusLabel(status);
    case 'task':
      return taskStatusLabel(status);
    case 'opportunity':
      return opportunityStatusLabel(status);
    case 'project':
      return projectStatusLabel(status);
    case 'milestone':
      return milestoneStatusLabel(status);
  }
}

function colorFor(kind: StatusBadgeKind, status: string): string {
  switch (kind) {
    case 'employee':
      return EMPLOYEE_COLORS[status] ?? DEFAULT_COLOR;
    case 'task':
      return TASK_COLORS[status] ?? DEFAULT_COLOR;
    case 'opportunity':
      return OPPORTUNITY_COLORS[status] ?? DEFAULT_COLOR;
    case 'project':
      return PROJECT_COLORS[status] ?? DEFAULT_COLOR;
    case 'milestone':
      return MILESTONE_COLORS[status] ?? DEFAULT_COLOR;
  }
}

const DEFAULT_COLOR = 'ui-badge-neutral';

const EMPLOYEE_COLORS: Record<string, string> = {
  DRAFT: 'ui-badge-neutral',
  PENDING_CONTRACT: 'ui-badge-warning',
  ACTIVE: 'ui-badge-success',
  SICK: 'ui-badge-warning',
  ON_VACATION: 'bg-surface-container-high text-on-surface',
  ON_LEAVE: 'ui-badge-info',
  TERMINATED: 'ui-badge-danger',
  FIRED: 'ui-badge-danger',
  RESIGNED: 'bg-surface-container-high text-on-surface-variant',
};

const TASK_COLORS: Record<string, string> = {
  PENDING: 'ui-badge-neutral',
  IN_PROGRESS: 'bg-secondary/20 text-on-surface',
  COMPLETED: 'ui-badge-success',
  CANCELLED: 'bg-surface-container-high text-on-surface-variant',
  DELAYED: 'ui-badge-warning',
  ON_HOLD: 'ui-badge-warning',
  FAILED: 'ui-badge-danger',
};

const OPPORTUNITY_COLORS: Record<string, string> = {
  NEW: 'bg-surface-container-high text-on-surface',
  DISCOVERY: 'ui-badge-info',
  PROPOSAL: 'ui-badge-warning',
  NEGOTIATION: 'ui-badge-warning',
  WON: 'ui-badge-success',
  LOST: 'ui-badge-danger',
  ABANDONED: 'bg-surface-container-high text-on-surface-variant',
};

const PROJECT_COLORS: Record<string, string> = {
  PLANNING: 'bg-surface-container-high text-on-surface',
  ACTIVE: 'ui-badge-success',
  ON_HOLD: 'ui-badge-warning',
  COMPLETED: 'ui-badge-success',
  CANCELLED: 'ui-badge-danger',
  ARCHIVED: 'bg-surface-container-high text-on-surface-variant',
};

const MILESTONE_COLORS: Record<string, string> = {
  PENDING: 'ui-badge-neutral',
  IN_PROGRESS: 'bg-secondary/20 text-on-surface',
  COMPLETED: 'ui-badge-success',
  DELAYED: 'ui-badge-warning',
  CANCELLED: 'bg-surface-container-high text-on-surface-variant',
};
