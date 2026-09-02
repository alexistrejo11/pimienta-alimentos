/** Mirrors nested enums on {@link io.github.alexistrejo11.pimienta.module.task.core.domain.Task}. */
export type TaskStatus =
  | 'PENDING'
  | 'IN_PROGRESS'
  | 'COMPLETED'
  | 'CANCELLED'
  | 'DELAYED'
  | 'ON_HOLD'
  | 'FAILED'
  | 'UNDEFINED';

export type TaskPriority = 'LOW' | 'MEDIUM' | 'HIGH' | 'URGENT' | 'UNDEFINED';
