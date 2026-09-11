import { Component, inject, OnInit, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { DatePipe } from '@angular/common';
import { finalize } from 'rxjs';

import { HeadquarterLookupService } from '../../../../core/headquarters/headquarter-lookup.service';
import { PosAdminService } from '../../../../core/pos/pos-admin.service';
import { parseApiError, type ParsedApiError } from '../../../../core/http/parse-api-error';
import type { PosSyncIncidentResponse } from '../../../../core/model/pos/pos.dto';
import { PageHeaderComponent } from '../../../../shared/ui/page-header/page-header';
import { DataStateComponent } from '../../../../shared/ui/data-state/data-state';

@Component({
  selector: 'app-incidencias-page',
  imports: [PageHeaderComponent, DataStateComponent, ReactiveFormsModule, DatePipe],
  templateUrl: './incidencias-page.html',
})
export class IncidenciasPageComponent implements OnInit {
  private readonly posAdmin = inject(PosAdminService);
  private readonly lookup = inject(HeadquarterLookupService);
  private readonly fb = inject(FormBuilder);

  readonly hqName = this.lookup.name.bind(this.lookup);

  readonly loading = signal(true);
  readonly error = signal<ParsedApiError | null>(null);
  readonly incidents = signal<PosSyncIncidentResponse[]>([]);
  readonly acceptingId = signal<string | null>(null);
  readonly selectedId = signal<string | null>(null);

  readonly acceptForm = this.fb.nonNullable.group({
    label: ['', Validators.required],
    note: ['', Validators.required],
  });

  ngOnInit(): void {
    void this.lookup.ensureLoaded();
    this.cargar();
  }

  cargar(): void {
    this.error.set(null);
    this.loading.set(true);
    this.posAdmin
      .listSyncIncidents({ page: 0, size: 50, openOnly: true })
      .pipe(finalize(() => this.loading.set(false)))
      .subscribe({
        next: (page) => this.incidents.set(page.items),
        error: (err: unknown) => this.error.set(parseApiError(err)),
      });
  }

  select(incident: PosSyncIncidentResponse): void {
    this.selectedId.set(incident.id);
    this.acceptForm.reset({ label: '', note: '' });
  }

  accept(incidentId: string): void {
    if (this.acceptForm.invalid) {
      this.acceptForm.markAllAsTouched();
      return;
    }

    const v = this.acceptForm.getRawValue();
    this.acceptingId.set(incidentId);
    this.posAdmin
      .acceptSyncIncident(incidentId, { label: v.label, note: v.note })
      .pipe(finalize(() => this.acceptingId.set(null)))
      .subscribe({
        next: () => {
          this.selectedId.set(null);
          this.cargar();
        },
        error: (err: unknown) => this.error.set(parseApiError(err)),
      });
  }
}
