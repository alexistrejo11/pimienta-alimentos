import type { Gender } from './enums';

/** GET /api/v1/users/me — perfil del usuario autenticado (JWT). */
export interface UserResponse {
  id: number;
  email: string;
  firstName: string;
  lastName: string;
  gender: Gender;
  phone: string;
  /** ISO date (yyyy-MM-dd). */
  dateOfBirth: string;
  banned: boolean;
  bannedReason: string | null;
  bannedAt: string | null;
  roles: string[];
  permissions: string[];
  createdAt: string;
  updatedAt: string;
}

/** PATCH /api/v1/users/me — cuerpo alineado con {@code UpdateProfileRequest} en el backend. */
export interface UpdateProfileRequest {
  firstName: string;
  lastName: string;
  gender: Gender;
  phone: string;
  /** ISO date (yyyy-MM-dd). */
  dateOfBirth: string;
}

/** GET /api/v1/users/me/dashboard — métricas para el panel del gestor. */
export interface UserDashboardResponse {
  totalActiveEmployees: number;
  totalActiveProjects: number;
  totalActiveHeadquarters: number;
  totalActivePersonalTasks: number;
  totalPendingPersonalTasks: number;
  totalActiveEmployeesTasks: number;
  totalEmployeePending: number;
}

/** GET /api/v1/users/management/statistics */
export interface UserStatisticsResponse {
  totalUsers: number;
  activeUsers: number;
  bannedUsers: number;
}

/** POST /api/v1/users/management/:id/roles */
export interface AddRolesRequest {
  roles: string[];
}

/** POST /api/v1/users/management/:id/ban */
export interface BanUserRequest {
  reason?: string | null;
}
