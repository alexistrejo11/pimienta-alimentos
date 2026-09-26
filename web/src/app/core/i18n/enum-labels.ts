/**
 * Spanish UI labels for API enum wire values (English codes stay on the wire).
 * Never show raw enum codes in templates — always use a label helper from this file.
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

export function itemCategoryLabel(category: string): string {
  return ITEM_CATEGORY_LABELS[category] ?? category;
}

export function itemUnitLabel(unit: string): string {
  return ITEM_UNIT_LABELS[unit] ?? unit;
}

export function catalogRoleLabel(role: string): string {
  return CATALOG_ROLE_LABELS[role] ?? role;
}

export function inventoryStatusLabel(status: string): string {
  return INVENTORY_STATUS_LABELS[status] ?? status;
}

export function inventoryCountStatusLabel(status: string): string {
  return INVENTORY_COUNT_STATUS_LABELS[status] ?? status;
}

export function inventoryCountTypeLabel(type: string): string {
  return INVENTORY_COUNT_TYPE_LABELS[type] ?? type;
}

export function locationTypeLabel(type: string): string {
  return LOCATION_TYPE_LABELS[type] ?? type;
}

export function stockPolicyLabel(policy: string): string {
  return STOCK_POLICY_LABELS[policy] ?? policy;
}

export function inventoryTransactionTypeLabel(type: string): string {
  return INVENTORY_TRANSACTION_TYPE_LABELS[type] ?? type;
}

export function inventoryTransactionStatusLabel(status: string): string {
  return INVENTORY_TRANSACTION_STATUS_LABELS[status] ?? status;
}

export function inventoryMovementTypeLabel(type: string): string {
  return INVENTORY_MOVEMENT_TYPE_LABELS[type] ?? type;
}

export function movementDirectionLabel(direction: string): string {
  return MOVEMENT_DIRECTION_LABELS[direction] ?? direction;
}

export function posRoleLabel(role: string): string {
  return POS_ROLE_LABELS[role] ?? role;
}

export function posDeviceStatusLabel(status: string): string {
  return POS_DEVICE_STATUS_LABELS[status] ?? status;
}

export function posSaleTicketStatusLabel(status: string): string {
  return POS_SALE_TICKET_STATUS_LABELS[status] ?? status;
}

export function posSyncEventStatusLabel(status: string): string {
  return POS_SYNC_EVENT_STATUS_LABELS[status] ?? status;
}

export function posShiftStatusLabel(status: string): string {
  return POS_SHIFT_STATUS_LABELS[status] ?? status;
}

const INVENTORY_EXIT_REASON_LABELS: Record<string, string> = {
  SCRAP: 'Merma genérica',
  DAMAGED: 'Deteriorado',
  EXPIRED: 'Caducado',
  INTERNAL_USE: 'Uso interno',
  INVENTORY_ADJUSTMENT: 'Ajuste de inventario',
};

export function inventoryExitReasonLabel(reason: string): string {
  return INVENTORY_EXIT_REASON_LABELS[reason] ?? reason;
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

export function contractCategoryLabel(category: string): string {
  return CONTRACT_CATEGORY_LABELS[category] ?? category;
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
  MALE: 'Hombre',
  FEMALE: 'Mujer',
  OTHER: 'Otro',
};

const ROLE_LABELS: Record<string, string> = {
  USER: 'Usuario',
  SUPPORT: 'Soporte',
  SALES: 'Ventas',
  MANAGER: 'Gerente',
  DIRECTOR: 'Director',
  ADMIN: 'Administrador',
  EMPLOYEE: 'Empleado',
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

const ITEM_CATEGORY_LABELS: Record<string, string> = {
  RAW_MATERIAL: 'Materia prima',
  FINISHED_GOOD: 'Producto terminado',
  CONSUMABLE: 'Consumible',
  SPARE_PART: 'Refacción',
  PACKAGING: 'Empaque',
  TOOL: 'Herramienta',
  MACHINE: 'Máquina',
  FURNITURE: 'Mobiliario',
  OTHER: 'Otro',
};

const ITEM_UNIT_LABELS: Record<string, string> = {
  PIECE: 'Pieza',
  KG: 'Kilogramo',
  GRAM: 'Gramo',
  LITER: 'Litro',
  ML: 'Mililitro',
  BOX: 'Caja',
  DOZEN: 'Docena',
  METER: 'Metro',
  SQUARE_METER: 'Metro cuadrado',
};

const CATALOG_ROLE_LABELS: Record<string, string> = {
  INVENTORY_ONLY: 'Sólo inventario',
  POS_SELLABLE: 'Vendible en POS',
};

const INVENTORY_STATUS_LABELS: Record<string, string> = {
  NORMAL: 'Normal',
  LOW_STOCK: 'Stock bajo',
  OUT_OF_STOCK: 'Agotado',
  OVERSTOCKED: 'Sobre stock',
};

const INVENTORY_COUNT_STATUS_LABELS: Record<string, string> = {
  DRAFT: 'Borrador',
  SUBMITTED: 'Enviado',
  APPROVED: 'Aprobado',
  CANCELLED: 'Cancelado',
};

const INVENTORY_COUNT_TYPE_LABELS: Record<string, string> = {
  FULL: 'Completo',
  PARTIAL: 'Parcial',
};

const LOCATION_TYPE_LABELS: Record<string, string> = {
  WAREHOUSE: 'Almacén',
  ZONE: 'Zona',
  AISLE: 'Pasillo',
  SHELF: 'Estante',
  BIN: 'Contenedor',
  POS: 'Punto de venta',
};

const STOCK_POLICY_LABELS: Record<string, string> = {
  CONTROLLED: 'Inventariable',
  NOT_CONTROLLED: 'Sin control de stock',
};

const INVENTORY_TRANSACTION_TYPE_LABELS: Record<string, string> = {
  PURCHASE_RECEIPT: 'Entrada por compra', SALE_DISPATCH: 'Salida por venta', INTERNAL_TRANSFER: 'Transferencia interna',
  PHYSICAL_COUNT: 'Conteo físico', RETURN_FROM_CLIENT: 'Devolución de cliente', RETURN_TO_SUPPLIER: 'Devolución a proveedor',
  PRODUCTION_ISSUE: 'Consumo de producción', SCRAP_WRITE_OFF: 'Merma',
};

const INVENTORY_TRANSACTION_STATUS_LABELS: Record<string, string> = {
  DRAFT: 'Borrador', PENDING: 'Pendiente', APPROVED: 'Aprobado', IN_PROGRESS: 'En proceso', COMPLETED: 'Completado', CANCELLED: 'Cancelado',
};

const INVENTORY_MOVEMENT_TYPE_LABELS: Record<string, string> = {
  PURCHASE: 'Compra', RETURN_FROM_CLIENT: 'Devolución de cliente', INITIAL_STOCK: 'Existencia inicial', SALE: 'Venta',
  RETURN_TO_SUPPLIER: 'Devolución a proveedor', USAGE: 'Consumo', SCRAP: 'Merma', TRANSFER: 'Transferencia',
  ADJUSTMENT_PLUS: 'Ajuste positivo', ADJUSTMENT_MINUS: 'Ajuste negativo',
};

const MOVEMENT_DIRECTION_LABELS: Record<string, string> = { IN: 'Entrada', OUT: 'Salida', NEUTRAL: 'Ajuste' };

const POS_ROLE_LABELS: Record<string, string> = {
  CASHIER: 'Cajero',
  MANAGER: 'Gerente',
  SUPERADMIN: 'Superadmin',
};

const POS_DEVICE_STATUS_LABELS: Record<string, string> = {
  PENDING: 'Pendiente',
  AUTHORIZED: 'Autorizado',
  REVOKED: 'Revocado',
  DISABLED: 'Deshabilitado',
};

const POS_SALE_TICKET_STATUS_LABELS: Record<string, string> = {
  CONFIRMED: 'Confirmada',
  CANCELLED: 'Cancelada',
};

const POS_SYNC_EVENT_STATUS_LABELS: Record<string, string> = {
  ACCEPTED: 'Sincronizada',
  REQUIRES_REVIEW: 'Pendiente de revisión',
  REJECTED: 'Rechazada',
  DUPLICATE: 'Duplicada',
};

const POS_SHIFT_STATUS_LABELS: Record<string, string> = {
  OPEN: 'Abierto',
  CLOSED: 'Cerrado',
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

const CONTRACT_CATEGORY_LABELS: Record<string, string> = {
  SUPPLIER: 'Proveedor',
  CUSTOMER: 'Cliente',
  EMPLOYEE: 'Colaborador',
  PARTNER: 'Socio',
  OTHER: 'Otro',
};
