/** Wire values for {@code OperatingExpenseCategory}. Labels live in enum-labels. */
export const OPERATING_EXPENSE_CATEGORIES = [
  'SUPPLIERS_MERCHANDISE',
  'RENT',
  'UTILITIES',
  'MAINTENANCE',
  'SUPPLIES_PETTY_CASH',
  'OTHER',
] as const;

export type OperatingExpenseCategory = (typeof OPERATING_EXPENSE_CATEGORIES)[number];

/** GET /api/v1/operating-expenses item and detail. */
export interface OperatingExpenseAttachmentResponse {
  key: string;
  url: string;
}

export interface OperatingExpenseResponse {
  id: number;
  headquarterId: number;
  amount: number | string;
  category: OperatingExpenseCategory;
  concept: string;
  notes: string | null;
  occurredOn: string;
  recordedByUserId: number;
  attachments: OperatingExpenseAttachmentResponse[];
  createdAt: string;
  updatedAt: string;
}

/** One category inside GET /api/v1/operating-expenses/summary. */
export interface OperatingExpenseCategoryTotal {
  category: OperatingExpenseCategory;
  totalAmount: number | string;
  count: number;
}

/** One business day inside GET /api/v1/operating-expenses/summary. Days without expenses are omitted. */
export interface OperatingExpenseDayTotal {
  occurredOn: string;
  totalAmount: number | string;
  count: number;
}

/** GET /api/v1/operating-expenses/summary */
export interface OperatingExpenseSummaryResponse {
  totalAmount: number | string;
  topCategory: OperatingExpenseCategoryTotal | null;
  byDay: OperatingExpenseDayTotal[];
}

export interface OperatingExpenseSearchParams {
  page?: number;
  size?: number;
  search?: string;
  headquarterId?: number;
  from?: string;
  to?: string;
  category?: OperatingExpenseCategory;
}

/** JSON part `data` of POST/PUT /api/v1/operating-expenses. */
export interface UpsertOperatingExpenseRequest {
  headquarterId?: number | null;
  amount: number;
  category: OperatingExpenseCategory;
  concept: string;
  notes?: string | null;
  occurredOn?: string | null;
  /** Present on update. Empty array drops every existing receipt. */
  retainAttachmentKeys?: string[];
}
