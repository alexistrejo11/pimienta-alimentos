import { Component, inject, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { finalize } from 'rxjs';

import { SessionContextService } from '../../../../core/auth/session-context.service';
import { HeadquarterService } from '../../../../core/headquarters/headquarter.service';
import { PosAdminService } from '../../../../core/pos/pos-admin.service';
import { parseApiError, type ParsedApiError } from '../../../../core/http/parse-api-error';
import type { HeadQuarterResponse } from '../../../../core/model/headquarter/headquarter.dto';
import type { PosDeviceAdminResponse } from '../../../../core/model/pos/pos.dto';
import { PageHeaderComponent } from '../../../../shared/ui/page-header/page-header';
import { DataStateComponent } from '../../../../shared/ui/data-state/data-state';

@Component({
  selector: 'app-dispositivos-page',
  imports: [PageHeaderComponent, DataStateComponent, FormsModule],
  templateUrl: './dispositivos-page.html',
})
export class DispositivosPageComponent implements OnInit {
  private readonly posAdmin = inject(PosAdminService);
  private readonly session = inject(SessionContextService);
  private readonly hqService = inject(HeadquarterService);

  readonly loading = signal(true);
  readonly error = signal<ParsedApiError | null>(null);
  readonly devices = signal<PosDeviceAdminResponse[]>([]);
  readonly sedes = signal<HeadQuarterResponse[]>([]);
  readonly revokingId = signal<string | null>(null);

  readonly isAdmin = this.session.isAdmin;
  filterHeadquarterId: number | null = null;

  ngOnInit(): void {
    if (!this.session.isAdmin()) {
      this.filterHeadquarterId = this.session.managerHeadquarterId();
      this.cargar();
      return;
    }

    this.hqService.list(0, 100).subscribe({
      next: (page) => this.sedes.set(page.content),
      error: () => {},
    });
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
