/** Mirrors {@link io.github.alexistrejo11.pimienta.module.account.user.core.domain.enums.Gender}. */
export type Gender = 'MALE' | 'FEMALE' | 'OTHER';

/** Mirrors {@link io.github.alexistrejo11.pimienta.module.account.user.core.domain.enums.AccountStatus}. */
export type AccountStatus = 'PENDING_APPROVAL' | 'ACTIVE' | 'BANNED';

export enum AppRole {
  ADMIN = 'ADMIN',
  DIRECTOR = 'DIRECTOR',
  MANAGER = 'MANAGER',
  SALES = 'SALES',
  EMPLOYEE = 'EMPLOYEE',
  SUPPORT = 'SUPPORT',
  USER = 'USER',
}
