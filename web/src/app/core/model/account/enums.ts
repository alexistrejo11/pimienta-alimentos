/** Mirrors {@link io.github.alexistrejo11.pimienta.module.account.user.core.domain.enums.Gender}. */
export type Gender =
  | 'MALE'
  | 'FEMALE'
  | 'NON_BINARY'
  | 'OTHER'
  | 'PREFER_NOT_TO_SAY';

/** Mirrors {@link io.github.alexistrejo11.pimienta.module.account.user.core.domain.enums.AccountStatus}. */
export type AccountStatus = 'PENDING_APPROVAL' | 'ACTIVE' | 'BANNED';
