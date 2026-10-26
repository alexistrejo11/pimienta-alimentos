import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { OperatingExpenseService } from './expense.service';

describe('OperatingExpenseService', () => {
  let service: OperatingExpenseService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [OperatingExpenseService, provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(OperatingExpenseService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('sends create as multipart with a JSON data part and attachments', async () => {
    const file = new File([new Uint8Array([1, 2, 3])], 'ticket.jpg', { type: 'image/jpeg' });
    service
      .create(
        {
          headquarterId: 4,
          amount: 12.5,
          category: 'SUPPLIERS_MERCHANDISE',
          concept: 'Hielo',
          notes: '  ',
          occurredOn: '2026-10-14',
        },
        [file],
      )
      .subscribe();

    const request = http.expectOne('http://localhost:8080/api/v1/operating-expenses');
    expect(request.request.method).toBe('POST');
    const body = request.request.body as FormData;
    const data = body.get('data');
    expect(data).toBeInstanceOf(Blob);
    expect((data as Blob).type).toBe('application/json');
    const text = await (data as Blob).text();
    expect(JSON.parse(text)).toEqual({
      amount: 12.5,
      category: 'SUPPLIERS_MERCHANDISE',
      concept: 'Hielo',
      headquarterId: 4,
      occurredOn: '2026-10-14',
    });
    const attached = body.get('attachments');
    expect(attached).toBeInstanceOf(File);
    expect((attached as File).name).toBe('ticket.jpg');
    expect((attached as File).type).toBe('image/jpeg');
    request.flush({ id: 1 });
  });

  it('sends retainAttachmentKeys on update, including an empty list', async () => {
    service
      .update(9, { amount: 1, category: 'RENT', concept: 'Gas', retainAttachmentKeys: [] }, [])
      .subscribe();

    const request = http.expectOne('http://localhost:8080/api/v1/operating-expenses/9');
    expect(request.request.method).toBe('PUT');
    const data = (request.request.body as FormData).get('data') as Blob;
    expect(JSON.parse(await data.text())).toEqual({
      amount: 1,
      category: 'RENT',
      concept: 'Gas',
      retainAttachmentKeys: [],
    });
    request.flush({ id: 9 });
  });

  it('omits dates and sede when the list filters are empty', () => {
    service.list({ page: 0, size: 20, search: 'hielo' }).subscribe();

    const request = http.expectOne((req) => req.url.endsWith('/operating-expenses'));
    expect(request.request.params.get('search')).toBe('hielo');
    expect(request.request.params.has('from')).toBe(false);
    expect(request.request.params.has('to')).toBe(false);
    expect(request.request.params.has('headquarterId')).toBe(false);
    request.flush({ items: [], metadata: {} });
  });

  it('requests today totals for one sede', () => {
    service.summary({ headquarterId: 4, from: '2026-10-15', to: '2026-10-15' }).subscribe();

    const request = http.expectOne((req) => req.url.endsWith('/operating-expenses/summary'));
    expect(request.request.method).toBe('GET');
    expect(request.request.params.get('headquarterId')).toBe('4');
    expect(request.request.params.get('from')).toBe('2026-10-15');
    expect(request.request.params.get('to')).toBe('2026-10-15');
    expect(request.request.params.has('search')).toBe(false);
    request.flush({ totalAmount: 25.5 });
  });

  it('forwards search and category on the summary', () => {
    service
      .summary({
        headquarterId: 4,
        from: '2026-10-01',
        to: '2026-10-15',
        search: ' hielo ',
        category: 'RENT',
      })
      .subscribe();

    const request = http.expectOne((req) => req.url.endsWith('/operating-expenses/summary'));
    expect(request.request.params.get('search')).toBe('hielo');
    expect(request.request.params.get('category')).toBe('RENT');
    expect(request.request.params.get('from')).toBe('2026-10-01');
    expect(request.request.params.get('to')).toBe('2026-10-15');
    request.flush({ totalAmount: 0 });
  });
});
