import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { finalize } from 'rxjs';

import { SessionContextService } from '../../../core/auth/session-context.service';
import { HeadquarterLookupService } from '../../../core/headquarters/headquarter-lookup.service';
import { parseApiError, type ParsedApiError } from '../../../core/http/parse-api-error';
import type { SupplierResponse } from '../../../core/model/supplier/supplier.dto';
import { SupplierService } from '../../../core/suppliers/supplier.service';
import { DataStateComponent } from '../../../shared/ui/data-state/data-state';
import { HeadquarterSelectComponent } from '../../../shared/ui/headquarter-select/headquarter-select';
import { PageHeaderComponent } from '../../../shared/ui/page-header/page-header';

@Component({
  selector: 'app-proveedores-page',
  imports: [
    FormsModule,
    RouterLink,
    PageHeaderComponent,
    DataStateComponent,
    HeadquarterSelectComponent,
  ],
  templateUrl: './proveedores-page.html',
})
export class ProveedoresPageComponent implements OnInit {
  readonly session = inject(SessionContextService);
  private readonly suppliers = inject(SupplierService);
  private readonly hqLookup = inject(HeadquarterLookupService);

  readonly loading = signal(true);
  readonly error = signal<ParsedApiError | null>(null);
  readonly items = signal<SupplierResponse[]>([]);
  readonly metadata = signal<{
    pageNumber: number;
    totalPages: number;
    totalElements: number;
    hasNext: boolean;
    hasPrevious: boolean;
  } | null>(null);
  readonly staffUnassigned = signal(false);
  readonly deletingId = signal<number | null>(null);

  selectedHeadquarterId: number | null = null;
  search = '';
  page = 0;
  private initialLoad = true;

  readonly effectiveHeadquarterId = computed(() => {
    if (this.session.isAdmin()) {
      return this.selectedHeadquarterId;
    }
    return this.session.activeHeadquarterId();
  });

  ngOnInit(): void {
    void this.hqLookup.ensureLoaded();
    this.session.ensureLoaded().subscribe({
      next: () => {
        if (!this.session.isAdmin()) {
          const hq = this.session.activeHeadquarterId();
          if (hq == null && this.session.assignedHeadquarterIds().length === 0) {
            this.staffUnassigned.set(true);
            this.loading.set(false);
            return;
          }
          this.selectedHeadquarterId = hq;
          this.cargar();
        } else {
          this.loading.set(false);
        }
      },
      error: () => this.loading.set(false),
    });
  }

  headquarterLabel(id: number): string {
    return this.hqLookup.name(id);
  }

  headquarterLabels(ids: number[]): string {
    if (ids.length === 0) return '—';
    return ids.map((id) => this.hqLookup.name(id)).join(', ');
  }

  onHeadquarterChange(id: number | number[] | null): void {
    const hqId = Array.isArray(id) ? (id[0] ?? null) : id;
    this.selectedHeadquarterId = hqId;
    this.session.selectHeadquarter(hqId);
    if (this.session.isAdmin() && hqId == null) {
      this.page = 0;
      this.cargar();
      return;
    }
    if (hqId == null) {
      this.items.set([]);
      this.metadata.set(null);
      this.loading.set(false);
      return;
    }
    if (this.initialLoad) {
      this.initialLoad = false;
    }
    this.page = 0;
    this.cargar();
  }

  onSearchInput(value: string): void {
    this.search = value;
    this.page = 0;
    this.cargar();
  }

  setPage(page: number): void {
    this.page = page;
    this.cargar();
  }

  cargar(): void {
    this.error.set(null);
    this.loading.set(true);
    const hqId = this.effectiveHeadquarterId();
    this.suppliers
      .list({
        page: this.page,
        size: 20,
        headquarterId: hqId ?? undefined,
        search: this.search,
      })
      .pipe(finalize(() => this.loading.set(false)))
      .subscribe({
        next: (page) => {
          this.items.set(page.items);
          this.metadata.set(page.metadata);
        },
        error: (err: unknown) => this.error.set(parseApiError(err)),
      });
  }

  eliminar(item: SupplierResponse): void {
    const label = item.contactName || item.name;
    if (!confirm(`¿Eliminar el proveedor "${label}"?`)) return;
    this.deletingId.set(item.id);
    this.suppliers.delete(item.id).subscribe({
      next: () => {
        this.deletingId.set(null);
        this.cargar();
      },
      error: (err: unknown) => {
        this.deletingId.set(null);
        this.error.set(parseApiError(err));
      },
    });
  }
}
