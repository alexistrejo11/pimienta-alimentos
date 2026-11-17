import type {
  OrganizationKind,
  OpportunityStatus,
  ProposalKind,
  RequirementCategory,
  RequirementStatus,
} from './opportunity.enums';

export interface OrganizationResponse {
  id: number;
  name: string;
  kind: OrganizationKind;
  notes: string | null;
  contactName: string | null;
  contactEmail: string | null;
  contactPhone: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface UpsertOrganizationRequest {
  name: string;
  kind: OrganizationKind;
  notes?: string | null;
  contactName?: string | null;
  contactEmail?: string | null;
  contactPhone?: string | null;
}

export interface OpportunityResponse {
  id: number;
  organizationId: number;
  title: string;
  status: OpportunityStatus;
  sourceUrl: string | null;
  externalReference: string | null;
  publishedOn: string | null;
  submissionDeadline: string | null;
  decisionOn: string | null;
  notes: string | null;
  contractId: number | null;
  submittedAt: string | null;
  closedAt: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface CreateOpportunityRequest {
  organizationId: number;
  title: string;
  sourceUrl?: string | null;
  externalReference?: string | null;
  publishedOn?: string | null;
  submissionDeadline?: string | null;
  decisionOn?: string | null;
  notes?: string | null;
}

export type UpdateOpportunityRequest = CreateOpportunityRequest;

export interface RequirementResponse {
  id: number;
  opportunityId: number;
  title: string;
  category: RequirementCategory;
  mandatory: boolean;
  status: RequirementStatus;
  dueOn: string | null;
  notes: string | null;
  sortOrder: number;
  createdAt: string;
  updatedAt: string;
}

export interface CreateRequirementRequest {
  title: string;
  category: RequirementCategory;
  mandatory: boolean;
  dueOn?: string | null;
  notes?: string | null;
  sortOrder?: number | null;
}

export type UpdateRequirementRequest = CreateRequirementRequest;

export interface ProposalResponse {
  id: number;
  opportunityId: number;
  kind: ProposalKind;
  notes: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface UpsertProposalRequest {
  kind: ProposalKind;
  notes?: string | null;
}
