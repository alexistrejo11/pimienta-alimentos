import type { ContractCategory, ContractTermKind } from './contract.enums';

/** POST/PUT /api/v1/contracts */
export interface CreateOrUpdateContractRequest {
  name: string;
  description?: string | null;
  category: ContractCategory;
  employeeId?: number | null;
  opportunityId?: number | null;
  projectId?: number | null;
  termKind: ContractTermKind;
  effectiveStart: string;
  effectiveEnd?: string | null;
  /** URL del documento firmado (p. ej. almacenamiento del front). */
  documentUrl: string;
  termsAndConditions?: string | null;
  referenceCode?: string | null;
  renewalCycleMonths?: number | null;
  agreedValue?: number | null;
  currencyCode?: string | null;
}

export interface ExtendContractRequest {
  newEnd: string;
}

export interface ContractResponse {
  id: number;
  name: string;
  description: string;
  category: ContractCategory;
  employeeId: number | null;
  opportunityId: number | null;
  projectId: number | null;
  termKind: ContractTermKind;
  effectiveStart: string;
  effectiveEnd: string | null;
  documentUrl: string | null;
  termsAndConditions: string | null;
  referenceCode: string | null;
  agreedValue: number | null;
  currencyCode: string | null;
  renewalCycleMonths: number | null;
  extensionCount: number;
  lastRenewedAt: string | null;
  createdAt: string;
  updatedAt: string;
}
