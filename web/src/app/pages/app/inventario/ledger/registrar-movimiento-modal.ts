import { Component, HostListener, computed, inject, OnInit, output, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { finalize } from 'rxjs';

import { SessionContextService } from '../../../../core/auth/session-context.service';
import { HeadquarterLookupService } from '../../../../core/headquarters/headquarter-lookup.service';
import { parseApiError, type ParsedApiError } from '../../../../core/http/parse-api-error';
import { InventoryService } from '../../../../core/inventory/inventory.service';
import type { ItemResponse, StorageLocationResponse } from '../../../../core/model/inventory/inventory.dto';
import type { StaffExitReason } from '../../../../core/model/inventory/inventory.enums';
import { HeadquarterSelectComponent } from '../../../../shared/ui/headquarter-select/headquarter-select';
import { ItemSelectComponent } from '../../../../shared/ui/item-select/item-select';

type MovementKind = 'entry' | 'exit' | 'adjustment';
type EntryMode = 'receipt' | 'initial';

const STAFF_EXIT_REASONS: StaffExitReason[] = ['WASTE', 'WITHDRAWAL'];

@Component({
  selector: 'app-registrar-movimiento-modal',
  imports: [FormsModule, HeadquarterSelectComponent, ItemSelectComponent],
  templateUrl: './registrar-movimiento-modal.html',
})
export class RegistrarMovimientoModalComponent implements OnInit {
  private readonly session = inject(SessionContextService);
  private readonly inventory = inject(InventoryService);
  private readonly hqLookup = inject(HeadquarterLookupService);

  readonly cerrar = output<void>();
  readonly registrado = output<string>();

  readonly loading = signal(false);
  readonly error = signal<ParsedApiError | null>(null);
  readonly validation = signal('');
  readonly locations = signal<StorageLocationResponse[]>([]);
  readonly kind = signal<MovementKind>('entry');
  readonly entryMode = signal<EntryMode>('receipt');
  readonly isAdmin = this.session.isAdmin;
  readonly exitReasons = STAFF_EXIT_REASONS;

  readonly needsHeadquarter = computed(() => this.kind() !== 'adjustment');

  selectedHeadquarterId: number | null = null;
  itemId: number | null = null;
  locationId: number | null = null;
  quantity = 1;
  unitCost = 0;
  reference = '';
  notes = '';
  exitReason: StaffExitReason = 'WASTE';
  countedQuantity = 0;
  reason = '';

  readonly effectiveHeadquarterId = computed(() =>
    this.session.isAdmin() ? this.selectedHeadquarterId : this.session.activeHeadquarterId(),
  );

  ngOnInit(): void {
    void this.hqLookup.ensureLoaded();
    if (!this.session.isAdmin()) {
      this.selectedHeadquarterId = this.session.activeHeadquarterId();
    }
    this.loadLocations();
  }

  @HostListener('document:keydown.escape')
  onEscape(): void {
    if (!this.loading()) this.cerrar.emit();
  }

  selectKind(kind: MovementKind): void {
    if (this.loading()) return;
    if (kind === 'adjustment' && !this.isAdmin()) return;
    this.kind.set(kind);
    this.error.set(null);
    this.validation.set('');
    this.itemId = null;
    this.locationId = null;
    this.quantity = 1;
    this.unitCost = 0;
    this.reference = '';
    this.notes = '';
    this.exitReason = 'WASTE';
    this.countedQuantity = 0;
    this.reason = '';
    this.loadLocations();
  }

  selectEntryMode(mode: EntryMode): void {
    if (this.loading()) return;
    this.entryMode.set(mode);
  }

  exitReasonOptionLabel(reason: StaffExitReason): string {
    return reason === 'WASTE' ? 'Merma / Desperdicio' : 'Retiro';
  }

  onHeadquarterChange(id: number | number[] | null): void {
    this.selectedHeadquarterId = Array.isArray(id) ? (id[0] ?? null) : id;
    this.session.selectHeadquarter(this.selectedHeadquarterId);
    this.itemId = null;
    this.locationId = null;
    this.unitCost = 0;
    this.error.set(null);
    this.loadLocations();
  }

  onItemChange(id: number | null): void {
    this.itemId = id;
  }

  onItemPicked(item: ItemResponse | null): void {
    if (!item || this.unitCost !== 0) return;
    if (this.kind() === 'exit' || (this.kind() === 'entry' && this.entryMode() === 'receipt')) {
      this.unitCost = item.costPrice;
    }
  }

  loadLocations(): void {
    if (this.kind() === 'adjustment') {
      this.inventory.searchLocations({ page: 0, size: 100 }).subscribe((page) => this.locations.set(page.items));
      return;
    }
    const hqId = this.effectiveHeadquarterId();
    if (hqId == null) {
      this.locations.set([]);
      return;
    }
    this.inventory
      .searchLocations({ headquarterId: hqId, page: 0, size: 100 })
      .subscribe((page) => this.locations.set(page.items));
  }

  guardar(): void {
    if (this.loading()) return;
    const missing = this.missingFields();
    if (missing) {
      this.validation.set(missing);
      return;
    }
    this.validation.set('');
    this.error.set(null);
    this.loading.set(true);
    const done = () => this.loading.set(false);
    const kind = this.kind();
    const initiatedById = this.session.userId() > 0 ? this.session.userId() : undefined;
    if (kind === 'entry' && this.entryMode() === 'initial') {
      this.inventory
        .createInitialStock({
          itemId: this.itemId!,
          locationId: this.locationId!,
          initialQuantity: this.quantity,
        })
        .pipe(finalize(done))
        .subscribe({
          next: () => this.registrado.emit('Stock inicial registrado.'),
          error: (err: unknown) => this.error.set(parseApiError(err)),
        });
      return;
    }
    if (kind === 'entry') {
      this.inventory
        .stockIn({
          externalReference: this.reference || undefined,
          notes: this.notes || undefined,
          initiatedById,
          lines: [
            {
              itemId: this.itemId!,
              locationId: this.locationId!,
              quantity: this.quantity,
              unitCost: this.unitCost,
            },
          ],
        })
        .pipe(finalize(done))
        .subscribe({
          next: () => this.registrado.emit('Entrada registrada.'),
          error: (err: unknown) => this.error.set(parseApiError(err)),
        });
      return;
    }
    if (kind === 'exit') {
      this.inventory
        .stockOut({
          externalReference: this.reference || undefined,
          notes: this.notes.trim() || undefined,
          exitReason: this.exitReason,
          initiatedById,
          lines: [
            {
              itemId: this.itemId!,
              locationId: this.locationId!,
              quantity: this.quantity,
              unitCost: this.unitCost,
            },
          ],
        })
        .pipe(finalize(done))
        .subscribe({
          next: () => this.registrado.emit('Salida registrada.'),
          error: (err: unknown) => this.error.set(parseApiError(err)),
        });
      return;
    }
    this.inventory
      .adjustment({
        notes: this.notes.trim() || undefined,
        initiatedById,
        lines: [
          {
            itemId: this.itemId!,
            locationId: this.locationId!,
            countedQuantity: this.countedQuantity,
            reason: this.reason.trim(),
          },
        ],
      })
      .pipe(finalize(done))
      .subscribe({
        next: () => this.registrado.emit('Ajuste registrado.'),
        error: (err: unknown) => this.error.set(parseApiError(err)),
      });
  }

  private missingFields(): string | null {
    if (this.needsHeadquarter() && this.session.isAdmin() && this.selectedHeadquarterId == null) {
      return 'Selecciona la sede.';
    }
    if (this.itemId == null) return 'Selecciona el artículo.';
    if (this.locationId == null) return 'Selecciona la ubicación.';
    if (this.kind() === 'adjustment') {
      if (!this.reason.trim()) return 'Escribe el motivo del ajuste.';
      if (!Number.isFinite(this.countedQuantity) || this.countedQuantity < 0) {
        return 'La cantidad contada no puede ser negativa.';
      }
      return null;
    }
    if (this.quantity <= 0) return 'La cantidad debe ser mayor a cero.';
    return null;
  }
}
