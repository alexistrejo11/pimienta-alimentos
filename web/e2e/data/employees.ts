import type {
  EmployeeListItemResponse,
  EmployeeResponse,
  EmployeeStatisticsResponse,
} from '../../src/app/core/model/employee/employee.dto';

const stamp = '2026-02-01T10:00:00Z';

export function employeeListItem(
  overrides: Partial<EmployeeListItemResponse> = {},
): EmployeeListItemResponse {
  return {
    id: 7,
    fullName: 'Luis Hernández',
    photoUrl: 'https://example.test/luis.png',
    email: 'luis@pimienta.test',
    department: 'Cocina',
    position: 'Cocinero',
    status: 'ACTIVE',
    hireDate: '2024-03-01',
    ...overrides,
  };
}

export function employeeDetail(overrides: Partial<EmployeeResponse> = {}): EmployeeResponse {
  return {
    id: 7,
    firstName: 'Luis',
    lastName: 'Hernández',
    photoUrl: 'https://example.test/luis.png',
    email: 'luis@pimienta.test',
    phone: '5551112233',
    address: 'Av. 5',
    birthDate: '1992-05-12',
    nationality: 'MX',
    curp: 'HERL920512HDFRRN01',
    rfc: 'HERL920512XX1',
    nss: '12345678901',
    clabe: '012345678901234567',
    employeeNumber: 'E-007',
    position: 'Cocinero',
    department: 'Cocina',
    contractType: 'INDEFINITE',
    workShift: 'MORNING',
    hireDate: '2024-03-01',
    terminationDate: null,
    status: 'ACTIVE',
    salaryPerWeek: 2500,
    bonuses: 0,
    foodVouchers: 0,
    integrationFactor: 1,
    imssWorkerType: null,
    imssSalaryType: null,
    christmasBonusDays: 15,
    vacationDays: 12,
    vacationPremiumPercent: 25,
    createdAt: stamp,
    updatedAt: stamp,
    ...overrides,
  };
}

export function employeeStats(
  overrides: Partial<EmployeeStatisticsResponse> = {},
): EmployeeStatisticsResponse {
  return { total: 4, active: 3, notActive: 1, ...overrides };
}
