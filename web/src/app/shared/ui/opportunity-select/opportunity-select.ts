import { Component, inject, input, OnInit, output, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';

import { CrmLookupService } from '../../../core/crm/crm-lookup.service';
import { CrmService } from '../../../core/crm/crm.service';
import type { OpportunityResponse } from '../../../core/model/crm/opportunity.dto';

@Component({
  selector: 'app-opportunity-select',
  imports: [FormsModule],
  templateUrl: './opportunity-select.html',
})
export class OpportunitySelectComponent implements OnInit {
  private readonly crm = inject(CrmService);
  private readonly lookup = inject(CrmLookupService);

  readonly label = input('Oportunidad');
  readonly allowNull = input(true);
  readonly nullLabel = input('Sin vínculo');
  readonly disabled = input(false);

  readonly value = input<number | null>(null);
  readonly valueChange = output<number | null>();

  readonly options = signal<OpportunityResponse[]>([]);
  readonly loading = signal(true);

  ngOnInit(): void {
    this.crm.listOpportunities({ page: 0, size: 100 }).subscribe({
      next: (page) => {
        this.options.set(page.items);
        void this.lookup.ensureOpportunitiesLoaded();
        this.loading.set(false);
      },
      error: () => this.loading.set(false),
    });
  }

  protected onChange(id: number | null): void {
    this.valueChange.emit(id);
  }

  protected optionLabel(o: OpportunityResponse): string {
    return `${o.title} — ${o.companyName}`;
  }
}
