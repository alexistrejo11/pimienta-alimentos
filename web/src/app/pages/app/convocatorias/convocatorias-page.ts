import { Component, inject, OnInit, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { finalize } from 'rxjs';

import { parseApiError, type ParsedApiError } from '../../../core/http/parse-api-error';
import {
  opportunityStatusLabel,
  organizationKindLabel,
} from '../../../core/i18n/enum-labels';
import type { OpportunityResponse } from '../../../core/model/opportunity/opportunity.dto';
import type { OrganizationResponse } from '../../../core/model/opportunity/opportunity.dto';
import {
  ORGANIZATION_KINDS,
  OPPORTUNITY_STATUSES,
  type OpportunityStatus,
  type OrganizationKind,
} from '../../../core/model/opportunity/opportunity.enums';
import { OpportunityService } from '../../../core/opportunity/opportunity.service';
import { OrganizationService } from '../../../core/opportunity/organization.service';
import { DataStateComponent } from '../../../shared/ui/data-state/data-state';
import { ListSearchFieldComponent } from '../../../shared/ui/list-search-field/list-search-field';
import { PageHeaderComponent } from '../../../shared/ui/page-header/page-header';

@Component({
  selector: 'app-convocatorias-page',
  imports: [
    PageHeaderComponent,
    DataStateComponent,
    ListSearchFieldComponent,
    ReactiveFormsModule,
    RouterLink,
  ],
  templateUrl: './convocatorias-page.html',
})
export class ConvocatoriasPageComponent implements OnInit {
  private readonly opportunities = inject(OpportunityService);
  private readonly organizations = inject(OrganizationService);
  private readonly fb = inject(FormBuilder);

  readonly loading = signal(true);
  readonly saving = signal(false);
  readonly error = signal<ParsedApiError | null>(null);
  readonly formError = signal<string | null>(null);
  readonly items = signal<OpportunityResponse[]>([]);
  readonly orgOptions = signal<OrganizationResponse[]>([]);
  readonly showCreate = signal(false);
  readonly searchDraft = signal('');
  readonly appliedSearch = signal('');
  readonly statusFilter = signal<OpportunityStatus | ''>('');

  readonly opportunityStatusLabel = opportunityStatusLabel;
  readonly organizationKindLabel = organizationKindLabel;
  readonly statuses = OPPORTUNITY_STATUSES;
  readonly kinds = ORGANIZATION_KINDS;

  readonly form = this.fb.nonNullable.group({
    organizationId: this.fb.control<number | null>(null),
    title: ['', Validators.required],
    sourceUrl: [''],
    externalReference: [''],
    publishedOn: [''],
    submissionDeadline: [''],
    notes: [''],
    newOrgName: [''],
    newOrgKind: this.fb.nonNullable.control<OrganizationKind>('PUBLIC_INSTITUTION'),
  });

  ngOnInit(): void {
    this.loadOrganizations();
    this.reloadList();
  }

  orgName(id: number): string {
    return this.orgOptions().find((o) => o.id === id)?.name ?? `Org #${id}`;
  }

  onSearch(term: string): void {
    this.appliedSearch.set(term);
    this.reloadList();
  }

  onStatusChange(raw: string): void {
    this.statusFilter.set((raw || '') as OpportunityStatus | '');
    this.reloadList();
  }

  toggleCreate(): void {
    if (this.showCreate()) this.cancelCreate();
    else this.openCreate();
  }

  openCreate(): void {
    this.form.reset({
      organizationId: null,
      title: '',
      sourceUrl: '',
      externalReference: '',
      publishedOn: '',
      submissionDeadline: '',
      notes: '',
      newOrgName: '',
      newOrgKind: 'PUBLIC_INSTITUTION',
    });
    this.formError.set(null);
    this.showCreate.set(true);
  }

  cancelCreate(): void {
    if (this.saving()) return;
    this.showCreate.set(false);
    this.formError.set(null);
  }

  reloadList(): void {
    this.error.set(null);
    this.loading.set(true);
    this.opportunities
      .list({
        page: 0,
        size: 50,
        search: this.appliedSearch(),
        status: this.statusFilter(),
      })
      .pipe(finalize(() => this.loading.set(false)))
      .subscribe({
        next: (page) => this.items.set(page.items),
        error: (err: unknown) => this.error.set(parseApiError(err)),
      });
  }

  submitCreate(): void {
    if (this.form.invalid || this.saving()) {
      this.form.markAllAsTouched();
      return;
    }
    this.formError.set(null);
    this.saving.set(true);

    const v = this.form.getRawValue();
    const createOpp = (organizationId: number) => {
      this.opportunities
        .create({
          organizationId,
          title: v.title.trim(),
          sourceUrl: v.sourceUrl.trim() || null,
          externalReference: v.externalReference.trim() || null,
          publishedOn: v.publishedOn || null,
          submissionDeadline: v.submissionDeadline || null,
          notes: v.notes.trim() || null,
        })
        .pipe(finalize(() => this.saving.set(false)))
        .subscribe({
          next: () => {
            this.showCreate.set(false);
            this.reloadList();
          },
          error: (err: unknown) => {
            this.formError.set(parseApiError(err).message);
          },
        });
    };

    if (v.organizationId != null) {
      createOpp(v.organizationId);
      return;
    }

    const newName = v.newOrgName.trim();
    if (!newName) {
      this.saving.set(false);
      this.formError.set('Elige una organización o crea una nueva.');
      return;
    }

    this.organizations
      .create({ name: newName, kind: v.newOrgKind })
      .subscribe({
        next: (org) => {
          this.orgOptions.update((list) => [org, ...list]);
          createOpp(org.id);
        },
        error: (err: unknown) => {
          this.saving.set(false);
          this.formError.set(parseApiError(err).message);
        },
      });
  }

  private loadOrganizations(): void {
    this.organizations.list({ page: 0, size: 100 }).subscribe({
      next: (page) => this.orgOptions.set(page.items),
      error: () => this.orgOptions.set([]),
    });
  }
}
