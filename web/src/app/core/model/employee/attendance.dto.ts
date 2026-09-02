/** Lifecycle status of a single attendance record. */
export type AttendanceStatus =
  | 'UNDEFINED'
  | 'CHECKED_IN'
  | 'CHECKED_OUT'
  | 'AUTO_CLOSED_EXCEEDED_MAX_SHIFT_HOURS'
  | 'AUTO_CLOSED_ASSUMED_CONTRACT_DAY';

/** POST /api/v1/employees/:id/attendance/start-workday — part "attendance". */
export interface StartWorkdayRequest {
  /** Headquarter where the employee is clocking in. */
  headquarterId: number;
  /** Optional. ISO date (yyyy-MM-dd); defaults to today. */
  workDate?: string;
}

/** POST /api/v1/employees/:id/attendance/end-workday — part "attendance". */
export interface EndWorkdayRequest {
  /** Optional. ISO date (yyyy-MM-dd); defaults to today. */
  workDate?: string;
}

/** Single attendance record returned by the API. */
export interface AttendanceResponse {
  id: number;
  employeeId: number;
  headquarterId: number;
  /** ISO date (yyyy-MM-dd). */
  workDate: string;
  /** ISO date-time. */
  checkInTime: string;
  /** ISO date-time or null while still checked in. */
  checkOutTime: string | null;
  status: AttendanceStatus;
  /** HTTPS presigned URL or empty string when no photo. */
  checkInEvidencePhotoUrl: string;
  /** HTTPS presigned URL or empty string when no photo. */
  checkOutEvidencePhotoUrl: string;
  /** Minutes between check-in and check-out; 0 when open. */
  minutesWorked: number;
}

/** Query params for GET /api/v1/employees/attendances/search */
export interface AttendanceSearchParams {
  employeeId?: number;
  headquarterId?: number;
  workDate?: string;
  workDateFrom?: string;
  workDateTo?: string;
  status?: AttendanceStatus;
  onlyOpen?: boolean;
  page?: number;
  size?: number;
}

/** Query params for GET /api/v1/employees/:id/attendances */
export interface EmployeeAttendanceListParams {
  workDateFrom?: string;
  workDateTo?: string;
  page?: number;
  size?: number;
}
