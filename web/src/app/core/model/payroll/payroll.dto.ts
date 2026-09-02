import type { PayrollAdjustmentType, PayrollFrequency, PayrollRecordStatus } from './payroll.enums';

/** GET /api/v1/payroll/records — query (extiende paginación en backend). */
export interface PayrollRecordSearchParams {
  page?: number;
  size?: number;
  employeeId?: number;
  periodId?: number;
}

export interface PayrollRecordResponse {
  id: number;
  employeeId: number;
  periodId: number | null;
  workedDaysStart: string;
  workedDaysEnd: string;
  grossAmount: number;
  totalDiscounts: number;
  totalBonuses: number;
  netAmount: number;
  status: PayrollRecordStatus;
}

export interface RegisterPayrollRecordRequest {
  employeeId: number;
  periodId?: number | null;
  workedDaysStart: string;
  workedDaysEnd: string;
  grossAmount: number;
}

export interface RegisterPayrollPeriodRequest {
  frequency: PayrollFrequency;
  startDate: string;
  endDate: string;
}

export interface PayrollPeriodResponse {
  id: number;
  frequency: PayrollFrequency;
  startDate: string;
  endDate: string;
  status: string;
}

export interface RegisterPayrollPaymentRequest {
  employeeId: number;
  frequency: PayrollFrequency;
  workedDaysStart: string;
  workedDaysEnd: string;
  grossAmount: number;
  netAmount: number;
  destinationAccount: string;
  transactionId: string;
}

export interface PayrollPaymentResponse {
  id: number;
  employeeId: number;
  frequency: PayrollFrequency;
  workedDaysStart: string;
  workedDaysEnd: string;
  grossAmount: number;
  netAmount: number;
  destinationAccount: string | null;
  transactionId: string | null;
  status: string;
  pendingAmount: number | null;
  createdAt: string;
}

export interface RegisterPayrollAdjustmentRequest {
  type: PayrollAdjustmentType;
  amount: number;
  reason: string;
}

export interface PayrollSummaryResponse {
  from: string | null;
  to: string | null;
  employeesPaid: number;
  totalGross: number;
  totalNet: number;
  totalDiscounts: number;
  totalBonuses: number;
  totalDebt: number;
}

export interface PayrollDebtResponse {
  id: number;
  employeeId: number;
  payrollRecordId: number | null;
  amountOwed: number;
  reason: string | null;
  settled: boolean;
  createdAt: string | null;
  settledAt: string | null;
}

/** Alcance para import/export masivo (query). */
export interface PayrollBulkScopeParams {
  page?: number;
  size?: number;
  employeeId?: number;
  periodId?: number;
  from?: string;
  to?: string;
}

export interface SpreadsheetBulkImportResult {
  updated: number;
  created: number;
  skipped: number;
  errors: { excelRowNumber: number; message: string }[];
}
