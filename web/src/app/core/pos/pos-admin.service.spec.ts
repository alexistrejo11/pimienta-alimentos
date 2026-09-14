import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { PosAdminService } from './pos-admin.service';

describe('PosAdminService observability reads', () => {
  let service: PosAdminService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [PosAdminService, provideHttpClient(), provideHttpClientTesting()] });
    service = TestBed.inject(PosAdminService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('requests shifts scoped to a headquarter', () => {
    service.listShifts({ headquarterId: 7, page: 1, size: 5 }).subscribe();

    const request = http.expectOne((req) => req.url.endsWith('/pos/admin/shifts'));
    expect(request.request.method).toBe('GET');
    expect(request.request.params.get('headquarterId')).toBe('7');
    expect(request.request.params.get('page')).toBe('1');
    expect(request.request.params.get('size')).toBe('5');
    request.flush({ items: [], metadata: {} });
  });

  it('requests the summary with an optional site and date range', () => {
    service.reportSummary(7, '2026-09-13T00:00:00.000Z', '2026-09-13T23:59:59.999Z').subscribe();

    const request = http.expectOne((req) => req.url.endsWith('/pos/admin/reports/summary'));
    expect(request.request.params.get('headquarterId')).toBe('7');
    expect(request.request.params.get('from')).toContain('2026-09-13');
    expect(request.request.params.get('to')).toContain('2026-09-13');
    request.flush({ rows: [] });
  });
});
