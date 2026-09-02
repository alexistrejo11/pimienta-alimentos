import { Component, inject, OnInit, signal } from '@angular/core';
import { FormBuilder, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { finalize } from 'rxjs';

import { ContractService } from '../../../core/contracts/contract.service';
import { CrmService } from '../../../core/crm/crm.service';
import { EmployeeService } from '../../../core/employees/employee.service';
import { parseApiError, type ParsedApiError } from '../../../core/http/parse-api-error';
import type { ContractCategory, ContractTermKind } from '../../../core/model/contract/contract.enums';
import type { ContractResponse, CreateOrUpdateContractRequest } from '../../../core/model/contract/contract.dto';
import type { EmployeeListItemResponse } from '../../../core/model/employee/employee.dto';
import type { OpportunityResponse } from '../../../core/model/crm/opportunity.dto';
import type { ProjectResponse } from '../../../core/model/crm/project.dto';
import { DataStateComponent } from '../../../shared/ui/data-state/data-state';
import { PageHeaderComponent } from '../../../shared/ui/page-header/page-header';

/**
 * Contratos comerciales y laborales: listado y alta enlazando empleados, oportunidades o proyectos.
 */
@Component({
  selector: 'app-contratos-page',
  
  imports: [PageHeaderComponent, DataStateComponent, ReactiveFormsModule, FormsModule],
  templateUrl: './contratos-page.html',
})
export class ContratosPageComponent implements OnInit {
  private readonly contracts = inject(ContractService);
  private readonly employees = inject(EmployeeService);
  private readonly crm = inject(CrmService);
  private readonly fb = inject(FormBuilder);

  readonly loading = signal(true);
  readonly saving = signal(false);
  readonly error = signal<ParsedApiError | null>(null);
  readonly formError = signal<string | null>(null);
  readonly items = signal<ContractResponse[]>([]);
  readonly showCreate = signal(false);

  readonly employeeOptions = signal<EmployeeListItemResponse[]>([]);
  readonly opportunityOptions = signal<OpportunityResponse[]>([]);
  readonly projectOptions = signal<ProjectResponse[]>([]);

  readonly form = this.fb.nonNullable.group({
    name: ['', Validators.required],
    description: [''],
    category: this.fb.nonNullable.control<ContractCategory>('SUPPLIER'),
    employeeId: this.fb.control<number | null>(null),
    opportunityId: this.fb.control<number | null>(null),
    projectId: this.fb.control<number | null>(null),
    termKind: this.fb.nonNullable.control<ContractTermKind>('INDEFINITE'),
    effectiveStart: ['', Validators.required],
    effectiveEnd: [''],
    documentUrl: ['', Validators.required],
    termsAndConditions: [''],
    referenceCode: [''],
    renewalCycleMonths: this.fb.control<number | null>(null),
    agreedValue: this.fb.control<number | null>(null),
    currencyCode: [''],
  });

  ngOnInit(): void {
    this.loadRelationOptions();
    this.reloadList();
  }

  toggleCreate(): void {
    this.showCreate.update((v) => !v);
    this.formError.set(null);
  }

  reloadList(): void {
    this.error.set(null);
    this.loading.set(true);
    this.contracts
      .list(0, 50)
      .pipe(finalize(() => this.loading.set(false)))
      .subscribe({
        next: (page) => this.items.set(page.items),
        error: (err: unknown) => this.error.set(parseApiError(err)),
      });
  }

  private loadRelationOptions(): void {
    this.employees.list(0, 100).subscribe({
      next: (p) => this.employeeOptions.set(p.items),
      error: () => this.employeeOptions.set([]),
    });
    this.crm.listOpportunities({ page: 0, size: 50 }).subscribe({
      next: (p) => this.opportunityOptions.set(p.items),
      error: () => this.opportunityOptions.set([]),
    });
    this.crm.listProjects({ page: 0, size: 50 }).subscribe({
      next: (p) => this.projectOptions.set(p.items),
      error: () => this.projectOptions.set([]),
    });
  }

  submitCreate(): void {
    this.formError.set(null);
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      this.formError.set('Revisa los campos obligatorios.');
      return;
    }
    const v = this.form.getRawValue();
    if (v.category === 'EMPLOYEE' && (v.employeeId == null || Number.isNaN(v.employeeId))) {
      this.formError.set('Los contratos de tipo empleado deben vincular a un colaborador.');
      return;
    }
    if (v.termKind === 'FIXED_TERM' && !v.effectiveEnd?.trim()) {
      this.formError.set('Los contratos a plazo fijo requieren fecha de fin.');
      return;
    }

    const currency = v.currencyCode?.trim().toUpperCase() || null;
    const agreed = v.agreedValue != null && !Number.isNaN(v.agreedValue) ? v.agreedValue : null;

    const body: CreateOrUpdateContractRequest = {
      name: v.name.trim(),
      description: v.description.trim() || null,
      category: v.category,
      employeeId: v.category === 'EMPLOYEE' ? v.employeeId : null,
      opportunityId: v.opportunityId ?? null,
      projectId: v.projectId ?? null,
      termKind: v.termKind,
      effectiveStart: v.effectiveStart,
      effectiveEnd: v.termKind === 'FIXED_TERM' ? v.effectiveEnd.trim() : null,
      documentUrl: v.documentUrl.trim(),
      termsAndConditions: v.termsAndConditions.trim() || null,
      referenceCode: v.referenceCode.trim() || null,
      renewalCycleMonths: v.renewalCycleMonths ?? null,
      agreedValue: agreed,
      currencyCode: agreed != null ? currency : null,
    };

    if (body.agreedValue != null && (!body.currencyCode || body.currencyCode.length !== 3)) {
      this.formError.set('Si indicas un monto, usa un código de moneda ISO de 3 letras (p. ej. MXN).');
      return;
    }

    this.saving.set(true);
    this.contracts
      .create(body)
      .pipe(finalize(() => this.saving.set(false)))
      .subscribe({
        next: () => {
          this.form.reset({
            name: '',
            description: '',
            category: 'SUPPLIER',
            employeeId: null,
            opportunityId: null,
            projectId: null,
            termKind: 'INDEFINITE',
            effectiveStart: '',
            effectiveEnd: '',
            documentUrl: '',
            termsAndConditions: '',
            referenceCode: '',
            renewalCycleMonths: null,
            agreedValue: null,
            currencyCode: '',
          });
          this.showCreate.set(false);
          this.reloadList();
        },
        error: (err: unknown) => this.formError.set(parseApiError(err).message),
      });
  }

  opportunityLabel(o: OpportunityResponse): string {
    return `#${o.id} · ${o.title} (${o.companyName})`;
  }

  projectLabel(p: ProjectResponse): string {
    return `#${p.id} · ${p.projectCode} — ${p.projectName}`;
  }
}
