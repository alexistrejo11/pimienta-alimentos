/** Mirrors {@link io.github.alexistrejo11.pimienta.module.crm.core.domain.Opportunity.OpportunityStatus}. */
export type OpportunityStatus =
  | 'NEW'
  | 'DISCOVERY'
  | 'PROPOSAL'
  | 'NEGOTIATION'
  | 'WON'
  | 'LOST'
  | 'ABANDONED';

/** Mirrors {@link io.github.alexistrejo11.pimienta.module.crm.core.domain.Opportunity.OpportunitySource}. */
export type OpportunitySource =
  | 'INBOUND'
  | 'OUTBOUND'
  | 'REFERRAL'
  | 'SOCIAL_MEDIA'
  | 'EVENT'
  | 'COLD_CALL'
  | 'OTHER';

/** Mirrors {@link io.github.alexistrejo11.pimienta.module.crm.core.domain.Project.ProjectStatus}. */
export type ProjectStatus =
  | 'PLANNING'
  | 'ACTIVE'
  | 'ON_HOLD'
  | 'COMPLETED'
  | 'CANCELLED'
  | 'ARCHIVED';

/** Mirrors {@link io.github.alexistrejo11.pimienta.module.crm.core.domain.Project.ProjectType}. */
export type ProjectType =
  | 'CONSULTING'
  | 'SOFTWARE_DEVELOPMENT'
  | 'IMPLEMENTATION'
  | 'MAINTENANCE'
  | 'TRAINING'
  | 'RESEARCH'
  | 'OTHER';

/** Mirrors {@link io.github.alexistrejo11.pimienta.module.crm.core.domain.Project.ProjectPriority}. */
export type ProjectPriority = 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL';

/** Mirrors {@link io.github.alexistrejo11.pimienta.module.crm.core.domain.ProjectMilestone.MilestoneStatus}. */
export type MilestoneStatus =
  | 'PENDING'
  | 'IN_PROGRESS'
  | 'COMPLETED'
  | 'DELAYED'
  | 'CANCELLED';
