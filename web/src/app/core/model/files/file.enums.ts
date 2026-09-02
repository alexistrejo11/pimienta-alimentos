/** Storage category for company file assets. */
export type FileCategory = 'TEMPLATE' | 'COMPANY' | 'EXTRAS' | 'RESOURCE';

/** Module slugs for RESOURCE-scoped files (manager dropdown). */
export const FILE_MODULE_OPTIONS: { value: string; label: string }[] = [
  { value: 'crm', label: 'CRM' },
  { value: 'employees', label: 'Empleados' },
  { value: 'payroll', label: 'Nómina' },
  { value: 'inventory', label: 'Inventario' },
  { value: 'contracts', label: 'Contratos' },
  { value: 'tasks', label: 'Tareas' },
  { value: 'headquarters', label: 'Sedes' },
];

export const FILE_CATEGORY_LABELS: Record<FileCategory, string> = {
  TEMPLATE: 'Plantilla',
  COMPANY: 'Empresa',
  EXTRAS: 'Extras',
  RESOURCE: 'Recurso de módulo',
};
