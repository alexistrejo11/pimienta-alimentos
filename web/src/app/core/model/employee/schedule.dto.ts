export type DayOfWeek =
  | 'MONDAY'
  | 'TUESDAY'
  | 'WEDNESDAY'
  | 'THURSDAY'
  | 'FRIDAY'
  | 'SATURDAY'
  | 'SUNDAY';

/** One weekly time window in a schedule. */
export interface WorkDayScheduleSlotResponse {
  dayOfWeek: DayOfWeek;
  /** "HH:mm" local time. */
  startTime: string;
  /** "HH:mm" local time. */
  endTime: string;
}

/** GET /api/v1/employees/:id/work-schedule */
export interface EmployeeWorkScheduleResponse {
  slots: WorkDayScheduleSlotResponse[];
}

/** One slot in the PUT body. */
export interface WorkDayScheduleSlotRequest {
  dayOfWeek: DayOfWeek;
  startTime: string;
  endTime: string;
}

/** PUT /api/v1/employees/:id/work-schedule */
export interface UpdateWorkScheduleRequest {
  slots: WorkDayScheduleSlotRequest[];
}
