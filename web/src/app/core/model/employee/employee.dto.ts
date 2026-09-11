import type {
  ContractType,
  EmployeeOnboardingPhase,
  EmployeeStatus,
  ImssSalaryType,
  ImssWorkerType,
  WorkShift,
} from './employee.enums';

/** Query params for GET /api/v1/employees and /export. */
export interface EmployeeSearchParams {
  status?: EmployeeStatus;
  department?: string;
  q?: string;
  page?: number;
  size?: number;
}

/** POST /api/v1/employees — solo firstName/lastName obligatorios. */
export interface RegisterEmployeeRequest {
  firstName: string;
  lastName: string;
  email?: string | null;
  phone?: string | null;
  address?: string | null;
  curp?: string | null;
  rfc?: string | null;
  nss?: string | null;
  clabe?: string | null;
  employeeNumber?: string | null;
  position?: string | null;
  department?: string | null;
  contractType?: ContractType | null;
  workShift?: WorkShift | null;
  salaryPerWeek?: number | null;
  /** ISO date (yyyy-MM-dd). */
  birthDate?: string | null;
  onboardingPhase?: EmployeeOnboardingPhase | null;
}

/** PUT /api/v1/employees/:id — solo firstName/lastName obligatorios. */
export interface UpdateEmployeeRequest {
  firstName: string;
  lastName: string;
  email?: string | null;
  phone?: string | null;
  address?: string | null;
  curp?: string | null;
  rfc?: string | null;
  nss?: string | null;
  clabe?: string | null;
  position?: string | null;
  department?: string | null;
  contractType?: ContractType | null;
  workShift?: WorkShift | null;
  salaryPerWeek?: number | null;
  bonuses?: number | null;
  foodVouchers?: number | null;
  integrationFactor?: number | null;
}

/** PUT …/promote, …/demote */
export interface ChangePositionRequest {
  position: string;
}

/** GET /api/v1/employees/:id */
export interface EmployeeResponse {
  id: number;
  firstName: string;
  lastName: string;
  /** HTTPS URL (S3 presigned, legacy, or ui-avatars placeholder). */
  photoUrl: string;
  email: string;
  phone: string;
  address: string;
  birthDate: string | null;
  nationality: string;
  curp: string;
  rfc: string;
  nss: string;
  clabe: string;
  employeeNumber: string;
  position: string;
  department: string;
  contractType: ContractType | null;
  workShift: WorkShift | null;
  hireDate: string | null;
  terminationDate: string | null;
  status: EmployeeStatus;
  salaryPerWeek: number | null;
  bonuses: number | null;
  foodVouchers: number | null;
  integrationFactor: number | null;
  imssWorkerType: ImssWorkerType | null;
  imssSalaryType: ImssSalaryType | null;
  christmasBonusDays: number;
  vacationDays: number;
  vacationPremiumPercent: number;
  createdAt: string;
  updatedAt: string;
}

/** GET /api/v1/employees list item */
export interface EmployeeListItemResponse {
  id: number;
  fullName: string;
  photoUrl: string;
  email: string;
  department: string;
  position: string;
  status: EmployeeStatus;
  hireDate: string | null;
}

export interface EmployeeStatisticsResponse {
  total: number;
  active: number;
  notActive: number;
}

export interface DepartmentHeadcountResponse {
  department: string;
  headcount: number;
}

export interface EmployeeSummaryResponse {
  totalNotDeleted: number;
  headcountByDepartment: DepartmentHeadcountResponse[];
}
