/** Mirrors {@link io.github.alexistrejo11.pimienta.module.employees.core.domain.ContractType}. */
export type ContractType =
  | 'INDEFINITE'
  | 'FIXED_TERM'
  | 'PROJECT_BASED'
  | 'TEMPORARY'
  | 'FREELANCE';

/** Mirrors {@link io.github.alexistrejo11.pimienta.module.employees.core.domain.WorkShift}. */
export type WorkShift =
  | 'MORNING'
  | 'AFTERNOON'
  | 'NIGHT'
  | 'MIXED'
  | 'REMOTE';

/** Mirrors {@link io.github.alexistrejo11.pimienta.module.employees.core.domain.EmployeeStatus}. */
export type EmployeeStatus =
  | 'DRAFT'
  | 'PENDING_CONTRACT'
  | 'ACTIVE'
  | 'SICK'
  | 'ON_VACATION'
  | 'ON_LEAVE'
  | 'TERMINATED'
  | 'FIRED'
  | 'RESIGNED';

/** Mirrors {@link io.github.alexistrejo11.pimienta.module.employees.core.domain.enums.EmployeeOnboardingPhase}. */
export type EmployeeOnboardingPhase = 'DRAFT' | 'PENDING_CONTRACT';

/** Mirrors {@link io.github.alexistrejo11.pimienta.module.employees.core.domain.ImssSalaryType}. */
export type ImssSalaryType = 'FIXED' | 'VARIABLE' | 'MIXED';

/** Mirrors {@link io.github.alexistrejo11.pimienta.module.employees.core.domain.ImssWorkerType}. */
export type ImssWorkerType =
  | 'PERMANENT_URBAN'
  | 'EVENTUAL_URBAN'
  | 'PERMANENT_RURAL'
  | 'EVENTUAL_RURAL';
