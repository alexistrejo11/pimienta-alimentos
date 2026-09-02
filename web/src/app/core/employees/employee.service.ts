import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { API_BASE_URL } from '../config/api.config';
import type {
  EmployeeListItemResponse,
  EmployeeResponse,
  EmployeeSearchParams,
  EmployeeStatisticsResponse,
  EmployeeSummaryResponse,
  RegisterEmployeeRequest,
  UpdateEmployeeRequest,
} from '../model/employee/employee.dto';
import type { PagedResponse } from '../model/common/pagination';
import type {
  AttendanceResponse,
  AttendanceSearchParams,
  EmployeeAttendanceListParams,
  EndWorkdayRequest,
  StartWorkdayRequest,
} from '../model/employee/attendance.dto';
import type {
  EmployeeWorkScheduleResponse,
  UpdateWorkScheduleRequest,
} from '../model/employee/schedule.dto';

/** Llamadas al módulo de empleados: /api/v1/employees */
@Injectable({ providedIn: 'root' })
export class EmployeeService {
  private readonly http = inject(HttpClient);
  private readonly base = `${API_BASE_URL}/employees`;

  // ── Listado y detalle ─────────────────────────────────────────────────────

  /** Lista paginada de empleados con campos resumidos. */
  list(page = 0, size = 20): Observable<PagedResponse<EmployeeListItemResponse>> {
    const params = new HttpParams().set('page', page).set('size', size);
    return this.http.get<PagedResponse<EmployeeListItemResponse>>(this.base, { params });
  }

  /** Detalle completo de un empleado por su ID. */
  getById(id: number): Observable<EmployeeResponse> {
    return this.http.get<EmployeeResponse>(`${this.base}/${id}`);
  }

  /** Estadísticas globales: total, activos, inactivos. */
  statistics(): Observable<EmployeeStatisticsResponse> {
    return this.http.get<EmployeeStatisticsResponse>(`${this.base}/statistics`);
  }

  /** Resumen por departamento: headcount desglosado. */
  summary(): Observable<EmployeeSummaryResponse> {
    return this.http.get<EmployeeSummaryResponse>(`${this.base}/summary`);
  }

  /** Descarga Excel (GET /api/v1/employees/export). */
  export(search: EmployeeSearchParams = {}): Observable<Blob> {
    let params = new HttpParams()
      .set('page', String(search.page ?? 0))
      .set('size', String(search.size ?? 100));
    if (search.status) params = params.set('status', search.status);
    if (search.department) params = params.set('department', search.department);
    if (search.q) params = params.set('q', search.q);
    return this.http.get(`${this.base}/export`, { params, responseType: 'blob' });
  }

  // ── Registro y actualización (multipart/form-data) ────────────────────────

  /**
   * Registra un nuevo empleado.
   * El cuerpo se envía como multipart: parte "employee" (JSON) y parte "photo" (opcional).
   */
  register(data: RegisterEmployeeRequest, photo?: File): Observable<EmployeeResponse> {
    const form = new FormData();
    form.append('employee', new Blob([JSON.stringify(data)], { type: 'application/json' }));
    if (photo) {
      form.append('photo', photo);
    }
    return this.http.post<EmployeeResponse>(this.base, form);
  }

  /**
   * Actualiza los datos de un empleado.
   * El cuerpo se envía como multipart: parte "employee" (JSON) y parte "photo" (opcional).
   */
  update(id: number, data: UpdateEmployeeRequest, photo?: File): Observable<EmployeeResponse> {
    const form = new FormData();
    form.append('employee', new Blob([JSON.stringify(data)], { type: 'application/json' }));
    if (photo) {
      form.append('photo', photo);
    }
    return this.http.put<EmployeeResponse>(`${this.base}/${id}`, form);
  }

  // ── Asistencia (por empleado) ──────────────────────────────────────────────

  /**
   * Historial de asistencia de un empleado con filtros opcionales de rango de fechas.
   * GET /api/v1/employees/:id/attendances
   */
  listAttendances(
    employeeId: number,
    params: EmployeeAttendanceListParams = {},
  ): Observable<PagedResponse<AttendanceResponse>> {
    let p = new HttpParams()
      .set('page', params.page ?? 0)
      .set('size', params.size ?? 20);
    if (params.workDateFrom) p = p.set('workDateFrom', params.workDateFrom);
    if (params.workDateTo) p = p.set('workDateTo', params.workDateTo);
    return this.http.get<PagedResponse<AttendanceResponse>>(
      `${this.base}/${employeeId}/attendances`,
      { params: p },
    );
  }

  /**
   * Registra entrada (check-in) de un empleado.
   * POST /api/v1/employees/:id/attendance/start-workday
   * Multipart: parte "attendance" (JSON) y parte "checkInEvidencePhoto" (opcional).
   */
  startWorkday(
    employeeId: number,
    data: StartWorkdayRequest,
    photo?: File,
  ): Observable<AttendanceResponse> {
    const form = new FormData();
    form.append('attendance', new Blob([JSON.stringify(data)], { type: 'application/json' }));
    if (photo) {
      form.append('checkInEvidencePhoto', photo);
    }
    return this.http.post<AttendanceResponse>(
      `${this.base}/${employeeId}/attendance/start-workday`,
      form,
    );
  }

  /**
   * Registra salida (check-out) de un empleado.
   * POST /api/v1/employees/:id/attendance/end-workday
   * Multipart: parte "attendance" (JSON) y parte "checkOutEvidencePhoto" (opcional).
   */
  endWorkday(
    employeeId: number,
    data: EndWorkdayRequest,
    photo?: File,
  ): Observable<AttendanceResponse> {
    const form = new FormData();
    form.append('attendance', new Blob([JSON.stringify(data)], { type: 'application/json' }));
    if (photo) {
      form.append('checkOutEvidencePhoto', photo);
    }
    return this.http.post<AttendanceResponse>(
      `${this.base}/${employeeId}/attendance/end-workday`,
      form,
    );
  }

  // ── Horario semanal ───────────────────────────────────────────────────────

  /** Obtiene el horario semanal de un empleado. GET /api/v1/employees/:id/work-schedule */
  getWorkSchedule(employeeId: number): Observable<EmployeeWorkScheduleResponse> {
    return this.http.get<EmployeeWorkScheduleResponse>(
      `${this.base}/${employeeId}/work-schedule`,
    );
  }

  /** Reemplaza el horario semanal completo. PUT /api/v1/employees/:id/work-schedule */
  updateWorkSchedule(
    employeeId: number,
    data: UpdateWorkScheduleRequest,
  ): Observable<EmployeeWorkScheduleResponse> {
    return this.http.put<EmployeeWorkScheduleResponse>(
      `${this.base}/${employeeId}/work-schedule`,
      data,
    );
  }
}
