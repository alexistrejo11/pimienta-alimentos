import type { ProjectPriority, ProjectStatus, ProjectType } from './crm.enums';

/** GET /api/v1/projects — query (ModelAttribute) */
export interface ProjectSearchParams {
  status?: ProjectStatus;
  clientId?: number;
  projectManagerId?: number;
  originOpportunityId?: number;
  page?: number;
  size?: number;
}

/** POST /api/v1/projects */
export interface CreateProjectRequest {
  projectCode: string;
  projectName: string;
  description?: string | null;
  clientId: number;
  originOpportunityId?: number | null;
  type: ProjectType;
  priority: ProjectPriority;
  projectManagerId?: number | null;
  assignedSalesmanId?: number | null;
  plannedStartDate?: string | null;
  plannedEndDate?: string | null;
  contractedValue: number;
  estimatedCost: number;
}

/** PATCH /api/v1/projects/:id */
export interface UpdateProjectRequest {
  projectName?: string | null;
  description?: string | null;
  type?: ProjectType | null;
  priority?: ProjectPriority | null;
  projectManagerId?: number | null;
  assignedSalesmanId?: number | null;
  plannedStartDate?: string | null;
  plannedEndDate?: string | null;
  contractedValue?: number | null;
  estimatedCost?: number | null;
  progressPercent?: number | null;
}

export interface ProjectResponse {
  id: number;
  clientId: number;
  originOpportunityId: number | null;
  projectCode: string;
  projectName: string;
  description: string;
  type: ProjectType;
  status: ProjectStatus;
  priority: ProjectPriority;
  projectManagerId: number | null;
  assignedSalesmanId: number | null;
  plannedStartDate: string | null;
  plannedEndDate: string | null;
  actualStartDate: string | null;
  actualEndDate: string | null;
  onHoldReason: string | null;
  contractedValue: number;
  estimatedCost: number;
  actualCost: number;
  progressPercent: number;
  cancellationReason: string | null;
  createdAt: string;
  updatedAt: string;
}

/** GET /api/v1/projects/:id/summary */
export interface ProjectSummaryResponse {
  project: ProjectResponse;
  milestoneCount: number;
  milestoneCompletedCount: number;
  taskCount: number;
  overdue: boolean;
}

/** POST …/hold, …/cancel */
export interface ReasonRequest {
  reason: string;
}
