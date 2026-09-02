import { Component, inject, OnInit, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { finalize } from 'rxjs';

import { CrmService } from '../../../../../core/crm/crm.service';
import { fieldMessage, parseApiError, type ParsedApiError } from '../../../../../core/http/parse-api-error';
import type { OpportunitySource } from '../../../../../core/model/crm/crm.enums';
import type {
  CreateOpportunityRequest,
  OpportunityResponse,
  UpdateOpportunityRequest,
} from '../../../../../core/model/crm/opportunity.dto';
import { PageHeaderComponent } from '../../../../../shared/ui/page-header/page-header';

const SOURCES: OpportunitySource[] = [
  'INBOUND',
  'OUTBOUND',
  'REFERRAL',
  'SOCIAL_MEDIA',
  'EVENT',
  'COLD_CALL',
  'OTHER',
];

@Component({
  selector: 'app-oportunidad-form-page',
  
  imports: [ReactiveFormsModule, RouterLink, PageHeaderComponent],
  templateUrl: './oportunidad-form-page.html',
})
export class OportunidadFormPageComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly crm = inject(CrmService);

  readonly loading = signal(false);
  readonly loadingExisting = signal(false);
  readonly apiError = signal<ParsedApiError | null>(null);
  readonly opportunityId = signal<number | null>(null);

  readonly sources = SOURCES;
  readonly sourceLabel: Record<OpportunitySource, string> = {
    INBOUND: 'Entrante',
    OUTBOUND: 'Saliente',
    REFERRAL: 'Referido',
    SOCIAL_MEDIA: 'Redes sociales',
    EVENT: 'Evento',
    COLD_CALL: 'Llamada en frío',
    OTHER: 'Otro',
  };

  readonly form = this.fb.nonNullable.group({
    title: ['', [Validators.required, Validators.maxLength(500)]],
    description: [''],
    contactName: ['', [Validators.required]],
    contactEmail: ['', [Validators.required, Validators.email]],
    contactPhone: [''],
    companyName: ['', [Validators.required]],
    companyLocation: [''],
    industry: [''],
    estimatedValue: ['', [Validators.required, Validators.min(0)]],
    probabilityPercent: ['25'],
    source: this.fb.nonNullable.control<OpportunitySource>('INBOUND', [Validators.required]),
    expectedCloseDate: [''],
    assignedSalesmanId: [''],
  });

  ngOnInit(): void {
    const isEditRoute = this.route.snapshot.url.some((u) => u.path === 'editar');
    const idRaw = isEditRoute ? this.route.snapshot.paramMap.get('id') : null;
    if (idRaw) {
      const id = Number(idRaw);
      if (!Number.isFinite(id) || id <= 0) {
        void this.router.navigateByUrl('/app/crm/oportunidades');
        return;
      }
      this.opportunityId.set(id);
      this.loadOpportunity(id);
    }
  }

  private loadOpportunity(id: number): void {
    this.loadingExisting.set(true);
    this.apiError.set(null);
    this.crm.getOpportunity(id).subscribe({
      next: (op: OpportunityResponse) => {
        this.patchFromOpportunity(op);
        this.loadingExisting.set(false);
      },
      error: (err: unknown) => {
        this.apiError.set(parseApiError(err));
        this.loadingExisting.set(false);
      },
    });
  }

  private patchFromOpportunity(op: OpportunityResponse): void {
    this.form.patchValue({
      title: op.title,
      description: op.description ?? '',
      contactName: op.contactName,
      contactEmail: op.contactEmail,
      contactPhone: op.contactPhone ?? '',
      companyName: op.companyName,
      companyLocation: op.companyLocation ?? '',
      industry: op.industry ?? '',
      estimatedValue: String(op.estimatedValue),
      probabilityPercent: String(op.probabilityPercent ?? ''),
      source: op.source,
      expectedCloseDate: op.expectedCloseDate ? op.expectedCloseDate.slice(0, 10) : '',
      assignedSalesmanId: op.assignedSalesmanId != null ? String(op.assignedSalesmanId) : '',
    });
  }

  get isEdit(): boolean {
    return this.opportunityId() != null;
  }

  submit(): void {
    this.apiError.set(null);
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const id = this.opportunityId();
    this.loading.set(true);
    if (id == null) {
      this.crm
        .createOpportunity(this.buildCreateBody())
        .pipe(finalize(() => this.loading.set(false)))
        .subscribe({
          next: (created) => void this.router.navigateByUrl(`/app/crm/oportunidades/${created.id}`),
          error: (err: unknown) => this.apiError.set(parseApiError(err)),
        });
    } else {
      this.crm
        .updateOpportunity(id, this.buildUpdateBody())
        .pipe(finalize(() => this.loading.set(false)))
        .subscribe({
          next: () => void this.router.navigateByUrl(`/app/crm/oportunidades/${id}`),
          error: (err: unknown) => this.apiError.set(parseApiError(err)),
        });
    }
  }

  private buildCreateBody(): CreateOpportunityRequest {
    const v = this.form.getRawValue();
    const body: CreateOpportunityRequest = {
      title: v.title.trim(),
      contactName: v.contactName.trim(),
      contactEmail: v.contactEmail.trim(),
      companyName: v.companyName.trim(),
      estimatedValue: Number(v.estimatedValue),
      source: v.source,
    };
    const desc = v.description.trim();
    if (desc) body.description = desc;
    const phone = v.contactPhone.trim();
    if (phone) body.contactPhone = phone;
    const loc = v.companyLocation.trim();
    if (loc) body.companyLocation = loc;
    const ind = v.industry.trim();
    if (ind) body.industry = ind;
    const close = v.expectedCloseDate.trim();
    if (close) body.expectedCloseDate = close;
    const prob = Number(v.probabilityPercent);
    if (v.probabilityPercent.trim() !== '' && !Number.isNaN(prob)) {
      body.probabilityPercent = Math.min(100, Math.max(0, Math.round(prob)));
    }
    const sid = Number(v.assignedSalesmanId);
    if (v.assignedSalesmanId.trim() !== '' && !Number.isNaN(sid) && sid > 0) {
      body.assignedSalesmanId = sid;
    }
    return body;
  }

  private buildUpdateBody(): UpdateOpportunityRequest {
    const v = this.form.getRawValue();
    const body: UpdateOpportunityRequest = {
      title: v.title.trim(),
      contactName: v.contactName.trim(),
      contactEmail: v.contactEmail.trim(),
      companyName: v.companyName.trim(),
      estimatedValue: Number(v.estimatedValue),
      source: v.source,
      probabilityPercent:
        v.probabilityPercent.trim() === ''
          ? null
          : Math.min(100, Math.max(0, Math.round(Number(v.probabilityPercent)))),
    };
    body.description = v.description.trim() || null;
    body.contactPhone = v.contactPhone.trim() || null;
    body.companyLocation = v.companyLocation.trim() || null;
    body.industry = v.industry.trim() || null;
    body.expectedCloseDate = v.expectedCloseDate.trim() || null;
    const sid = Number(v.assignedSalesmanId);
    body.assignedSalesmanId =
      v.assignedSalesmanId.trim() === '' || Number.isNaN(sid) ? null : sid;
    return body;
  }

  apiFieldMessage(field: string): string | undefined {
    const p = this.apiError();
    if (!p) return undefined;
    return fieldMessage(p, field);
  }
}
