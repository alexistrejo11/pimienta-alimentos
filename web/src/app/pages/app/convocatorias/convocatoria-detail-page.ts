import { Component, inject, OnInit, signal } from '@angular/core';
import { FormBuilder, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { finalize } from 'rxjs';

import { FileService } from '../../../core/files/file.service';
import { parseApiError, type ParsedApiError } from '../../../core/http/parse-api-error';
import { formatDateTime } from '../../../core/i18n/format-date';
import {
  opportunityStatusLabel,
  organizationKindLabel,
  proposalKindLabel,
  requirementCategoryLabel,
  requirementStatusLabel,
} from '../../../core/i18n/enum-labels';
import type { FileAssetResponse } from '../../../core/model/files/file.dto';
import type {
  OpportunityResponse,
  OrganizationResponse,
  ProposalResponse,
  RequirementResponse,
} from '../../../core/model/opportunity/opportunity.dto';
import {
  PROPOSAL_KINDS,
  REQUIREMENT_CATEGORIES,
  REQUIREMENT_STATUSES,
  type OpportunityStatus,
  type ProposalKind,
  type RequirementCategory,
  type RequirementStatus,
} from '../../../core/model/opportunity/opportunity.enums';
import { OpportunityProposalService } from '../../../core/opportunity/opportunity-proposal.service';
import { OpportunityRequirementService } from '../../../core/opportunity/opportunity-requirement.service';
import { OpportunityService } from '../../../core/opportunity/opportunity.service';
import { OrganizationService } from '../../../core/opportunity/organization.service';
import { DataStateComponent } from '../../../shared/ui/data-state/data-state';
import { PageHeaderComponent } from '../../../shared/ui/page-header/page-header';

type LifecycleAction =
  | 'submitForReview'
  | 'startPreparing'
  | 'markSubmitted'
  | 'markWon'
  | 'markLost'
  | 'abandon'
  | 'cancel';

const FILE_MODULE = 'opportunity';
const FILE_ENTITY_TYPE_OPP = 'opportunity';
const FILE_ENTITY_TYPE_REQ = 'requirement';
const FILE_ENTITY_TYPE_PROP = 'proposal';
const MAX_FILE_BYTES = 50 * 1024 * 1024;

@Component({
  selector: 'app-convocatoria-detail-page',
  imports: [
    PageHeaderComponent,
    DataStateComponent,
    ReactiveFormsModule,
    FormsModule,
    RouterLink,
  ],
  templateUrl: './convocatoria-detail-page.html',
})
export class ConvocatoriaDetailPageComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly opportunities = inject(OpportunityService);
  private readonly organizations = inject(OrganizationService);
  private readonly requirementsApi = inject(OpportunityRequirementService);
  private readonly proposalsApi = inject(OpportunityProposalService);
  private readonly files = inject(FileService);
  private readonly fb = inject(FormBuilder);

  readonly loading = signal(true);
  readonly saving = signal(false);
  readonly error = signal<ParsedApiError | null>(null);
  readonly actionError = signal<string | null>(null);
  readonly item = signal<OpportunityResponse | null>(null);
  readonly organization = signal<OrganizationResponse | null>(null);

  readonly docsLoading = signal(false);
  readonly docsError = signal<string | null>(null);
  readonly documents = signal<FileAssetResponse[]>([]);
  readonly uploading = signal(false);
  readonly uploadError = signal<string | null>(null);
  readonly downloadingId = signal<string | null>(null);
  readonly deletingId = signal<string | null>(null);
  readonly selectedFile = signal<File | null>(null);
  readonly uploadDescription = signal('');

  readonly requirements = signal<RequirementResponse[]>([]);
  readonly reqLoading = signal(false);
  readonly reqError = signal<string | null>(null);
  readonly reqSaving = signal(false);
  readonly evidenceByReq = signal<Record<number, FileAssetResponse[]>>({});
  readonly evidenceUploadingReqId = signal<number | null>(null);
  readonly expandedEvidenceReqId = signal<number | null>(null);

  readonly newReqTitle = signal('');
  readonly newReqCategory = signal<RequirementCategory>('ADMINISTRATIVE');
  readonly newReqMandatory = signal(true);

  readonly proposals = signal<ProposalResponse[]>([]);
  readonly propLoading = signal(false);
  readonly propError = signal<string | null>(null);
  readonly propSaving = signal(false);
  readonly newPropKind = signal<ProposalKind>('COMBINED');
  readonly newPropNotes = signal('');
  readonly filesByProposal = signal<Record<number, FileAssetResponse[]>>({});
  readonly expandedProposalId = signal<number | null>(null);
  readonly proposalUploadingId = signal<number | null>(null);

  readonly opportunityStatusLabel = opportunityStatusLabel;
  readonly organizationKindLabel = organizationKindLabel;
  readonly requirementCategoryLabel = requirementCategoryLabel;
  readonly requirementStatusLabel = requirementStatusLabel;
  readonly proposalKindLabel = proposalKindLabel;
  readonly formatDateTime = formatDateTime;
  readonly categories = REQUIREMENT_CATEGORIES;
  readonly reqStatuses = REQUIREMENT_STATUSES;
  readonly proposalKinds = PROPOSAL_KINDS;

  readonly form = this.fb.nonNullable.group({
    organizationId: this.fb.nonNullable.control(0, Validators.required),
    title: ['', Validators.required],
    sourceUrl: [''],
    externalReference: [''],
    publishedOn: [''],
    submissionDeadline: [''],
    decisionOn: [''],
    notes: [''],
  });

  ngOnInit(): void {
    const id = Number(this.route.snapshot.paramMap.get('id'));
    if (!Number.isFinite(id) || id <= 0) {
      void this.router.navigateByUrl('/app/erp/convocatorias');
      return;
    }
    this.load(id);
  }

  reload(): void {
    const current = this.item();
    if (current) this.load(current.id);
  }

  canEditRequirements(status: OpportunityStatus): boolean {
    return (
      status === 'DRAFT' ||
      status === 'UNDER_REVIEW' ||
      status === 'PREPARING'
    );
  }

  isTerminal(status: OpportunityStatus): boolean {
    return status === 'WON' || status === 'LOST' || status === 'ABANDONED' || status === 'CANCELLED';
  }

  pendingMandatoryCount(): number {
    return this.requirements().filter(
      (r) => r.mandatory && r.status !== 'DONE' && r.status !== 'WAIVED',
    ).length;
  }

  nextActions(status: OpportunityStatus): { action: LifecycleAction; label: string }[] {
    switch (status) {
      case 'DRAFT':
        return [
          { action: 'submitForReview', label: 'Enviar a revisión' },
          { action: 'cancel', label: 'Cancelar' },
          { action: 'abandon', label: 'Abandonar' },
        ];
      case 'UNDER_REVIEW':
        return [
          { action: 'startPreparing', label: 'Iniciar preparación' },
          { action: 'cancel', label: 'Cancelar' },
          { action: 'abandon', label: 'Abandonar' },
        ];
      case 'PREPARING':
        return [
          { action: 'markSubmitted', label: 'Marcar enviada' },
          { action: 'cancel', label: 'Cancelar' },
          { action: 'abandon', label: 'Abandonar' },
        ];
      case 'SUBMITTED':
        return [
          { action: 'markWon', label: 'Ganada' },
          { action: 'markLost', label: 'Perdida' },
          { action: 'abandon', label: 'Abandonar' },
        ];
      default:
        return [];
    }
  }

  saveDetails(): void {
    const current = this.item();
    if (!current || this.form.invalid || this.saving() || this.isTerminal(current.status)) {
      this.form.markAllAsTouched();
      return;
    }
    const v = this.form.getRawValue();
    this.actionError.set(null);
    this.saving.set(true);
    this.opportunities
      .update(current.id, {
        organizationId: v.organizationId,
        title: v.title.trim(),
        sourceUrl: v.sourceUrl.trim() || null,
        externalReference: v.externalReference.trim() || null,
        publishedOn: v.publishedOn || null,
        submissionDeadline: v.submissionDeadline || null,
        decisionOn: v.decisionOn || null,
        notes: v.notes.trim() || null,
      })
      .pipe(finalize(() => this.saving.set(false)))
      .subscribe({
        next: (updated) => this.applyItem(updated),
        error: (err: unknown) => this.actionError.set(parseApiError(err).message),
      });
  }

  runAction(action: LifecycleAction): void {
    const current = this.item();
    if (!current || this.saving()) return;
    this.actionError.set(null);
    this.saving.set(true);
    const req$ = this.opportunities[action](current.id);
    req$.pipe(finalize(() => this.saving.set(false))).subscribe({
      next: (updated) => this.applyItem(updated),
      error: (err: unknown) => this.actionError.set(parseApiError(err).message),
    });
  }

  addRequirement(): void {
    const current = this.item();
    const title = this.newReqTitle().trim();
    if (!current || !title || this.reqSaving()) return;
    this.reqError.set(null);
    this.reqSaving.set(true);
    this.requirementsApi
      .create(current.id, {
        title,
        category: this.newReqCategory(),
        mandatory: this.newReqMandatory(),
      })
      .pipe(finalize(() => this.reqSaving.set(false)))
      .subscribe({
        next: () => {
          this.newReqTitle.set('');
          this.loadRequirements(current.id);
        },
        error: (err: unknown) => this.reqError.set(parseApiError(err).message),
      });
  }

  changeRequirementStatus(req: RequirementResponse, status: RequirementStatus): void {
    const current = this.item();
    if (!current || this.reqSaving()) return;
    this.reqError.set(null);
    this.reqSaving.set(true);
    this.requirementsApi
      .changeStatus(current.id, req.id, status)
      .pipe(finalize(() => this.reqSaving.set(false)))
      .subscribe({
        next: () => this.loadRequirements(current.id),
        error: (err: unknown) => this.reqError.set(parseApiError(err).message),
      });
  }

  deleteRequirement(req: RequirementResponse): void {
    const current = this.item();
    if (!current) return;
    if (!confirm(`¿Eliminar el requisito "${req.title}"?`)) return;
    this.reqError.set(null);
    this.reqSaving.set(true);
    this.requirementsApi
      .delete(current.id, req.id)
      .pipe(finalize(() => this.reqSaving.set(false)))
      .subscribe({
        next: () => this.loadRequirements(current.id),
        error: (err: unknown) => this.reqError.set(parseApiError(err).message),
      });
  }

  toggleEvidence(reqId: number): void {
    if (this.expandedEvidenceReqId() === reqId) {
      this.expandedEvidenceReqId.set(null);
      return;
    }
    this.expandedEvidenceReqId.set(reqId);
    this.loadEvidence(reqId);
  }

  onEvidencePicked(reqId: number, event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];
    if (!file) return;
    this.uploadEvidence(reqId, file);
    input.value = '';
  }

  onFilePicked(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0] ?? null;
    this.selectedFile.set(file);
    this.uploadError.set(null);
  }

  uploadDocument(): void {
    const current = this.item();
    const file = this.selectedFile();
    if (!current || !file || this.uploading()) return;

    if (file.size > MAX_FILE_BYTES) {
      this.uploadError.set('El archivo supera el límite de 50 MB.');
      return;
    }

    this.uploadError.set(null);
    this.uploading.set(true);
    this.files
      .uploadManagement(file, {
        category: 'RESOURCE',
        module: FILE_MODULE,
        entityType: FILE_ENTITY_TYPE_OPP,
        entityId: current.id,
        description: this.uploadDescription().trim() || undefined,
      })
      .pipe(finalize(() => this.uploading.set(false)))
      .subscribe({
        next: () => {
          this.selectedFile.set(null);
          this.uploadDescription.set('');
          this.loadDocuments(current.id);
        },
        error: (err: unknown) => this.uploadError.set(parseApiError(err).message),
      });
  }

  downloadDocument(id: string): void {
    this.downloadingId.set(id);
    this.files
      .getManagementDownloadUrl(id)
      .pipe(finalize(() => this.downloadingId.set(null)))
      .subscribe({
        next: (res) => window.open(res.url, '_blank', 'noopener,noreferrer'),
        error: (err: unknown) => this.docsError.set(parseApiError(err).message),
      });
  }

  deleteDocument(id: string): void {
    const doc = this.documents().find((f) => f.id === id);
    const name = doc?.originalName ?? 'este archivo';
    if (!confirm(`¿Eliminar "${name}"? Esta acción no se puede deshacer.`)) return;

    const current = this.item();
    this.deletingId.set(id);
    this.files
      .delete(id)
      .pipe(finalize(() => this.deletingId.set(null)))
      .subscribe({
        next: () => {
          if (current) this.loadDocuments(current.id);
        },
        error: (err: unknown) => this.docsError.set(parseApiError(err).message),
      });
  }

  evidenceFor(requirementId: number): FileAssetResponse[] {
    return this.evidenceByReq()[requirementId] ?? [];
  }

  proposalFiles(proposalId: number): FileAssetResponse[] {
    return this.filesByProposal()[proposalId] ?? [];
  }

  addProposal(): void {
    const current = this.item();
    if (!current || this.propSaving()) return;
    this.propError.set(null);
    this.propSaving.set(true);
    this.proposalsApi
      .create(current.id, {
        kind: this.newPropKind(),
        notes: this.newPropNotes().trim() || null,
      })
      .pipe(finalize(() => this.propSaving.set(false)))
      .subscribe({
        next: () => {
          this.newPropNotes.set('');
          this.newPropKind.set('COMBINED');
          this.loadProposals(current.id);
        },
        error: (err: unknown) => this.propError.set(parseApiError(err).message),
      });
  }

  deleteProposal(proposal: ProposalResponse): void {
    const current = this.item();
    if (!current) return;
    if (!confirm(`¿Eliminar la propuesta ${this.proposalKindLabel(proposal.kind)}?`)) return;
    this.propError.set(null);
    this.propSaving.set(true);
    this.proposalsApi
      .delete(current.id, proposal.id)
      .pipe(finalize(() => this.propSaving.set(false)))
      .subscribe({
        next: () => this.loadProposals(current.id),
        error: (err: unknown) => this.propError.set(parseApiError(err).message),
      });
  }

  toggleProposalFiles(proposalId: number): void {
    if (this.expandedProposalId() === proposalId) {
      this.expandedProposalId.set(null);
      return;
    }
    this.expandedProposalId.set(proposalId);
    this.loadProposalFiles(proposalId);
  }

  onProposalFilePicked(proposalId: number, event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];
    if (!file) return;
    this.uploadProposalFile(proposalId, file);
    input.value = '';
  }

  formatBytes(bytes: number): string {
    if (bytes < 1024) return `${bytes} B`;
    if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
    return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
  }

  private uploadEvidence(reqId: number, file: File): void {
    if (file.size > MAX_FILE_BYTES) {
      this.reqError.set('El archivo supera el límite de 50 MB.');
      return;
    }
    this.reqError.set(null);
    this.evidenceUploadingReqId.set(reqId);
    this.files
      .uploadManagement(file, {
        category: 'RESOURCE',
        module: FILE_MODULE,
        entityType: FILE_ENTITY_TYPE_REQ,
        entityId: reqId,
      })
      .pipe(finalize(() => this.evidenceUploadingReqId.set(null)))
      .subscribe({
        next: () => this.loadEvidence(reqId),
        error: (err: unknown) => this.reqError.set(parseApiError(err).message),
      });
  }

  private load(id: number): void {
    this.error.set(null);
    this.loading.set(true);
    this.opportunities
      .getById(id)
      .pipe(finalize(() => this.loading.set(false)))
      .subscribe({
        next: (opp) => {
          this.applyItem(opp);
          this.loadDocuments(opp.id);
          this.loadRequirements(opp.id);
          this.loadProposals(opp.id);
          this.organizations.getById(opp.organizationId).subscribe({
            next: (org) => this.organization.set(org),
            error: () => this.organization.set(null),
          });
        },
        error: (err: unknown) => this.error.set(parseApiError(err)),
      });
  }

  private loadRequirements(opportunityId: number): void {
    this.reqError.set(null);
    this.reqLoading.set(true);
    this.requirementsApi
      .list(opportunityId)
      .pipe(finalize(() => this.reqLoading.set(false)))
      .subscribe({
        next: (items) => this.requirements.set(items),
        error: (err: unknown) => this.reqError.set(parseApiError(err).message),
      });
  }

  private loadProposals(opportunityId: number): void {
    this.propError.set(null);
    this.propLoading.set(true);
    this.proposalsApi
      .list(opportunityId)
      .pipe(finalize(() => this.propLoading.set(false)))
      .subscribe({
        next: (items) => this.proposals.set(items),
        error: (err: unknown) => this.propError.set(parseApiError(err).message),
      });
  }

  private loadProposalFiles(proposalId: number): void {
    this.files
      .searchManagement({
        page: 0,
        size: 20,
        category: 'RESOURCE',
        module: FILE_MODULE,
        entityType: FILE_ENTITY_TYPE_PROP,
        entityId: proposalId,
      })
      .subscribe({
        next: (page) => {
          this.filesByProposal.update((map) => ({ ...map, [proposalId]: page.items }));
        },
        error: (err: unknown) => this.propError.set(parseApiError(err).message),
      });
  }

  private uploadProposalFile(proposalId: number, file: File): void {
    if (file.size > MAX_FILE_BYTES) {
      this.propError.set('El archivo supera el límite de 50 MB.');
      return;
    }
    this.propError.set(null);
    this.proposalUploadingId.set(proposalId);
    this.files
      .uploadManagement(file, {
        category: 'RESOURCE',
        module: FILE_MODULE,
        entityType: FILE_ENTITY_TYPE_PROP,
        entityId: proposalId,
      })
      .pipe(finalize(() => this.proposalUploadingId.set(null)))
      .subscribe({
        next: () => this.loadProposalFiles(proposalId),
        error: (err: unknown) => this.propError.set(parseApiError(err).message),
      });
  }

  private loadEvidence(requirementId: number): void {
    this.files
      .searchManagement({
        page: 0,
        size: 20,
        category: 'RESOURCE',
        module: FILE_MODULE,
        entityType: FILE_ENTITY_TYPE_REQ,
        entityId: requirementId,
      })
      .subscribe({
        next: (page) => {
          this.evidenceByReq.update((map) => ({ ...map, [requirementId]: page.items }));
        },
        error: (err: unknown) => this.reqError.set(parseApiError(err).message),
      });
  }

  private loadDocuments(opportunityId: number): void {
    this.docsError.set(null);
    this.docsLoading.set(true);
    this.files
      .searchManagement({
        page: 0,
        size: 50,
        category: 'RESOURCE',
        module: FILE_MODULE,
        entityType: FILE_ENTITY_TYPE_OPP,
        entityId: opportunityId,
      })
      .pipe(finalize(() => this.docsLoading.set(false)))
      .subscribe({
        next: (page) => this.documents.set(page.items),
        error: (err: unknown) => this.docsError.set(parseApiError(err).message),
      });
  }

  private applyItem(opp: OpportunityResponse): void {
    this.item.set(opp);
    this.form.setValue({
      organizationId: opp.organizationId,
      title: opp.title,
      sourceUrl: opp.sourceUrl ?? '',
      externalReference: opp.externalReference ?? '',
      publishedOn: opp.publishedOn ?? '',
      submissionDeadline: opp.submissionDeadline ?? '',
      decisionOn: opp.decisionOn ?? '',
      notes: opp.notes ?? '',
    });
  }
}
