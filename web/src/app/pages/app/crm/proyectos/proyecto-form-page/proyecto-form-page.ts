import { Component, inject, OnInit, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { finalize } from 'rxjs';

import { CrmService } from '../../../../../core/crm/crm.service';
import { fieldMessage, parseApiError, type ParsedApiError } from '../../../../../core/http/parse-api-error';
import type { ProjectPriority, ProjectType } from '../../../../../core/model/crm/crm.enums';
import type {
  CreateProjectRequest,
  ProjectResponse,
  UpdateProjectRequest,
} from '../../../../../core/model/crm/project.dto';
import { PageHeaderComponent } from '../../../../../shared/ui/page-header/page-header';

const TYPES: ProjectType[] = [
  'CONSULTING',
  'SOFTWARE_DEVELOPMENT',
  'IMPLEMENTATION',
  'MAINTENANCE',
  'TRAINING',
  'RESEARCH',
  'OTHER',
];

const PRIORITIES: ProjectPriority[] = ['LOW', 'MEDIUM', 'HIGH', 'CRITICAL'];

@Component({
  selector: 'app-proyecto-form-page',
  
  imports: [ReactiveFormsModule, RouterLink, PageHeaderComponent],
  templateUrl: './proyecto-form-page.html',
})
export class ProyectoFormPageComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly crm = inject(CrmService);

  readonly loading = signal(false);
  readonly loadingExisting = signal(false);
  readonly apiError = signal<ParsedApiError | null>(null);
  readonly projectId = signal<number | null>(null);

  readonly types = TYPES;
  readonly priorities = PRIORITIES;

  readonly typeLabel: Record<ProjectType, string> = {
    CONSULTING: 'Consultoría',
    SOFTWARE_DEVELOPMENT: 'Desarrollo de software',
    IMPLEMENTATION: 'Implementación',
    MAINTENANCE: 'Mantenimiento',
    TRAINING: 'Capacitación',
    RESEARCH: 'Investigación',
    OTHER: 'Otro',
  };

  readonly priorityLabel: Record<ProjectPriority, string> = {
    LOW: 'Baja',
    MEDIUM: 'Media',
    HIGH: 'Alta',
    CRITICAL: 'Crítica',
  };

  readonly form = this.fb.nonNullable.group({
    projectCode: ['', [Validators.required]],
    projectName: ['', [Validators.required]],
    description: [''],
    clientId: ['', [Validators.required, Validators.min(1)]],
    originOpportunityId: [''],
    type: this.fb.nonNullable.control<ProjectType>('CONSULTING', [Validators.required]),
    priority: this.fb.nonNullable.control<ProjectPriority>('MEDIUM', [Validators.required]),
    projectManagerId: [''],
    assignedSalesmanId: [''],
    plannedStartDate: [''],
    plannedEndDate: [''],
    contractedValue: ['', [Validators.required, Validators.min(0)]],
    estimatedCost: ['', [Validators.required, Validators.min(0)]],
    progressPercent: [''],
  });

  ngOnInit(): void {
    const isEditRoute = this.route.snapshot.url.some((u) => u.path === 'editar');
    const idRaw = isEditRoute ? this.route.snapshot.paramMap.get('id') : null;
    if (idRaw) {
      const id = Number(idRaw);
      if (!Number.isFinite(id) || id <= 0) {
        void this.router.navigateByUrl('/app/crm/proyectos');
        return;
      }
      this.projectId.set(id);
      this.form.controls.projectCode.disable({ emitEvent: false });
      this.loadProject(id);
    }
  }

  private loadProject(id: number): void {
    this.loadingExisting.set(true);
    this.apiError.set(null);
    this.crm.getProject(id).subscribe({
      next: (p: ProjectResponse) => {
        this.form.patchValue({
          projectCode: p.projectCode,
          projectName: p.projectName,
          description: p.description ?? '',
          clientId: String(p.clientId),
          originOpportunityId: p.originOpportunityId != null ? String(p.originOpportunityId) : '',
          type: p.type,
          priority: p.priority,
          projectManagerId: p.projectManagerId != null ? String(p.projectManagerId) : '',
          assignedSalesmanId: p.assignedSalesmanId != null ? String(p.assignedSalesmanId) : '',
          plannedStartDate: p.plannedStartDate ? p.plannedStartDate.slice(0, 10) : '',
          plannedEndDate: p.plannedEndDate ? p.plannedEndDate.slice(0, 10) : '',
          contractedValue: String(p.contractedValue),
          estimatedCost: String(p.estimatedCost),
          progressPercent: String(p.progressPercent ?? ''),
        });
        this.form.controls.clientId.disable({ emitEvent: false });
        this.form.controls.originOpportunityId.disable({ emitEvent: false });
        this.loadingExisting.set(false);
      },
      error: (err: unknown) => {
        this.apiError.set(parseApiError(err));
        this.loadingExisting.set(false);
      },
    });
  }

  get isEdit(): boolean {
    return this.projectId() != null;
  }

  private asText(value: unknown): string {
    if (value == null) return '';
    return String(value).trim();
  }

  submit(): void {
    this.apiError.set(null);
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const id = this.projectId();
    this.loading.set(true);
    if (id == null) {
      this.crm
        .createProject(this.buildCreateBody())
        .pipe(finalize(() => this.loading.set(false)))
        .subscribe({
          next: (created) => void this.router.navigateByUrl(`/app/crm/proyectos/${created.id}`),
          error: (err: unknown) => this.apiError.set(parseApiError(err)),
        });
    } else {
      this.crm
        .updateProject(id, this.buildUpdateBody())
        .pipe(finalize(() => this.loading.set(false)))
        .subscribe({
          next: () => void this.router.navigateByUrl(`/app/crm/proyectos/${id}`),
          error: (err: unknown) => this.apiError.set(parseApiError(err)),
        });
    }
  }

  private buildCreateBody(): CreateProjectRequest {
    const v = this.form.getRawValue();
    const clientId = Number(v.clientId);
    const body: CreateProjectRequest = {
      projectCode: this.asText(v.projectCode),
      projectName: this.asText(v.projectName),
      clientId,
      type: v.type,
      priority: v.priority,
      contractedValue: Number(v.contractedValue),
      estimatedCost: Number(v.estimatedCost),
    };
    const desc = this.asText(v.description);
    if (desc) body.description = desc;
    const oid = Number(v.originOpportunityId);
    if (this.asText(v.originOpportunityId) !== '' && !Number.isNaN(oid) && oid > 0) {
      body.originOpportunityId = oid;
    }
    const pm = Number(v.projectManagerId);
    if (this.asText(v.projectManagerId) !== '' && !Number.isNaN(pm) && pm > 0) {
      body.projectManagerId = pm;
    }
    const sales = Number(v.assignedSalesmanId);
    if (this.asText(v.assignedSalesmanId) !== '' && !Number.isNaN(sales) && sales > 0) {
      body.assignedSalesmanId = sales;
    }
    if (this.asText(v.plannedStartDate)) body.plannedStartDate = this.asText(v.plannedStartDate);
    if (this.asText(v.plannedEndDate)) body.plannedEndDate = this.asText(v.plannedEndDate);
    return body;
  }

  private buildUpdateBody(): UpdateProjectRequest {
    const v = this.form.getRawValue();
    const body: UpdateProjectRequest = {
      projectName: this.asText(v.projectName),
      description: this.asText(v.description) || null,
      type: v.type,
      priority: v.priority,
      contractedValue: Number(v.contractedValue),
      estimatedCost: Number(v.estimatedCost),
    };
    if (this.asText(v.plannedStartDate)) body.plannedStartDate = this.asText(v.plannedStartDate);
    else body.plannedStartDate = null;
    if (this.asText(v.plannedEndDate)) body.plannedEndDate = this.asText(v.plannedEndDate);
    else body.plannedEndDate = null;
    const pm = Number(v.projectManagerId);
    body.projectManagerId =
      this.asText(v.projectManagerId) === '' || Number.isNaN(pm) ? null : pm;
    const sales = Number(v.assignedSalesmanId);
    body.assignedSalesmanId =
      this.asText(v.assignedSalesmanId) === '' || Number.isNaN(sales) ? null : sales;
    const prog = Number(v.progressPercent);
    body.progressPercent =
      this.asText(v.progressPercent) === '' || Number.isNaN(prog)
        ? null
        : Math.min(100, Math.max(0, Math.round(prog)));
    return body;
  }

  apiFieldMessage(field: string): string | undefined {
    const p = this.apiError();
    if (!p) return undefined;
    return fieldMessage(p, field);
  }
}
