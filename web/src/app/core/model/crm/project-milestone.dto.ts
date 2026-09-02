import type { MilestoneStatus } from './crm.enums';

/** POST /api/v1/projects/:projectId/milestones */
export interface CreateProjectMilestoneRequest {
  name: string;
  description?: string | null;
  plannedDate?: string | null;
  billingAmount: number;
  sortOrder: number;
}

/** PATCH /api/v1/projects/:projectId/milestones/:milestoneId */
export interface UpdateProjectMilestoneRequest {
  name?: string | null;
  description?: string | null;
  plannedDate?: string | null;
  billingAmount?: number | null;
  sortOrder?: number | null;
}

export interface ProjectMilestoneResponse {
  id: number;
  projectId: number;
  name: string;
  description: string;
  status: MilestoneStatus;
  plannedDate: string | null;
  actualDate: string | null;
  billingAmount: number;
  billed: boolean;
  sortOrder: number;
  createdAt: string;
  updatedAt: string;
}
