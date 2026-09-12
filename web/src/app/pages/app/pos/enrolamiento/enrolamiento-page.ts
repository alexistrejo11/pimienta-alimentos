import { Component, inject, OnInit, signal } from '@angular/core';
import { finalize } from 'rxjs';

import { SessionContextService } from '../../../../core/auth/session-context.service';
import { HeadquarterLookupService } from '../../../../core/headquarters/headquarter-lookup.service';
import { PosAdminService } from '../../../../core/pos/pos-admin.service';
import { parseApiError, type ParsedApiError } from '../../../../core/http/parse-api-error';
import type { EnrollmentCodeResponse } from '../../../../core/model/pos/pos.dto';
import { ApiFormErrorComponent } from '../../../../shared/ui/api-form-error/api-form-error';
import { HeadquarterSelectComponent } from '../../../../shared/ui/headquarter-select/headquarter-select';
import { PageHeaderComponent } from '../../../../shared/ui/page-header/page-header';

@Component({
  selector: 'app-enrolamiento-page',
  imports: [PageHeaderComponent, HeadquarterSelectComponent, ApiFormErrorComponent],
  templateUrl: './enrolamiento-page.html',
})
export class EnrolamientoPageComponent implements OnInit {
  private readonly posAdmin = inject(PosAdminService);
  private readonly session = inject(SessionContextService);
  private readonly lookup = inject(HeadquarterLookupService);

  readonly error = signal<ParsedApiError | null>(null);
  readonly generating = signal(false);
  readonly lastCode = signal<EnrollmentCodeResponse | null>(null);

  readonly isAdmin = this.session.isAdmin;
  readonly hqName = this.lookup.name.bind(this.lookup);

  selectedHeadquarterId: number | null = null;

  ngOnInit(): void {
    void this.lookup.ensureLoaded();
  }

  onHeadquarterChange(value: number | number[] | null): void {
    this.selectedHeadquarterId = typeof value === 'number' ? value : null;
  }

  emitir(): void {
    const hqId = this.selectedHeadquarterId;
    if (hqId == null) return;

    this.error.set(null);
    this.generating.set(true);
    this.posAdmin
      .createEnrollmentCode({ headquarterId: hqId })
      .pipe(finalize(() => this.generating.set(false)))
      .subscribe({
        next: (code) => this.lastCode.set(code),
        error: (err: unknown) => this.error.set(parseApiError(err)),
      });
  }

  formatExpiry(iso: string): string {
    return new Date(iso).toLocaleString('es-MX');
  }
}
