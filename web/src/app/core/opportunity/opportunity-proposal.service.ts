import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { API_BASE_URL } from '../config/api.config';
import type {
  ProposalResponse,
  UpsertProposalRequest,
} from '../model/opportunity/opportunity.dto';

/** /api/v1/opportunities/{id}/proposals */
@Injectable({ providedIn: 'root' })
export class OpportunityProposalService {
  private readonly http = inject(HttpClient);
  private readonly base = `${API_BASE_URL}/opportunities`;

  list(opportunityId: number): Observable<ProposalResponse[]> {
    return this.http.get<ProposalResponse[]>(`${this.base}/${opportunityId}/proposals`);
  }

  create(opportunityId: number, body: UpsertProposalRequest): Observable<ProposalResponse> {
    return this.http.post<ProposalResponse>(`${this.base}/${opportunityId}/proposals`, body);
  }

  update(
    opportunityId: number,
    proposalId: number,
    body: UpsertProposalRequest,
  ): Observable<ProposalResponse> {
    return this.http.put<ProposalResponse>(
      `${this.base}/${opportunityId}/proposals/${proposalId}`,
      body,
    );
  }

  delete(opportunityId: number, proposalId: number): Observable<void> {
    return this.http.delete<void>(`${this.base}/${opportunityId}/proposals/${proposalId}`);
  }
}
