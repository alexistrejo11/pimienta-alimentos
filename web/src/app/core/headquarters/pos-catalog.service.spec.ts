import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { PosCatalogService } from './pos-catalog.service';

describe('PosCatalogService category administration', () => {
  let service: PosCatalogService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [PosCatalogService, provideHttpClient(), provideHttpClientTesting()] });
    service = TestBed.inject(PosCatalogService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('loads inactive categories for structured administration', () => {
    service.listCategories(7, true).subscribe();
    const request = http.expectOne((req) => req.url.endsWith('/headquarters/7/pos-categories'));
    expect(request.request.params.get('includeInactive')).toBe('true');
    request.flush([]);
  });

  it('renames and reorders a category through the category endpoint', () => {
    service.updateCategory(7, 12, 'Bebidas frías', 2).subscribe();
    const request = http.expectOne('http://localhost:8080/api/v1/headquarters/7/pos-categories/12');
    expect(request.request.method).toBe('PUT');
    expect(request.request.body).toEqual({ name: 'Bebidas frías', displayOrder: 2 });
    request.flush({ id: 12, headquarterId: 7, name: 'Bebidas frías', displayOrder: 2, active: true });
  });

  it('archives a category through the category endpoint', () => {
    service.archiveCategory(7, 12).subscribe();
    const request = http.expectOne('http://localhost:8080/api/v1/headquarters/7/pos-categories/12');
    expect(request.request.method).toBe('DELETE');
    request.flush(null);
  });
});
