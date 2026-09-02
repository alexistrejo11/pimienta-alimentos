import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { API_BASE_URL } from '../config/api.config';
import type { AttendanceResponse, AttendanceSearchParams } from '../model/employee/attendance.dto';
import type { PagedResponse } from '../model/common/pagination';

const BASE = `${API_BASE_URL}/employees`;

/** Llamadas globales de asistencia (no ligadas a un empleado específico). */
@Injectable({ providedIn: 'root' })
export class AttendanceService {
  private readonly http = inject(HttpClient);

  /**
   * Asistencias del día de hoy para todas las sedes (o una en particular).
   * GET /api/v1/employees/attendances/for-today
   */
  forToday(
    headquarterId?: number,
    page = 0,
    size = 50,
  ): Observable<PagedResponse<AttendanceResponse>> {
    let p = new HttpParams().set('page', page).set('size', size);
    if (headquarterId != null) p = p.set('headquarterId', headquarterId);
    return this.http.get<PagedResponse<AttendanceResponse>>(
      `${BASE}/attendances/for-today`,
      { params: p },
    );
  }

  /**
   * Búsqueda libre de asistencias con filtros opcionales.
   * GET /api/v1/employees/attendances/search
   */
  search(params: AttendanceSearchParams = {}): Observable<PagedResponse<AttendanceResponse>> {
    let p = new HttpParams()
      .set('page', params.page ?? 0)
      .set('size', params.size ?? 30);
    if (params.employeeId != null) p = p.set('employeeId', params.employeeId);
    if (params.headquarterId != null) p = p.set('headquarterId', params.headquarterId);
    if (params.workDate) p = p.set('workDate', params.workDate);
    if (params.workDateFrom) p = p.set('workDateFrom', params.workDateFrom);
    if (params.workDateTo) p = p.set('workDateTo', params.workDateTo);
    if (params.status) p = p.set('status', params.status);
    if (params.onlyOpen != null) p = p.set('onlyOpen', params.onlyOpen);
    return this.http.get<PagedResponse<AttendanceResponse>>(
      `${BASE}/attendances/search`,
      { params: p },
    );
  }

  /** Obtiene un registro de asistencia por ID. GET /api/v1/employees/attendances/:id */
  getById(attendanceId: number): Observable<AttendanceResponse> {
    return this.http.get<AttendanceResponse>(`${BASE}/attendances/${attendanceId}`);
  }
}
