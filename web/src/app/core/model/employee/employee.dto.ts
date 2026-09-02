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

/** POST /api/v1/employees — registro en onboarding (multipart/form-data, part "employee"). */
export interface RegisterEmployeeRequest {
  firstName: string;
  lastName: string;
  email: string;
  phone: string;
  address: string;
  curp: string;
  rfc: string;
  nss: string;
  clabe: string;
  employeeNumber: string;
  position: string;
  department: string;
  contractType: ContractType;
  workShift: WorkShift;
  salaryPerWeek: number;
  /** ISO date (yyyy-MM-dd). */
  birthDate: string;
  onboardingPhase: EmployeeOnboardingPhase;
}

/** PUT /api/v1/employees/:id — multipart/form-data, part "employee". */
export interface UpdateEmployeeRequest {
  firstName: string;
  lastName: string;
  email: string;
  phone: string;
  address: string;
  curp: string;
  rfc: string;
  nss: string;
  clabe: string;
  position: string;
  department: string;
  contractType: ContractType;
  workShift: WorkShift;
  salaryPerWeek: number;
  bonuses: number;
  foodVouchers: number;
  integrationFactor: number;
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
  /** HTTPS URL (S3 presigned) or empty string when no photo. */
  photoUrl: string;
  email: string;
  phone: string;
  address: string;
  birthDate: string;
  nationality: string;
  curp: string;
  rfc: string;
  nss: string;
  clabe: string;
  employeeNumber: string;
  position: string;
  department: string;
  contractType: ContractType;
  workShift: WorkShift;
  hireDate: string | null;
  terminationDate: string | null;
  status: EmployeeStatus;
  salaryPerWeek: number;
  bonuses: number;
  foodVouchers: number;
  integrationFactor: number;
  imssWorkerType: ImssWorkerType;
  imssSalaryType: ImssSalaryType;
  christmasBonusDays: number;
  vacationDays: number;
  vacationPremiumPercent: number;
  createdAt: string;
  updatedAt: string;
}

/** GET /api/v1/employees (list, search, active) */
export interface EmployeeListItemResponse {
  id: number;
  fullName: string;
  /** HTTPS URL (S3 presigned) or empty string when no photo. */
  photoUrl: string;
  email: string;
  department: string;
  position: string;
  status: EmployeeStatus;
  hireDate: string | null;
}

/** GET /api/v1/employees/summary */
export interface DepartmentHeadcountResponse {
  department: string;
  headcount: number;
}

export interface EmployeeSummaryResponse {
  totalNotDeleted: number;
  headcountByDepartment: DepartmentHeadcountResponse[];
}

/** GET /api/v1/employees/statistics */
export interface EmployeeStatisticsResponse {
  total: number;
  active: number;
  notActive: number;
}
