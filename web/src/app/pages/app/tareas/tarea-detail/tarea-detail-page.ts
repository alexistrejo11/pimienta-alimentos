import { Component, inject, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { finalize } from 'rxjs';

import { TaskService } from '../../../../core/tasks/task.service';
import { parseApiError, type ParsedApiError } from '../../../../core/http/parse-api-error';
import type { TaskResponse } from '../../../../core/model/task/task.dto';
import { PageHeaderComponent } from '../../../../shared/ui/page-header/page-header';
import { DataStateComponent } from '../../../../shared/ui/data-state/data-state';
import { StatusBadgeComponent } from '../../../../shared/ui/status-badge/status-badge';
import { ChecklistSectionComponent } from '../components/checklist-section/checklist-section';

/** Detalle completo de una tarea, incluyendo checklist. */
@Component({
  selector: 'app-tarea-detail-page',
  
  imports: [PageHeaderComponent, DataStateComponent, StatusBadgeComponent, ChecklistSectionComponent, FormsModule],
  templateUrl: './tarea-detail-page.html',
})
export class TareaDetailPageComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly service = inject(TaskService);

  readonly loading = signal(true);
  readonly error = signal<ParsedApiError | null>(null);
  readonly tarea = signal<TaskResponse | null>(null);
  readonly updateStatus = signal('PENDING');
  readonly assigneeId = signal('');
  readonly saving = signal(false);
  readonly deleting = signal(false);
  readonly actionError = signal<ParsedApiError | null>(null);

  ngOnInit(): void {
    const id = Number(this.route.snapshot.paramMap.get('id'));
    this.cargar(id);
  }

  cargar(id: number): void {
    this.error.set(null);
    this.loading.set(true);
    this.service
      .getById(id)
      .pipe(finalize(() => this.loading.set(false)))
      .subscribe({
        next: (t) => {
          this.tarea.set(t);
          this.updateStatus.set(t.status);
          this.assigneeId.set(t.assignedToId == null ? '' : String(t.assignedToId));
        },
        error: (err: unknown) => this.error.set(parseApiError(err)),
      });
  }

  applyUpdates(): void {
    const t = this.tarea();
    if (!t) return;
    this.actionError.set(null);
    this.saving.set(true);

    const targetStatus = this.updateStatus();
    const targetAssigneeRaw = this.assigneeId().trim();
    const targetAssignee = targetAssigneeRaw === '' ? null : Number(targetAssigneeRaw);

    const runAssign = (latest: TaskResponse): void => {
      if (
        targetAssignee == null ||
        Number.isNaN(targetAssignee) ||
        targetAssignee <= 0 ||
        latest.assignedToId === targetAssignee
      ) {
        this.tarea.set(latest);
        this.saving.set(false);
        return;
      }
      this.service.assign(latest.id, { employeeId: targetAssignee }).subscribe({
        next: (updated) => {
          this.tarea.set(updated);
          this.updateStatus.set(updated.status);
          this.assigneeId.set(updated.assignedToId == null ? '' : String(updated.assignedToId));
          this.saving.set(false);
        },
        error: (err: unknown) => {
          this.actionError.set(parseApiError(err));
          this.saving.set(false);
        },
      });
    };

    if (targetStatus !== t.status) {
      this.service.updateStatus(t.id, { status: targetStatus as TaskResponse['status'] }).subscribe({
        next: (updated) => runAssign(updated),
        error: (err: unknown) => {
          this.actionError.set(parseApiError(err));
          this.saving.set(false);
        },
      });
      return;
    }

    runAssign(t);
  }

  toggleChecklist(displayOrder: number): void {
    const t = this.tarea();
    if (!t) return;
    this.actionError.set(null);
    this.service.toggleChecklistItem(t.id, displayOrder).subscribe({
      next: (updated) => {
        this.tarea.set(updated);
        this.updateStatus.set(updated.status);
        this.assigneeId.set(updated.assignedToId == null ? '' : String(updated.assignedToId));
      },
      error: (err: unknown) => this.actionError.set(parseApiError(err)),
    });
  }

  eliminar(): void {
    const t = this.tarea();
    if (!t) return;
    if (!confirm(`¿Eliminar la tarea "${t.title}"?`)) return;
    this.actionError.set(null);
    this.deleting.set(true);
    this.service
      .delete(t.id)
      .pipe(finalize(() => this.deleting.set(false)))
      .subscribe({
        next: () => void this.router.navigateByUrl('/app/tareas'),
        error: (err: unknown) => this.actionError.set(parseApiError(err)),
      });
  }

  formatDate(iso: string | null): string {
    if (!iso) return '—';
    return new Date(iso).toLocaleDateString('es-MX', { day: '2-digit', month: 'long', year: 'numeric' });
  }

  get prioridadLabel(): string {
    const map: Record<string, string> = {
      LOW: 'Baja',
      MEDIUM: 'Media',
      HIGH: 'Alta',
      URGENT: 'Urgente',
      UNDEFINED: 'Sin definir',
    };
    return map[this.tarea()?.priority ?? ''] ?? '—';
  }
}
