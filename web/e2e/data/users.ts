import { AppRole, type AccountStatus } from '../../src/app/core/model/account/enums';
import type { UserDashboardResponse, UserResponse } from '../../src/app/core/model/account/user.dto';

const stamp = '2026-01-15T12:00:00Z';

export function userResponse(overrides: Partial<UserResponse> = {}): UserResponse {
  return {
    id: 1,
    email: 'admin@pimienta.test',
    firstName: 'Ana',
    lastName: 'Admin',
    gender: 'FEMALE',
    phone: '5550000001',
    dateOfBirth: '1990-01-01',
    accountStatus: 'ACTIVE',
    bannedReason: null,
    bannedAt: null,
    roles: [AppRole.ADMIN],
    permissions: [],
    assignedHeadquarterIds: [10],
    createdAt: stamp,
    updatedAt: stamp,
    ...overrides,
  };
}

export const adminUser = userResponse();

export const directorUser = userResponse({
  id: 2,
  email: 'director@pimienta.test',
  firstName: 'Diego',
  lastName: 'Director',
  roles: [AppRole.DIRECTOR],
  assignedHeadquarterIds: [10],
});

export const managerUser = userResponse({
  id: 3,
  email: 'gerente@pimienta.test',
  firstName: 'Marta',
  lastName: 'Gerente',
  roles: [AppRole.MANAGER],
  assignedHeadquarterIds: [10],
});

export const employeeUser = userResponse({
  id: 4,
  email: 'empleado@pimienta.test',
  firstName: 'Eva',
  lastName: 'Empleada',
  roles: [AppRole.EMPLOYEE],
  assignedHeadquarterIds: [10],
});

export function managementUser(
  overrides: Partial<UserResponse> & { accountStatus?: AccountStatus } = {},
): UserResponse {
  return userResponse({
    id: 20,
    email: 'pendiente@pimienta.test',
    firstName: 'Pedro',
    lastName: 'Pendiente',
    roles: [AppRole.USER],
    accountStatus: 'PENDING_APPROVAL',
    assignedHeadquarterIds: [],
    ...overrides,
  });
}

export function userDashboard(overrides: Partial<UserDashboardResponse> = {}): UserDashboardResponse {
  return {
    totalActiveEmployees: 12,
    totalActiveProjects: 3,
    totalActiveHeadquarters: 2,
    totalActivePersonalTasks: 4,
    totalPendingPersonalTasks: 1,
    totalActiveEmployeesTasks: 8,
    totalEmployeePending: 2,
    ...overrides,
  };
}
