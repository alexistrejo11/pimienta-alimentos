import type {
  OpportunitySource,
  OpportunityStatus,
  ProjectPriority,
  ProjectType,
} from './crm.enums';

/** GET /api/v1/opportunities — query (ModelAttribute) */
export interface OpportunitySearchParams {
  status?: OpportunityStatus;
  companyNameContains?: string;
  titleContains?: string;
  page?: number;
  size?: number;
}

/** POST /api/v1/opportunities */
export interface CreateOpportunityRequest {
  title: string;
  description?: string | null;
  contactName: string;
  contactEmail: string;
  contactPhone?: string | null;
  companyName: string;
  companyLocation?: string | null;
  industry?: string | null;
  estimatedValue: number;
  /** 0–100; optional in API, omit or send when known. */
  probabilityPercent?: number | null;
  source: OpportunitySource;
  expectedCloseDate?: string | null;
  assignedSalesmanId?: number | null;
}

/** PATCH /api/v1/opportunities/:id */
export interface UpdateOpportunityRequest {
  title?: string | null;
  description?: string | null;
  contactName?: string | null;
  contactEmail?: string | null;
  contactPhone?: string | null;
  companyName?: string | null;
  companyLocation?: string | null;
  industry?: string | null;
  estimatedValue?: number | null;
  probabilityPercent?: number | null;
  source?: OpportunitySource | null;
  expectedCloseDate?: string | null;
  assignedSalesmanId?: number | null;
}

export interface OpportunityResponse {
  id: number;
  contactName: string;
  contactEmail: string;
  contactPhone: string;
  companyName: string;
  companyLocation: string;
  industry: string;
  title: string;
  description: string;
  estimatedValue: number;
  probabilityPercent: number;
  source: OpportunitySource;
  status: OpportunityStatus;
  expectedCloseDate: string | null;
  actualCloseDate: string | null;
  assignedSalesmanId: number | null;
  lostReason: string | null;
  convertedProjectId: number | null;
  createdAt: string;
  updatedAt: string;
}

/** GET /api/v1/opportunities/:id/summary */
export interface OpportunitySummaryResponse {
  opportunity: OpportunityResponse;
  taskCount: number;
  openTaskCount: number;
  weightedValue: number;
  overdue: boolean;
}

/** POST /api/v1/opportunities/:id/win */
export interface WinOpportunityRequest {
  projectCode: string;
  projectName: string;
  description?: string | null;
  clientId: number;
  type: ProjectType;
  priority: ProjectPriority;
  projectManagerId?: number | null;
  assignedSalesmanId?: number | null;
  plannedStartDate?: string | null;
  plannedEndDate?: string | null;
  contractedValue: number;
  estimatedCost: number;
}

/** POST /api/v1/opportunities/:id/lose */
export interface LoseOpportunityRequest {
  reason: string;
}
