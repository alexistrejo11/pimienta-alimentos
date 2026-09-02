import type { TaskPriority, TaskStatus } from './task.enums';

/** POST /api/v1/tasks, POST …/opportunities/:id/tasks, … */
export interface ChecklistLineRequest {
  description: string;
  displayOrder: number;
}

export interface TaskRequest {
  title: string;
  description?: string | null;
  priority: TaskPriority;
  dueDate?: string | null;
  headquarterId?: number | null;
  projectId?: number | null;
  opportunityId?: number | null;
  createdById?: number | null;
  checklist?: ChecklistLineRequest[] | null;
}

/** GET /api/v1/tasks — query params (ModelAttribute) */
export interface TaskSearchParams {
  headquarterId?: number;
  projectId?: number;
  opportunityId?: number;
  employeeId?: number;
  status?: TaskStatus;
  page?: number;
  size?: number;
}

/** PATCH /api/v1/tasks/:id/status */
export interface StatusUpdateRequest {
  status: TaskStatus;
}

/** PATCH /api/v1/tasks/:id/assign */
export interface AssignTaskRequest {
  employeeId: number;
}

/** GET /api/v1/tasks/:id */
export interface ChecklistItemResponse {
  description: string;
  completed: boolean;
  completedAt: string | null;
  displayOrder: number;
}

export interface TaskResponse {
  id: number;
  title: string;
  description: string;
  status: TaskStatus;
  priority: TaskPriority;
  assignedToId: number | null;
  assignedById: number | null;
  createdById: number | null;
  assignedAt: string | null;
  completedAt: string | null;
  dueDate: string | null;
  headquarterId: number | null;
  projectId: number | null;
  opportunityId: number | null;
  checklist: ChecklistItemResponse[];
  progressPercent: number;
  createdAt: string;
  updatedAt: string;
}

/** GET /api/v1/tasks (list) */
export interface TaskListItemResponse {
  id: number;
  title: string;
  status: TaskStatus;
  priority: TaskPriority;
  assignedToId: number | null;
  dueDate: string | null;
  headquarterId: number | null;
  projectId: number | null;
  opportunityId: number | null;
  progressPercent: number;
}
