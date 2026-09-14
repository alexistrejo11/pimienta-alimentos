import { Component, inject, OnInit, signal } from '@angular/core';
import { finalize } from 'rxjs';

import { SessionContextService } from '../../../../core/auth/session-context.service';
import { HeadquarterLookupService } from '../../../../core/headquarters/headquarter-lookup.service';
import { PosAdminService } from '../../../../core/pos/pos-admin.service';
import { parseApiError, type ParsedApiError } from '../../../../core/http/parse-api-error';
import type { PosDeviceAdminResponse } from '../../../../core/model/pos/pos.dto';
import { HeadquarterSelectComponent } from '../../../../shared/ui/headquarter-select/headquarter-select';
import { PageHeaderComponent } from '../../../../shared/ui/page-header/page-header';
import { DataStateComponent } from '../../../../shared/ui/data-state/data-state';
import { RouterLink } from '@angular/router';

@Component({
  selector: 'app-dispositivos-page',
  imports: [PageHeaderComponent, DataStateComponent, HeadquarterSelectComponent, RouterLink],
  templateUrl: './dispositivos-page.html',
})
export class DispositivosPageComponent implements OnInit {
  private readonly posAdmin = inject(PosAdminService);
  private readonly session = inject(SessionContextService);
  private readonly lookup = inject(HeadquarterLookupService);

  readonly loading = signal(true);
  readonly error = signal<ParsedApiError | null>(null);
  readonly devices = signal<PosDeviceAdminResponse[]>([]);
  readonly revokingId = signal<string | null>(null);

  readonly isAdmin = this.session.isAdmin;
  readonly hqName = this.lookup.name.bind(this.lookup);

  filterHeadquarterId: number | null = null;

  ngOnInit(): void {
    void this.lookup.ensureLoaded();
    this.filterHeadquarterId = this.session.activeHeadquarterId();
    this.cargar();
  }

  onFilterChange(value: number | number[] | null): void {
    this.filterHeadquarterId = typeof value === 'number' ? value : null;
    this.session.selectHeadquarter(this.filterHeadquarterId);
    this.cargar();
  }

  cargar(): void {
    this.error.set(null);
    this.loading.set(true);
    this.posAdmin
      .listDevices({
        page: 0,
        size: 50,
        headquarterId: this.filterHeadquarterId ?? undefined,
      })
      .pipe(finalize(() => this.loading.set(false)))
      .subscribe({
        next: (page) => this.devices.set(page.items),
        error: (err: unknown) => this.error.set(parseApiError(err)),
      });
  }

  revoke(device: PosDeviceAdminResponse): void {
    if (!confirm(`¿Revocar el dispositivo ${device.visibleCode}?`)) return;
    this.revokingId.set(device.id);
    this.posAdmin
      .revokeDevice(device.id)
      .pipe(finalize(() => this.revokingId.set(null)))
      .subscribe({
        next: () => this.cargar(),
        error: (err: unknown) => this.error.set(parseApiError(err)),
      });
  }
}
