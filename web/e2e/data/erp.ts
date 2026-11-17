import type { ContractResponse } from '../../src/app/core/model/contract/contract.dto';
import type { PayrollRecordResponse, PayrollSummaryResponse } from '../../src/app/core/model/payroll/payroll.dto';
import type { TaskListItemResponse, TaskResponse } from '../../src/app/core/model/task/task.dto';

export function payrollSummary(overrides: Partial<PayrollSummaryResponse> = {}): PayrollSummaryResponse {
  return {
    from: '2026-11-01',
    to: '2026-11-17',
    employeesPaid: 3,
    totalGross: 15000,
    totalNet: 14000,
    totalDiscounts: 1000,
    totalBonuses: 0,
    totalDebt: 200,
    ...overrides,
  };
}

export function payrollRecord(overrides: Partial<PayrollRecordResponse> = {}): PayrollRecordResponse {
  return {
    id: 1,
    employeeId: 7,
    periodId: 1,
    workedDaysStart: '2026-11-01',
    workedDaysEnd: '2026-11-15',
    grossAmount: 5000,
    totalDiscounts: 0,
    totalBonuses: 0,
    netAmount: 5000,
    status: 'PENDING',
    ...overrides,
  };
}

export function taskListItem(overrides: Partial<TaskListItemResponse> = {}): TaskListItemResponse {
  return {
    id: 3,
    title: 'Inventario semanal',
    status: 'PENDING',
    priority: 'MEDIUM',
    assignedToId: null,
    dueDate: '2026-11-20',
    headquarterId: 10,
    projectId: null,
    opportunityId: null,
    progressPercent: 0,
    ...overrides,
  };
}

export function taskDetail(overrides: Partial<TaskResponse> = {}): TaskResponse {
  return {
    id: 3,
    title: 'Inventario semanal',
    description: 'Contar bodega',
    status: 'PENDING',
    priority: 'MEDIUM',
    assignedToId: null,
    assignedById: null,
    createdById: 1,
    assignedAt: null,
    completedAt: null,
    dueDate: '2026-11-20',
    headquarterId: 10,
    projectId: null,
    opportunityId: null,
    checklist: [{ description: 'Contar aceite', completed: false, completedAt: null, displayOrder: 1 }],
    progressPercent: 0,
    createdAt: '2026-11-01T00:00:00Z',
    updatedAt: '2026-11-01T00:00:00Z',
    ...overrides,
  };
}

export function contract(overrides: Partial<ContractResponse> = {}): ContractResponse {
  return {
    id: 4,
    name: 'Contrato proveedor aceite',
    description: '',
    category: 'SUPPLIER',
    employeeId: null,
    opportunityId: null,
    projectId: null,
    termKind: 'INDEFINITE',
    effectiveStart: '2026-01-01',
    effectiveEnd: null,
    documentUrl: 'https://files.test/c.pdf',
    termsAndConditions: null,
    referenceCode: 'C-4',
    renewalCycleMonths: null,
    agreedValue: null,
    currencyCode: null,
    extensionCount: 0,
    lastRenewedAt: null,
    createdAt: '2026-01-01T00:00:00Z',
    updatedAt: '2026-01-01T00:00:00Z',
    ...overrides,
  };
}
