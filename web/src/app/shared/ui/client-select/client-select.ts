import { Component, inject, input, OnInit, output, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';

import { ClientLookupService } from '../../../core/crm/client-lookup.service';
import { ClientService } from '../../../core/crm/client.service';
import type { ClientResponse } from '../../../core/model/crm/client.dto';

@Component({
  selector: 'app-client-select',
  imports: [FormsModule],
  templateUrl: './client-select.html',
})
export class ClientSelectComponent implements OnInit {
  private readonly clients = inject(ClientService);
  private readonly lookup = inject(ClientLookupService);

  readonly label = input('Cliente');
  readonly allowNull = input(false);
  readonly nullLabel = input('Selecciona…');
  readonly disabled = input(false);

  readonly value = input<number | null>(null);
  readonly valueChange = output<number | null>();

  readonly options = signal<ClientResponse[]>([]);
  readonly loading = signal(true);

  ngOnInit(): void {
    this.clients.list({ page: 0, size: 200 }).subscribe({
      next: (page) => {
        this.options.set(page.items);
        void this.lookup.ensureLoaded();
        this.loading.set(false);
      },
      error: () => this.loading.set(false),
    });
  }

  protected onChange(id: number | null): void {
    this.valueChange.emit(id);
  }

  protected optionLabel(c: ClientResponse): string {
    return c.companyName ? `${c.name} (${c.companyName})` : c.name;
  }
}
