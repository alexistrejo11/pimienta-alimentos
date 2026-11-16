/** Mirrors {@link io.github.alexistrejo11.pimienta.module.employees.core.domain.ContractType}. */
export type ContractType =
  | 'INDEFINITE'
  | 'FIXED_TERM'
  | 'PROJECT_BASED'
  | 'TEMPORARY'
  | 'FREELANCE'
  | 'UNDEFINED';

/** Mirrors {@link io.github.alexistrejo11.pimienta.module.employees.core.domain.WorkShift}. */
export type WorkShift =
  | 'MORNING'
  | 'AFTERNOON'
  | 'NIGHT'
  | 'MIXED'
  | 'REMOTE'
  | 'UNDEFINED';

/** Mirrors {@link io.github.alexistrejo11.pimienta.module.employees.core.domain.enums.EmployeeStatus}. */
export type EmployeeStatus =
  | 'ACTIVE'
  | 'INACTIVE'
  | 'RESIGNED'
  | 'TERMINATED'
  | 'DOWN';

/** Mirrors {@link io.github.alexistrejo11.pimienta.module.employees.core.domain.ImssSalaryType}. */
export type ImssSalaryType = 'FIXED' | 'VARIABLE' | 'MIXED';

/** Mirrors {@link io.github.alexistrejo11.pimienta.module.employees.core.domain.ImssWorkerType}. */
export type ImssWorkerType =
  | 'PERMANENT_URBAN'
  | 'EVENTUAL_URBAN'
  | 'PERMANENT_RURAL'
  | 'EVENTUAL_RURAL';
