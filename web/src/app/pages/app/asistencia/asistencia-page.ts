import { DatePipe } from '@angular/common';
import { Component, inject, OnInit, signal } from '@angular/core';
import { finalize } from 'rxjs';

import { AttendanceService } from '../../../core/employees/attendance.service';
import { attendanceStatusLabel } from '../../../core/i18n/enum-labels';
import type { AttendanceResponse } from '../../../core/model/employee/attendance.dto';
import type { ParsedApiError } from '../../../core/http/parse-api-error';
import { parseApiError } from '../../../core/http/parse-api-error';

@Component({
  selector: 'app-asistencia-page',
  imports: [DatePipe],
  templateUrl: './asistencia-page.html',
})
export class AsistenciaPageComponent implements OnInit {
  private readonly service = inject(AttendanceService);
  readonly records = signal<AttendanceResponse[]>([]);
  readonly loading = signal(true);
  readonly error = signal<ParsedApiError | null>(null);

  ngOnInit(): void { this.load(); }
  load(): void {
    this.loading.set(true);
    this.service.forToday(undefined, 0, 50).pipe(finalize(() => this.loading.set(false))).subscribe({
      next: (page) => this.records.set(page.items),
      error: (err: unknown) => this.error.set(parseApiError(err)),
    });
  }
  statusLabel(status: string): string { return attendanceStatusLabel(status); }
}
