import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { API_BASE_URL } from '../config/api.config';
import type {
  CreateRequirementRequest,
  RequirementResponse,
  UpdateRequirementRequest,
} from '../model/opportunity/opportunity.dto';
import type { RequirementStatus } from '../model/opportunity/opportunity.enums';

/** /api/v1/opportunities/{id}/requirements */
@Injectable({ providedIn: 'root' })
export class OpportunityRequirementService {
  private readonly http = inject(HttpClient);
  private readonly base = `${API_BASE_URL}/opportunities`;

  list(opportunityId: number): Observable<RequirementResponse[]> {
    return this.http.get<RequirementResponse[]>(`${this.base}/${opportunityId}/requirements`);
  }

  create(
    opportunityId: number,
    body: CreateRequirementRequest,
  ): Observable<RequirementResponse> {
    return this.http.post<RequirementResponse>(
      `${this.base}/${opportunityId}/requirements`,
      body,
    );
  }

  update(
    opportunityId: number,
    requirementId: number,
    body: UpdateRequirementRequest,
  ): Observable<RequirementResponse> {
    return this.http.put<RequirementResponse>(
      `${this.base}/${opportunityId}/requirements/${requirementId}`,
      body,
    );
  }

  changeStatus(
    opportunityId: number,
    requirementId: number,
    status: RequirementStatus,
  ): Observable<RequirementResponse> {
    return this.http.put<RequirementResponse>(
      `${this.base}/${opportunityId}/requirements/${requirementId}/status`,
      { status },
    );
  }

  delete(opportunityId: number, requirementId: number): Observable<void> {
    return this.http.delete<void>(
      `${this.base}/${opportunityId}/requirements/${requirementId}`,
    );
  }
}
