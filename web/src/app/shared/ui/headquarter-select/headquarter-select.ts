import { Component, inject, input, OnInit, output, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';

import { SessionContextService } from '../../../core/auth/session-context.service';
import { HeadquarterService } from '../../../core/headquarters/headquarter.service';
import { HeadquarterLookupService } from '../../../core/headquarters/headquarter-lookup.service';
import type { HeadQuarterResponse } from '../../../core/model/headquarter/headquarter.dto';

@Component({
  selector: 'app-headquarter-select',
  imports: [FormsModule],
  templateUrl: './headquarter-select.html',
})
export class HeadquarterSelectComponent implements OnInit {
  private readonly session = inject(SessionContextService);
  private readonly hqService = inject(HeadquarterService);
  private readonly lookup = inject(HeadquarterLookupService);

  readonly mode = input<'single' | 'multi'>('single');
  readonly allowAll = input(false);
  readonly label = input('Sede');
  readonly disabled = input(false);

  readonly value = input<number | number[] | null>(null);
  readonly valueChange = output<number | number[] | null>();

  readonly sedes = signal<HeadQuarterResponse[]>([]);
  readonly loading = signal(true);
  readonly isAdmin = this.session.isAdmin;
  readonly managerHqId = this.session.managerHeadquarterId;
  readonly assignedHqIds = this.session.assignedHeadquarterIds;

  ngOnInit(): void {
    this.session.ensureLoaded().subscribe({
      next: () => this.loadHeadquarters(),
      error: () => this.loading.set(false),
    });
  }

  private loadHeadquarters(): void {
    if (!this.session.isAdmin()) {
      void this.lookup.ensureLoaded();
      const ids = this.assignedHqIds();
      this.sedes.set(ids.map((id) => ({ id, name: this.lookup.name(id) }) as HeadQuarterResponse));
      const current = this.value();
      if (this.mode() === 'single' && current == null) this.valueChange.emit(this.session.activeHeadquarterId());
      if (this.mode() === 'multi' && !Array.isArray(current)) this.valueChange.emit(ids);
      this.loading.set(false);
      return;
    }

    this.hqService.list(0, 100).subscribe({
      next: (page) => {
        this.sedes.set(page.content);
        this.lookup.ensureLoaded();
        this.loading.set(false);

        const current = this.value();
        if (this.mode() === 'single' && current == null && page.content.length > 0 && !this.allowAll()) {
          this.valueChange.emit(page.content[0].id);
        }
      },
      error: () => this.loading.set(false),
    });
  }

  protected onSingleChange(id: number | null): void {
    this.session.selectHeadquarter(id);
    this.valueChange.emit(id);
  }

  protected onMultiToggle(id: number, checked: boolean): void {
    const current = Array.isArray(this.value()) ? [...(this.value() as number[])] : [];
    const next = checked ? [...current, id] : current.filter((x) => x !== id);
    this.valueChange.emit(next);
  }

  protected isMultiSelected(id: number): boolean {
    const v = this.value();
    return Array.isArray(v) && v.includes(id);
  }

  protected managerSedeName(): string {
    const id = this.managerHqId();
    if (id == null) return '—';
    return this.lookup.name(id);
  }
}
