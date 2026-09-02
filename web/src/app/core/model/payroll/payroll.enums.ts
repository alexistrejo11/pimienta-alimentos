/** Estados de un registro de nómina (API Payroll). */
export type PayrollRecordStatus = 'PENDING' | 'PAID' | 'PARTIAL' | 'DEFERRED';

/** Frecuencia de período / pago. */
export type PayrollFrequency = 'WEEKLY' | 'BIWEEKLY' | 'MONTHLY' | 'CUSTOM';

/** Tipo de ajuste sobre un registro. */
export type PayrollAdjustmentType = 'DISCOUNT' | 'BONUS';
