import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';

import { API_BASE_URL } from '../config/api.config';
import type {
  CreateOpportunityRequest,
  OpportunityResponse,
  OpportunitySummaryResponse,
  OpportunitySearchParams,
  UpdateOpportunityRequest,
} from '../model/crm/opportunity.dto';
import type {
  CreateProjectRequest,
  ProjectResponse,
  ProjectSummaryResponse,
  ProjectSearchParams,
  UpdateProjectRequest,
} from '../model/crm/project.dto';
import type { ProjectMilestoneResponse } from '../model/crm/project-milestone.dto';
import type { PagedResponse } from '../model/common/pagination';

/** Llamadas al módulo CRM: oportunidades y proyectos. */
@Injectable({ providedIn: 'root' })
export class CrmService {
  private readonly http = inject(HttpClient);
  private readonly opportunitiesBase = `${API_BASE_URL}/opportunities`;
  private readonly projectsBase = `${API_BASE_URL}/projects`;

  // ── Oportunidades ──────────────────────────────────────────────────────────

  /** Lista paginada de oportunidades con filtros opcionales. */
  listOpportunities(
    params: OpportunitySearchParams = {},
  ): Observable<PagedResponse<OpportunityResponse>> {
    let httpParams = new HttpParams().set('page', params.page ?? 0).set('size', params.size ?? 20);

    if (params.status) httpParams = httpParams.set('status', params.status);

    if (params.companyNameContains)
      httpParams = httpParams.set('companyNameContains', params.companyNameContains);

    if (params.titleContains) httpParams = httpParams.set('titleContains', params.titleContains);
    return this.http.get<PagedResponse<OpportunityResponse>>(this.opportunitiesBase, {
      params: httpParams,
    });
  }

  /** Detalle completo de una oportunidad. */
  getOpportunity(id: number): Observable<OpportunityResponse> {
    return this.http.get<OpportunityResponse>(`${this.opportunitiesBase}/${id}`);
  }

  /** POST /api/v1/opportunities */
  createOpportunity(body: CreateOpportunityRequest): Observable<OpportunityResponse> {
    return this.http.post<OpportunityResponse>(this.opportunitiesBase, body);
  }

  /** PATCH /api/v1/opportunities/:id */
  updateOpportunity(id: number, body: UpdateOpportunityRequest): Observable<OpportunityResponse> {
    return this.http.patch<OpportunityResponse>(`${this.opportunitiesBase}/${id}`, body);
  }

  /** DELETE /api/v1/opportunities/:id — **204** sin cuerpo. */
  deleteOpportunity(id: number): Observable<void> {
    return this.http.delete(`${this.opportunitiesBase}/${id}`, { observe: 'response' }).pipe(
      map(() => undefined),
    );
  }

  /** Resumen con conteos de tareas y valor ponderado. */
  getOpportunitySummary(id: number): Observable<OpportunitySummaryResponse> {
    return this.http.get<OpportunitySummaryResponse>(`${this.opportunitiesBase}/${id}/summary`);
  }

  // ── Proyectos ──────────────────────────────────────────────────────────────

  /** Lista paginada de proyectos con filtros opcionales. */
  listProjects(params: ProjectSearchParams = {}): Observable<PagedResponse<ProjectResponse>> {
    let httpParams = new HttpParams().set('page', params.page ?? 0).set('size', params.size ?? 20);
    if (params.status) httpParams = httpParams.set('status', params.status);
    if (params.clientId) httpParams = httpParams.set('clientId', params.clientId);
    if (params.projectManagerId)
      httpParams = httpParams.set('projectManagerId', params.projectManagerId);
    return this.http.get<PagedResponse<ProjectResponse>>(this.projectsBase, {
      params: httpParams,
    });
  }

  /** Detalle completo de un proyecto. */
  getProject(id: number): Observable<ProjectResponse> {
    return this.http.get<ProjectResponse>(`${this.projectsBase}/${id}`);
  }

  /** POST /api/v1/projects */
  createProject(body: CreateProjectRequest): Observable<ProjectResponse> {
    return this.http.post<ProjectResponse>(this.projectsBase, body);
  }

  /** PATCH /api/v1/projects/:id */
  updateProject(id: number, body: UpdateProjectRequest): Observable<ProjectResponse> {
    return this.http.patch<ProjectResponse>(`${this.projectsBase}/${id}`, body);
  }

  /** DELETE /api/v1/projects/:id — **204** sin cuerpo. */
  deleteProject(id: number): Observable<void> {
    return this.http.delete(`${this.projectsBase}/${id}`, { observe: 'response' }).pipe(
      map(() => undefined),
    );
  }

  /** Resumen con conteo de hitos y tareas. */
  getProjectSummary(id: number): Observable<ProjectSummaryResponse> {
    return this.http.get<ProjectSummaryResponse>(`${this.projectsBase}/${id}/summary`);
  }

  /** Hitos de un proyecto en orden de sortOrder (API: {@code PagedResponse}). */
  listMilestones(projectId: number): Observable<ProjectMilestoneResponse[]> {
    return this.http
      .get<PagedResponse<ProjectMilestoneResponse>>(
        `${this.projectsBase}/${projectId}/milestones`,
      )
      .pipe(map((page) => page.items));
  }
}
