/**
 * Spanish UI labels for API enum wire values (English codes stay on the wire).
 */

export function accountStatusLabel(status: string): string {
  return ACCOUNT_STATUS_LABELS[status] ?? status;
}

export function genderLabel(gender: string): string {
  return GENDER_LABELS[gender] ?? gender;
}

export function roleLabel(role: string): string {
  const normalized = role.startsWith('ROLE_') ? role.slice(5) : role;
  return ROLE_LABELS[normalized] ?? role;
}

export function employeeStatusLabel(status: string): string {
  return EMPLOYEE_STATUS_LABELS[status] ?? status;
}

export function attendanceStatusLabel(status: string): string {
  return ATTENDANCE_STATUS_LABELS[status] ?? status;
}

export function payrollRecordStatusLabel(status: string): string {
  return PAYROLL_RECORD_STATUS_LABELS[status] ?? status;
}

export function payrollFrequencyLabel(frequency: string): string {
  return PAYROLL_FREQUENCY_LABELS[frequency] ?? frequency;
}

export function itemStatusLabel(status: string): string {
  return ITEM_STATUS_LABELS[status] ?? status;
}

export function posRoleLabel(role: string): string {
  return POS_ROLE_LABELS[role] ?? role;
}

export function taskStatusLabel(status: string): string {
  return TASK_STATUS_LABELS[status] ?? status;
}

export function opportunityStatusLabel(status: string): string {
  return OPPORTUNITY_STATUS_LABELS[status] ?? status;
}

export function projectStatusLabel(status: string): string {
  return PROJECT_STATUS_LABELS[status] ?? status;
}

export function milestoneStatusLabel(status: string): string {
  return MILESTONE_STATUS_LABELS[status] ?? status;
}

const ACCOUNT_STATUS_LABELS: Record<string, string> = {
  PENDING_APPROVAL: 'Pendiente de aprobación',
  ACTIVE: 'Activa',
  BANNED: 'Bloqueada',
};

const GENDER_LABELS: Record<string, string> = {
  MALE: 'Masculino',
  FEMALE: 'Femenino',
  NON_BINARY: 'No binario',
  OTHER: 'Otro',
  PREFER_NOT_TO_SAY: 'Prefiero no decir',
};

const ROLE_LABELS: Record<string, string> = {
  USER: 'Usuario',
  SUPPORT: 'Soporte',
  MANAGER: 'Gerente',
  ADMIN: 'Administrador',
};

const EMPLOYEE_STATUS_LABELS: Record<string, string> = {
  DRAFT: 'Borrador',
  PENDING_CONTRACT: 'Pendiente de contrato',
  ACTIVE: 'Activo',
  SICK: 'Incapacitado',
  ON_VACATION: 'Vacaciones',
  ON_LEAVE: 'Permiso',
  TERMINATED: 'Baja',
  FIRED: 'Despedido',
  RESIGNED: 'Renunció',
};

const ATTENDANCE_STATUS_LABELS: Record<string, string> = {
  UNDEFINED: 'Indefinido',
  CHECKED_IN: 'En turno',
  CHECKED_OUT: 'Salió',
  AUTO_CLOSED_EXCEEDED_MAX_SHIFT_HOURS: 'Cierre auto (horas)',
  AUTO_CLOSED_ASSUMED_CONTRACT_DAY: 'Cierre auto (jornada)',
};

const PAYROLL_RECORD_STATUS_LABELS: Record<string, string> = {
  PENDING: 'Pendiente',
  PAID: 'Pagado',
  PARTIAL: 'Parcial',
  DEFERRED: 'Diferido',
};

const PAYROLL_FREQUENCY_LABELS: Record<string, string> = {
  WEEKLY: 'Semanal',
  BIWEEKLY: 'Quincenal',
  MONTHLY: 'Mensual',
  CUSTOM: 'Personalizada',
};

const ITEM_STATUS_LABELS: Record<string, string> = {
  ACTIVE: 'Activo',
  DISCONTINUED: 'Descontinuado',
  OUT_OF_STOCK: 'Agotado',
  PENDING_APPROVAL: 'Pendiente de aprobación',
};

const POS_ROLE_LABELS: Record<string, string> = {
  CASHIER: 'Cajero',
  MANAGER: 'Gerente',
  SUPERADMIN: 'Superadmin',
};

const TASK_STATUS_LABELS: Record<string, string> = {
  PENDING: 'Pendiente',
  IN_PROGRESS: 'En progreso',
  COMPLETED: 'Completada',
  CANCELLED: 'Cancelada',
  DELAYED: 'Retrasada',
  ON_HOLD: 'En espera',
  FAILED: 'Fallida',
};

const OPPORTUNITY_STATUS_LABELS: Record<string, string> = {
  NEW: 'Nueva',
  DISCOVERY: 'Exploración',
  PROPOSAL: 'Propuesta',
  NEGOTIATION: 'Negociación',
  WON: 'Ganada',
  LOST: 'Perdida',
  ABANDONED: 'Abandonada',
};

const PROJECT_STATUS_LABELS: Record<string, string> = {
  PLANNING: 'Planeación',
  ACTIVE: 'Activo',
  ON_HOLD: 'En pausa',
  COMPLETED: 'Completado',
  CANCELLED: 'Cancelado',
  ARCHIVED: 'Archivado',
};

const MILESTONE_STATUS_LABELS: Record<string, string> = {
  PENDING: 'Pendiente',
  IN_PROGRESS: 'En progreso',
  COMPLETED: 'Completado',
  DELAYED: 'Retrasado',
  CANCELLED: 'Cancelado',
};
