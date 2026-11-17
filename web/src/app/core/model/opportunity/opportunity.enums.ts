export type OrganizationKind = 'PUBLIC_INSTITUTION' | 'PRIVATE_COMPANY' | 'OTHER' | 'UNDEFINED';

export type OpportunityStatus =
  | 'DRAFT'
  | 'UNDER_REVIEW'
  | 'PREPARING'
  | 'SUBMITTED'
  | 'WON'
  | 'LOST'
  | 'ABANDONED'
  | 'CANCELLED'
  | 'UNDEFINED';

export const ORGANIZATION_KINDS: OrganizationKind[] = [
  'PUBLIC_INSTITUTION',
  'PRIVATE_COMPANY',
  'OTHER',
];

export const OPPORTUNITY_STATUSES: OpportunityStatus[] = [
  'DRAFT',
  'UNDER_REVIEW',
  'PREPARING',
  'SUBMITTED',
  'WON',
  'LOST',
  'ABANDONED',
  'CANCELLED',
];

export type RequirementCategory =
  | 'ADMINISTRATIVE'
  | 'LEGAL'
  | 'TECHNICAL'
  | 'ECONOMIC'
  | 'OTHER'
  | 'UNDEFINED';

export type RequirementStatus =
  | 'PENDING'
  | 'IN_PROGRESS'
  | 'DONE'
  | 'BLOCKED'
  | 'WAIVED'
  | 'UNDEFINED';

export const REQUIREMENT_CATEGORIES: RequirementCategory[] = [
  'ADMINISTRATIVE',
  'LEGAL',
  'TECHNICAL',
  'ECONOMIC',
  'OTHER',
];

export const REQUIREMENT_STATUSES: RequirementStatus[] = [
  'PENDING',
  'IN_PROGRESS',
  'DONE',
  'BLOCKED',
  'WAIVED',
];

export type ProposalKind = 'TECHNICAL' | 'ECONOMIC' | 'COMBINED' | 'UNDEFINED';

export const PROPOSAL_KINDS: ProposalKind[] = ['TECHNICAL', 'ECONOMIC', 'COMBINED'];
