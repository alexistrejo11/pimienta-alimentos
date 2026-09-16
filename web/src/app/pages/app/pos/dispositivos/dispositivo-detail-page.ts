import { DatePipe } from '@angular/common';
import { Component, inject, OnInit, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { finalize } from 'rxjs';

import { HeadquarterLookupService } from '../../../../core/headquarters/headquarter-lookup.service';
import { parseApiError, type ParsedApiError } from '../../../../core/http/parse-api-error';
import type { PosDeviceAdminResponse, PosSaleReportResponse, PosShiftResponse } from '../../../../core/model/pos/pos.dto';
import { PosAdminService } from '../../../../core/pos/pos-admin.service';
import { posDeviceStatusLabel, posShiftStatusLabel, posSaleTicketStatusLabel } from '../../../../core/i18n/enum-labels';
import { todayInstantRange, formatCentavos } from '../../../../core/pos/pos-date.util';
import { DataStateComponent } from '../../../../shared/ui/data-state/data-state';
import { PageHeaderComponent } from '../../../../shared/ui/page-header/page-header';

@Component({
  selector: 'app-dispositivo-detail-page',
  imports: [DatePipe, RouterLink, PageHeaderComponent, DataStateComponent],
  templateUrl: './dispositivo-detail-page.html',
})
export class DispositivoDetailPageComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly posAdmin = inject(PosAdminService);
  private readonly lookup = inject(HeadquarterLookupService);

  readonly loading = signal(true);
  readonly error = signal<ParsedApiError | null>(null);
  readonly device = signal<PosDeviceAdminResponse | null>(null);
  readonly sales = signal<PosSaleReportResponse[]>([]);
  readonly shifts = signal<PosShiftResponse[]>([]);
  readonly formatCentavos = formatCentavos;
  readonly hqName = this.lookup.name.bind(this.lookup);
  readonly posDeviceStatusLabel = posDeviceStatusLabel;
  readonly posShiftStatusLabel = posShiftStatusLabel;
  readonly posSaleTicketStatusLabel = posSaleTicketStatusLabel;

  ngOnInit(): void {
    void this.lookup.ensureLoaded();
    const id = this.route.snapshot.paramMap.get('id');
    if (!id) {
      this.error.set(parseApiError(new Error('Dispositivo no encontrado.')));
      this.loading.set(false);
      return;
    }
    this.posAdmin.listDevices({ page: 0, size: 100 }).pipe(finalize(() => this.loading.set(false))).subscribe({
      next: (page) => {
        const found = page.items.find((item) => item.id === id);
        if (!found) {
          this.error.set(parseApiError(new Error('Dispositivo no encontrado o sin acceso.')));
          return;
        }
        this.device.set(found);
        this.loadActivity(found);
      },
      error: (err: unknown) => this.error.set(parseApiError(err)),
    });
  }

  private loadActivity(device: PosDeviceAdminResponse): void {
    const range = todayInstantRange();
    this.posAdmin.reportSales({ headquarterId: device.headquarterId, from: range.from, to: range.to, page: 0, size: 100 }).subscribe({
      next: (page) => this.sales.set(page.items.filter((sale) => sale.deviceId === device.id)),
      error: () => this.sales.set([]),
    });
    this.posAdmin.listShifts({ headquarterId: device.headquarterId, page: 0, size: 100 }).subscribe({
      next: (page) => this.shifts.set(page.items.filter((shift) => shift.deviceId === device.id)),
      error: () => this.shifts.set([]),
    });
  }
}
