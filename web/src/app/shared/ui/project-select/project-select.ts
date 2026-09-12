import { Component, inject, input, OnInit, output, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';

import { CrmLookupService } from '../../../core/crm/crm-lookup.service';
import { CrmService } from '../../../core/crm/crm.service';
import type { ProjectResponse } from '../../../core/model/crm/project.dto';

@Component({
  selector: 'app-project-select',
  imports: [FormsModule],
  templateUrl: './project-select.html',
})
export class ProjectSelectComponent implements OnInit {
  private readonly crm = inject(CrmService);
  private readonly lookup = inject(CrmLookupService);

  readonly label = input('Proyecto');
  readonly allowNull = input(true);
  readonly nullLabel = input('Sin vínculo');
  readonly disabled = input(false);

  readonly value = input<number | null>(null);
  readonly valueChange = output<number | null>();

  readonly options = signal<ProjectResponse[]>([]);
  readonly loading = signal(true);

  ngOnInit(): void {
    this.crm.listProjects({ page: 0, size: 100 }).subscribe({
      next: (page) => {
        this.options.set(page.items);
        void this.lookup.ensureProjectsLoaded();
        this.loading.set(false);
      },
      error: () => this.loading.set(false),
    });
  }

  protected onChange(id: number | null): void {
    this.valueChange.emit(id);
  }

  protected optionLabel(p: ProjectResponse): string {
    return `${p.projectCode} — ${p.projectName}`;
  }
}
