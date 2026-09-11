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

const DEFAULT_COLOR = 'bg-surface-container text-on-surface';

const EMPLOYEE_COLORS: Record<string, string> = {
  DRAFT: 'bg-surface-container text-on-surface-variant',
  PENDING_CONTRACT: 'bg-amber-100 text-amber-800',
  ACTIVE: 'bg-emerald-100 text-emerald-800',
  SICK: 'bg-amber-100 text-amber-800',
  ON_VACATION: 'bg-surface-container-high text-on-surface',
  ON_LEAVE: 'bg-violet-100 text-violet-800',
  TERMINATED: 'bg-red-100 text-red-800',
  FIRED: 'bg-red-100 text-red-800',
  RESIGNED: 'bg-surface-container-high text-on-surface-variant',
};

const TASK_COLORS: Record<string, string> = {
  PENDING: 'bg-surface-container text-on-surface-variant',
  IN_PROGRESS: 'bg-secondary/20 text-on-surface',
  COMPLETED: 'bg-emerald-100 text-emerald-800',
  CANCELLED: 'bg-surface-container-high text-on-surface-variant',
  DELAYED: 'bg-orange-100 text-orange-800',
  ON_HOLD: 'bg-amber-100 text-amber-800',
  FAILED: 'bg-red-100 text-red-800',
};

const OPPORTUNITY_COLORS: Record<string, string> = {
  NEW: 'bg-surface-container-high text-on-surface',
  DISCOVERY: 'bg-purple-100 text-purple-800',
  PROPOSAL: 'bg-yellow-100 text-yellow-800',
  NEGOTIATION: 'bg-orange-100 text-orange-800',
  WON: 'bg-green-100 text-green-800',
  LOST: 'bg-red-100 text-red-800',
  ABANDONED: 'bg-surface-container-high text-on-surface-variant',
};

const PROJECT_COLORS: Record<string, string> = {
  PLANNING: 'bg-surface-container-high text-on-surface',
  ACTIVE: 'bg-green-100 text-green-800',
  ON_HOLD: 'bg-amber-100 text-amber-800',
  COMPLETED: 'bg-emerald-100 text-emerald-800',
  CANCELLED: 'bg-red-100 text-red-800',
  ARCHIVED: 'bg-surface-container-high text-on-surface-variant',
};

const MILESTONE_COLORS: Record<string, string> = {
  PENDING: 'bg-surface-container text-on-surface-variant',
  IN_PROGRESS: 'bg-secondary/20 text-on-surface',
  COMPLETED: 'bg-green-100 text-green-800',
  DELAYED: 'bg-orange-100 text-orange-800',
  CANCELLED: 'bg-surface-container-high text-on-surface-variant',
};
