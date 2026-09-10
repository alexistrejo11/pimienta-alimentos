import { Component, inject, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { finalize } from 'rxjs';

import { SessionContextService } from '../../../../core/auth/session-context.service';
import { HeadquarterService } from '../../../../core/headquarters/headquarter.service';
import { PosAdminService } from '../../../../core/pos/pos-admin.service';
import { parseApiError, type ParsedApiError } from '../../../../core/http/parse-api-error';
import type { HeadQuarterResponse } from '../../../../core/model/headquarter/headquarter.dto';
import type { EnrollmentCodeResponse } from '../../../../core/model/pos/pos.dto';
import { PageHeaderComponent } from '../../../../shared/ui/page-header/page-header';

@Component({
  selector: 'app-enrolamiento-page',
  imports: [PageHeaderComponent, FormsModule],
  templateUrl: './enrolamiento-page.html',
})
export class EnrolamientoPageComponent implements OnInit {
  private readonly posAdmin = inject(PosAdminService);
  private readonly session = inject(SessionContextService);
  private readonly hqService = inject(HeadquarterService);

  readonly error = signal<ParsedApiError | null>(null);
  readonly generating = signal(false);
  readonly lastCode = signal<EnrollmentCodeResponse | null>(null);
  readonly sedes = signal<HeadQuarterResponse[]>([]);

  readonly isAdmin = this.session.isAdmin;
  selectedHeadquarterId: number | null = null;

  ngOnInit(): void {
    if (this.session.isAdmin()) {
      this.hqService.list(0, 100).subscribe({
        next: (page) => {
          this.sedes.set(page.content);
          if (page.content.length > 0) {
            this.selectedHeadquarterId = page.content[0].id;
          }
        },
      });
    } else {
      this.selectedHeadquarterId = this.session.managerHeadquarterId();
    }
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
