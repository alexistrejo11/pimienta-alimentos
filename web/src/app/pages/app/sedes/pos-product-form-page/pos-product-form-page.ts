import { Component, inject, OnInit, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { finalize } from 'rxjs';
import { PosCatalogService } from '../../../../core/headquarters/pos-catalog.service';
import { parseApiError, type ParsedApiError } from '../../../../core/http/parse-api-error';
import type { PosSaleCategoryResponse } from '../../../../core/model/pos/pos.dto';
import type { StockPolicy } from '../../../../core/model/pos/pos.enums';
import type { ItemCategory, ItemUnit } from '../../../../core/model/inventory/inventory.enums';
import { PageHeaderComponent } from '../../../../shared/ui/page-header/page-header';

const CATEGORIES: ItemCategory[] = ['FINISHED_GOOD', 'RAW_MATERIAL', 'CONSUMABLE', 'PACKAGING', 'OTHER'];
const UNITS: ItemUnit[] = ['PIECE', 'KG', 'GRAM', 'LITER', 'ML', 'BOX', 'DOZEN', 'METER', 'SQUARE_METER'];

@Component({ selector: 'app-pos-product-form-page', imports: [ReactiveFormsModule, RouterLink, PageHeaderComponent], templateUrl: './pos-product-form-page.html' })
export class PosProductFormPageComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly posCatalog = inject(PosCatalogService);
  readonly headquarterId = signal(0);
  readonly categories = signal<PosSaleCategoryResponse[]>([]);
  readonly loadingCategories = signal(true);
  readonly loading = signal(false);
  readonly error = signal<ParsedApiError | null>(null);
  readonly units = UNITS;
  readonly inventoryCategories = CATEGORIES;
  readonly stockPolicies: StockPolicy[] = ['CONTROLLED', 'NOT_CONTROLLED'];
  readonly form = this.fb.nonNullable.group({
    name: ['', [Validators.required, Validators.maxLength(300)]], description: [''], costPrice: [0, [Validators.required, Validators.min(0)]], salePrice: [0, [Validators.required, Validators.min(0)]],
    category: ['FINISHED_GOOD' as ItemCategory, Validators.required], unit: ['PIECE' as ItemUnit, Validators.required], brand: [''], barcode: [''], reorderPoint: [0, [Validators.required, Validators.min(0)]], reorderQuantity: [0, [Validators.required, Validators.min(0)]],
    posSaleCategoryId: [0, [Validators.required, Validators.min(1)]], available: [true], stockPolicy: ['NOT_CONTROLLED' as StockPolicy, Validators.required], negativeStockLimit: [null as number | null],
  });

  ngOnInit(): void {
    const id = Number(this.route.snapshot.paramMap.get('id')); this.headquarterId.set(id);
    this.posCatalog.listCategories(id).pipe(finalize(() => this.loadingCategories.set(false))).subscribe({ next: (items) => { this.categories.set(items); if (items[0]) this.form.patchValue({ posSaleCategoryId: items[0].id }); }, error: (e: unknown) => this.error.set(parseApiError(e)) });
  }
  setPrepared(prepared: boolean): void { this.form.patchValue({ stockPolicy: prepared ? 'NOT_CONTROLLED' : 'CONTROLLED' }); }
  submit(): void {
    this.error.set(null); if (this.form.invalid) { this.form.markAllAsTouched(); return; }
    this.loading.set(true); const v = this.form.getRawValue();
    this.posCatalog.createPosProduct(this.headquarterId(), { ...v, description: v.description || undefined, brand: v.brand || undefined, barcode: v.barcode || undefined, negativeStockLimit: v.negativeStockLimit }).pipe(finalize(() => this.loading.set(false))).subscribe({ next: () => this.router.navigate(['/app/sedes', this.headquarterId(), 'pos']), error: (e: unknown) => this.error.set(parseApiError(e)) });
  }
}
